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

    fun addToCart(p: Product) { engine.add(p); refreshCart() }
    fun removeFromCart(id: Long) { engine.remove(id); refreshCart() }
    fun clearCart() { engine.clear(); refreshCart() }

    private fun refreshCart() {
        cartLines.value = engine.lines()
        cartTotal.value = engine.total()
    }

    fun checkout() = viewModelScope.launch(Dispatchers.IO) {
        if (engine.lines().isEmpty()) return@launch
        val sale = engine.buildSale()
        db.saleDao().insert(sale)
        engine.lines().forEach { db.productDao().decrementStock(it.product.id, it.qty) }
        engine.clear()
        refreshCart()
    }

    fun addProduct(name: String, price: Double, stock: Int) = viewModelScope.launch(Dispatchers.IO) {
        db.productDao().insert(Product(name = name, price = price, stock = stock))
    }
}
