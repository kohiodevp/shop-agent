package com.shop.agent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shop.agent.data.*
import com.shop.agent.engine.TailorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

data class UiState(
    val clients: List<Client> = emptyList(),
    val selectedClientId: Long = 0,
    val measurements: Measurement? = null,
    val orders: List<Order> = emptyList(),
    val allOrders: List<Order> = emptyList(),
    // ---- Retail / Boutique ----
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val stockMovements: List<StockMovement> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val todayRevenue: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val message: String = "",
    val busy: Boolean = false
)

class TailorViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TailorDb(app)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        refreshClients()
        refreshAllOrders()
        refreshShop()
    }

    // ===================== TAILLEUR (existant) =====================
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
        val now = dateFmt.format(Date())
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

    // ===================== BOUTIQUE / RETAIL =====================

    fun refreshShop() {
        val today = dateFmt.format(Date())
        _state.value = _state.value.copy(
            products = db.getAllProducts(),
            categories = db.getAllCategories(),
            customers = db.getAllCustomers(),
            sales = db.getAllSales(),
            stockMovements = db.getAllStockMovements(),
            suppliers = db.getAllSuppliers(),
            todayRevenue = db.getRevenueBetween(today, today),
            totalRevenue = db.getTotalRevenue()
        )
    }

    // ---- Catégories ----
    fun addCategory(name: String, description: String) {
        db.insertCategory(Category(name = name, description = description))
        refreshShop()
    }

    fun removeCategory(id: Long) {
        db.deleteCategory(id)
        refreshShop()
    }

    // ---- Produits ----
    fun addProduct(
        name: String,
        category: ProductCategory,
        description: String,
        buyPrice: Double,
        sellPrice: Double,
        stock: Int,
        alertThreshold: Int,
        barcode: String?
    ): Long {
        val now = dateFmt.format(Date())
        val id = db.insertProduct(
            Product(
                name = name, category = category, description = description,
                buyPrice = buyPrice, sellPrice = sellPrice, stock = stock,
                alertThreshold = alertThreshold, barcode = barcode, addedAt = now
            )
        )
        refreshShop()
        return id
    }

    fun editProduct(p: Product) {
        db.updateProduct(p)
        refreshShop()
    }

    fun removeProduct(id: Long) {
        db.deleteProduct(id)
        refreshShop()
    }

    fun restockProduct(id: Long, qty: Int, reason: String) {
        val p = db.getProduct(id) ?: return
        val newStock = p.stock + qty
        db.updateProductStock(id, newStock)
        db.insertStockMovement(
            StockMovement(
                productId = id, type = MovementType.ENTREE, quantity = qty,
                date = dateFmt.format(Date()), reason = reason.ifBlank { "réappro" }
            )
        )
        refreshShop()
    }

    // ---- Clients boutique ----
    fun addCustomer(name: String, phone: String, email: String?, address: String?) {
        val now = dateFmt.format(Date())
        db.insertCustomer(Customer(name = name, phone = phone, email = email, address = address, registeredAt = now))
        refreshShop()
    }

    fun removeCustomer(id: Long) {
        db.deleteCustomer(id)
        refreshShop()
    }

    // ---- Ventes ----
    /**
     * Enregistre une vente complète :
     * - crée la vente (montant = calcul moteur)
     * - insère les lignes
     * - décrémente le stock de chaque produit
     * - journalise un mouvement de sortie par produit
     * Retourne l'id de la vente, ou -1 si échec (stock insuffisant).
     */
    fun recordSale(
        customerId: Long?,
        items: List<SaleItemInput>,
        paymentMode: PaymentMode,
        note: String
    ): Long {
        // Validation du stock disponible
        for (it in items) {
            val p = db.getProduct(it.productId) ?: return -1
            if (!TailorEngine.canSell(p, it.quantity)) {
                _state.value = _state.value.copy(message = "Stock insuffisant pour ${p.name}")
                return -1
            }
        }
        val now = dateFmt.format(Date())
        val saleItems = items.map {
            SaleItem(saleId = 0, productId = it.productId, quantity = it.quantity, unitPrice = it.unitPrice, discount = it.discount)
        }
        val total = TailorEngine.calculateSaleTotal(saleItems)
        val saleId = db.insertSale(
            Sale(customerId = customerId, saleDate = now, totalAmount = total, paymentMode = paymentMode, note = note)
        )
        for (it in items) {
            db.insertSaleItem(
                SaleItem(saleId = saleId, productId = it.productId, quantity = it.quantity, unitPrice = it.unitPrice, discount = it.discount)
            )
            val p = db.getProduct(it.productId)!!
            val newStock = TailorEngine.stockAfterSale(p, saleItems)
            db.updateProductStock(it.productId, newStock)
            db.insertStockMovement(
                StockMovement(
                    productId = it.productId, type = MovementType.SORTIE, quantity = it.quantity,
                    date = now, reason = "vente #$saleId"
                )
            )
        }
        refreshShop()
        return saleId
    }

    // ---- Fournisseurs ----
    fun addSupplier(name: String, contact: String, productsSupplied: String) {
        db.insertSupplier(Supplier(name = name, contact = contact, productsSupplied = productsSupplied))
        refreshShop()
    }

    fun removeSupplier(id: Long) {
        db.deleteSupplier(id)
        refreshShop()
    }

    // ---- Aides UI ----
    fun lowStockProducts(): List<Product> = _state.value.products.filter { TailorEngine.isLowStock(it) }
}

/** Ligne de vente saisie dans l'UI avant enregistrement. */
data class SaleItemInput(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double = 0.0
)
