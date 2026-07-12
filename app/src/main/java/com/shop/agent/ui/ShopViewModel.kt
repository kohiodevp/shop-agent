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

    // Recherche produits (catalogue)
    private val _productQuery = MutableStateFlow("")
    val productQuery: StateFlow<String> = _productQuery
    val filteredProducts: StateFlow<List<Product>> = combine(products, _productQuery) { list, q ->
        if (q.isBlank()) list else list.filter {
            it.name.startsWith(q, ignoreCase = true) || it.barcode.startsWith(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recherche ventes
    private val _saleQuery = MutableStateFlow("")
    val saleQuery: StateFlow<String> = _saleQuery
    val filteredSales: StateFlow<List<Sale>> = combine(sales, _saleQuery) { list, q ->
        if (q.isBlank()) list else list.filter {
            it.items.contains(q, ignoreCase = true) || it.total.toString().startsWith(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartLines: MutableStateFlow<List<com.shop.agent.engine.CartLine>> = MutableStateFlow(emptyList())
    val cartTotal: MutableStateFlow<Double> = MutableStateFlow(0.0)
    val cartCount: StateFlow<Int> = cartLines.map { lines -> lines.sumOf { it.qty } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    // --- Catalogue ---
    fun setProductQuery(q: String) { _productQuery.value = q }

    fun addToCart(p: Product) {
        if (p.stock <= 0) { _uiMessage.tryEmit("Stock épuisé : ${p.name}"); return }
        val current = engine.lines().find { it.product.id == p.id }?.qty ?: 0
        if (current >= p.stock) { _uiMessage.tryEmit("Stock max atteint pour ${p.name}"); return }
        engine.add(p); refreshCart()
    }

    fun updateProduct(id: Long, name: String, price: Double, stock: Int) = viewModelScope.launch(Dispatchers.IO) {
        val p = db.productDao().getById(id) ?: return@launch
        db.productDao().update(p.copy(name = name, price = price, stock = stock))
        _uiMessage.tryEmit("Produit modifié : $name")
    }

    fun restockProduct(id: Long, qty: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (qty <= 0) return@launch
        db.productDao().incrementStock(id, qty)
        val p = db.productDao().getById(id)
        _uiMessage.tryEmit("Réappro +$qty : ${p?.name ?: ""}")
    }

    fun deleteProduct(p: Product) = viewModelScope.launch(Dispatchers.IO) {
        db.productDao().delete(p)
        _uiMessage.tryEmit("Produit supprimé : ${p.name}")
    }

    // --- Panier ---
    fun removeFromCart(id: Long) { engine.remove(id); refreshCart() }
    fun setCartQty(id: Long, qty: Int) { engine.setQty(id, qty); refreshCart() }
    fun incCartQty(id: Long, by: Int = 1) { engine.incrementQty(id, by); refreshCart() }
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

    // --- Ventes ---
    fun setSaleQuery(q: String) { _saleQuery.value = q }
    fun clearSales() = viewModelScope.launch(Dispatchers.IO) {
        db.saleDao().clear()
        _uiMessage.tryEmit("Historique des ventes effacé")
    }

    fun addProduct(name: String, price: Double, stock: Int) = viewModelScope.launch(Dispatchers.IO) {
        db.productDao().insert(Product(name = name, price = price, stock = stock))
        _uiMessage.tryEmit("Produit ajouté : $name")
    }
}
