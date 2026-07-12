package com.shop.agent.python

import com.shop.agent.data.Product
import com.shop.agent.engine.ShopEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pont vers la logique metier de la boutique.
 * Le moteur ShopEngine (Kotlin) gere le panier, le total et les ventes.
 * Le module Python shop_logic.py reste la reference pour les tickets / stats.
 */
object PythonBridge {

    /** Calcule le total d'un panier (liste de produits + quantites). */
    fun calcCartTotal(itemsJson: String): Double {
        val arr = JSONArray(itemsJson)
        var total = 0.0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val price = o.optDouble("price", 0.0)
            val qty = o.optInt("qty", 0)
            total += price * qty
        }
        return total
    }

    /** Genere un ticket JSON compact pour une vente. */
    fun buildReceipt(saleJson: String): String {
        val arr = JSONArray(saleJson)
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val line = JSONObject()
            line.put("name", o.optString("name"))
            line.put("price", o.optDouble("price"))
            line.put("qty", o.optInt("qty"))
            out.put(line)
        }
        return out.toString()
    }

    /** Export JSON d'un catalogue produit. */
    fun exportCatalog(products: List<Product>): String {
        val arr = JSONArray()
        products.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("price", p.price)
            o.put("stock", p.stock)
            arr.put(o)
        }
        return arr.toString()
    }

    /** Reference au moteur (pour tests / bridging). */
    fun engine(): ShopEngine = ShopEngine()
}
