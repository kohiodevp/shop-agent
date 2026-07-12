package com.shop.agent.ui

import androidx.lifecycle.*
import com.shop.agent.data.*
import com.shop.agent.engine.ShopEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShopViewModel(private val db: ShopDb) : ViewModel() {
    private val engine = ShopEngine()
    val products: StateFlow<List<Product>> = db.productDao().all()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sales: StateFlow<List<Sale>> = db.saleDao().all()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartLines: MutableStateFlow<List<com.shop.agent.engine.CartLine>> =
        MutableStateFlow(emptyList())
    val cartTotal: MutableStateFlow<Double> = MutableStateFlow(0.0)
    val cartCount: StateFlow<Int> = cartLines.map { lines -> lines.sumOf { it.qty } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Messages UI (Snackbar)
    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    fun addToCart(p: Product) {
        if (p.stock <= 0) { _uiMessage.tryEmit("Stock épuisé : ${p.name}"); return }
        val current = engine.lines().find { it.product.id == p.id }?.qty ?: 0
        if (current >= p.stock) { _uiMessage.tryEmit("Stock max atteint pour ${p.name}"); return }
        engine.add(p); refreshCart()
    }
    fun removeFromCart(id: Long) { engine.remove(id); refreshCart() }
    fun clearCart() { engine.clear(); refreshCart() }

    private fun refreshCart() {
        cartLines.value = engine.lines()
        cartTotal.value = engine.total()
    }

    fun checkout() = viewModelScope.launch(Dispatchers.IO) {
        if (engine.lines().isEmpty()) { _uiMessage.tryEmit("Panier vide"); return@launch }
        val sale = engine.buildSale()
        db.saleDao().insert(sale)
        engine.lines().forEach { db.productDao().decrementStock(it.product.id, it.qty) }
        val total = sale.total.toInt()
        val n = engine.lines().size
        engine.clear()
        refreshCart()
        _uiMessage.tryEmit("Vente validée : $n article(s), $total FCFA")
    }

    fun addProduct(name: String, price: Double, stock: Int) = viewModelScope.launch(Dispatchers.IO) {
        db.productDao().insert(Product(name = name, price = price, stock = stock))
        _uiMessage.tryEmit("Produit ajouté : $name")
    }
}
