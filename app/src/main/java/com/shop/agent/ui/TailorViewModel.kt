package com.shop.agent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shop.agent.data.*
import com.shop.agent.python.PythonBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class UiState(
    val clients: List<Client> = emptyList(),
    val selectedClientId: Long = 0,
    val measurements: Measurement? = null,
    val orders: List<Order> = emptyList(),
    val allOrders: List<Order> = emptyList(),
    val message: String = "",
    val busy: Boolean = false
)

class TailorViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TailorDb(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        refreshClients()
        refreshAllOrders()
    }

    fun refreshClients() {
        _state.value = _state.value.copy(clients = db.getAllClients())
    }

    fun refreshAllOrders() {
        _state.value = _state.value.copy(allOrders = db.getAllOrders())
    }

    fun selectClient(id: Long) {
        val m = db.getMeasurement(id)
        val orders = db.getOrdersForClient(id)
        _state.value = _state.value.copy(selectedClientId = id, measurements = m, orders = orders)
    }

    fun addClient(name: String, phone: String, email: String, notes: String): Long {
        val id = db.insertClient(Client(name = name, phone = phone, email = email, notes = notes))
        refreshClients()
        return id
    }

    fun updateClient(c: Client) {
        db.updateClient(c)
        refreshClients()
    }

    fun deleteClient(id: Long) {
        db.deleteClient(id)
        refreshClients()
        if (_state.value.selectedClientId == id) {
            _state.value = _state.value.copy(selectedClientId = 0, measurements = null, orders = emptyList())
        }
    }

    fun saveMeasurements(m: Measurement) {
        db.upsertMeasurement(m)
        if (_state.value.selectedClientId == m.clientId) {
            _state.value = _state.value.copy(measurements = db.getMeasurement(m.clientId))
        }
    }

    fun addOrder(clientId: Long, garmentType: String, size: Int, status: OrderStatus, fabricM: Double) {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        db.insertOrder(Order(clientId = clientId, garmentType = garmentType, size = size, status = status, fabricM = fabricM, createdAt = now))
        refreshAllOrders()
        if (_state.value.selectedClientId == clientId) {
            _state.value = _state.value.copy(orders = db.getOrdersForClient(clientId))
        }
    }

    fun setOrderStatus(id: Long, status: OrderStatus) {
        db.updateOrderStatus(id, status)
        refreshAllOrders()
        val sel = _state.value.selectedClientId
        if (sel != 0L) _state.value = _state.value.copy(orders = db.getOrdersForClient(sel))
    }

    fun deleteOrder(id: Long) {
        db.deleteOrder(id)
        refreshAllOrders()
        val sel = _state.value.selectedClientId
        if (sel != 0L) _state.value = _state.value.copy(orders = db.getOrdersForClient(sel))
    }

    /** Calcule le métrage via Python (Chaquopy) pour un type+taille. */
    fun computeFabric(garmentType: String, size: Int, doublure: Boolean, ourletDouble: Boolean): Map<String, Any>? {
        return try {
            PythonBridge.calculateFabric(garmentType, size, doublure, ourletDouble)
        } catch (e: Exception) {
            _state.value = _state.value.copy(message = "Erreur Python: ${e.message}")
            null
        }
    }

    /** Export PDF de la fiche du client sélectionné, écrit dans filesDir. */
    fun exportSelectedPdf(): String? {
        val sel = _state.value.selectedClientId
        if (sel == 0L) return null
        val client = db.getClient(sel) ?: return null
        val m = db.getMeasurement(sel)
        val orders = db.getOrdersForClient(sel)
        val clientJson = JSONObject().apply {
            put("name", client.name); put("phone", client.phone)
            put("email", client.email); put("notes", client.notes)
        }.toString()
        val measJson = JSONObject().apply {
            m?.let {
                put("cou", it.cou); put("epaules", it.epaules); put("poitrine", it.poitrine)
                put("bras", it.bras); put("taille", it.taille); put("hanches", it.hanches)
                put("longueur_jambe", it.longueurJambe)
            }
        }.toString()
        val ordersArr = JSONArray()
        orders.forEach { o ->
            ordersArr.put(JSONObject().apply {
                put("status", o.status.name); put("garment_type", o.garmentType)
                put("size", o.size); put("fabric_m", o.fabricM)
            })
        }
        return try {
            val bytes = PythonBridge.exportPdf(clientJson, measJson, ordersArr.toString())
            val dir = getApplication<Application>().getExternalFilesDir(null)
            val file = java.io.File(dir, "fiche_${client.name.replace(' ', '_')}_$sel.pdf")
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            _state.value = _state.value.copy(message = "Export PDF échoué: ${e.message}")
            null
        }
    }

    fun listGarments(): List<String> = try { PythonBridge.listGarments() } catch (e: Exception) { emptyList() }
}
