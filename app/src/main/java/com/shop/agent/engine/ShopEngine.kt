package com.shop.agent.engine

import com.shop.agent.data.Product
import com.shop.agent.data.Sale
import org.json.JSONArray
import org.json.JSONObject

data class CartLine(val product: Product, var qty: Int)

class ShopEngine {
    private val cart = mutableListOf<CartLine>()

    fun add(product: Product, qty: Int = 1) {
        val line = cart.find { it.product.id == product.id }
        if (line != null) line.qty += qty
        else cart.add(CartLine(product, qty))
    }

    fun remove(productId: Long) {
        cart.removeIf { it.product.id == productId }
    }

    fun lines(): List<CartLine> = cart.toList()

    fun count(): Int = cart.sumOf { it.qty }

    fun total(): Double = cart.sumOf { it.qty * it.product.price }

    fun clear() = cart.clear()

    fun buildSale(): Sale {
        val arr = JSONArray()
        cart.forEach {
            val o = JSONObject()
            o.put("id", it.product.id)
            o.put("name", it.product.name)
            o.put("price", it.product.price)
            o.put("qty", it.qty)
            arr.put(o)
        }
        return Sale(total = total(), items = arr.toString())
    }
}
