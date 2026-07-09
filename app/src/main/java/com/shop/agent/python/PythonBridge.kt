package com.shop.agent.python

import com.shop.agent.engine.TailorEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pont vers le moteur métier du Tailleur.
 *
 * NOTE: initialement prévu via Chaquopy (Python embarqué), mais le plugin
 * Chaquopy 12.0.0 (seule version avec runtime publié sur chaquo.com/maven)
 * casse l'enregistrement de l'extension android dans la chaîne de build
 * Docker (Gradle 7.3.3 / AGP 7.0.4 / JDK 17). Le moteur a donc été réécrit
 * en Kotlin pur (TailorEngine) avec une logique strictement équivalente
 * à tailor_logic.py. Le module Python reste présent à titre de référence.
 */
object PythonBridge {

    /** Types de vêtements gérés (équivalent list_garments()). */
    fun listGarments(): List<String> =
        listOf("chemise", "pantalon", "costume", "robe", "jupe", "veste", "manteau")

    /**
     * Calcule le métrage pour un vêtement + taille (taille numérique entière).
     * doublure / ourletDouble ajustent le métrage (+15% / +5%).
     */
    fun calculateFabric(
        garmentType: String,
        size: Int,
        doublure: Boolean,
        ourletDouble: Boolean
    ): Map<String, Any> {
        var fm = TailorEngine.calculateFabric(garmentType, size.toString(), 1)
        if (doublure) fm += fm * 0.15
        if (ourletDouble) fm += fm * 0.05
        fm = Math.round(fm * 100.0) / 100.0
        return mapOf(
            "garment_type" to garmentType,
            "size" to size,
            "doublure" to doublure,
            "ourlet_double" to ourletDouble,
            "fabric_m" to fm.toString()
        )
    }

    /** Grade une fiche de mesures vers une taille cible (delta en tailles). */
    fun gradeSize(measurements: Map<String, Double>, targetSize: Int): Map<String, Any> {
        val delta = (targetSize - 42) / 2
        val graded = TailorEngine.grade(measurements, delta)
        return mapOf("target_size" to targetSize, "graded" to graded)
    }

    /** Export JSON d'une fiche complète. */
    fun exportJson(clientJson: String, measurementsJson: String, ordersJson: String): String {
        val order = mapOf(
            "client" to (safeJson(clientJson) ?: emptyMap<String, Any>()),
            "measurements" to (safeJson(measurementsJson) ?: emptyMap<String, Any>()),
            "orders" to (safeJson(ordersJson) ?: emptyList<Any>())
        )
        return TailorEngine.exportJson(order)
    }

    /** Export PDF (bytes) d'une fiche complète. */
    fun exportPdf(clientJson: String, measurementsJson: String, ordersJson: String): ByteArray {
        val lines = mutableListOf<String>()
        lines.add("Client: ${optString(clientJson, "name")}")
        lines.add("Telephone: ${optString(clientJson, "phone")}")
        lines.add("Email: ${optString(clientJson, "email")}")
        lines.add("Notes: ${optString(clientJson, "notes")}")
        lines.add("")
        lines.add("Mesures (cm):")
        optJsonObject(measurementsJson)?.let { m ->
            m.keys().forEach { k -> lines.add("  $k: ${m.opt(k)}") }
        }
        lines.add("")
        lines.add("Commandes:")
        optJsonArray(ordersJson)?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i)
                lines.add("  - ${o?.optString("garment_type")} taille ${o?.opt("size")} [${o?.optString("status")}] ${o?.opt("fabric_m")} m")
            }
        }
        return TailorEngine.exportPdf("Fiche Tailleur", lines)
    }

    fun summarize(clientJson: String, measurementsJson: String, ordersJson: String): String {
        val n = optJsonArray(ordersJson)?.length() ?: 0
        return "Client ${optString(clientJson, "name")} — $n commande(s)."
    }

    private fun safeJson(s: String): Any? = try {
        val j = JSONObject(s)
        j.keys().asSequence().associateWith { j.opt(it) }
    } catch (_: Exception) { null }

    private fun optJsonObject(s: String): JSONObject? = try { JSONObject(s) } catch (_: Exception) { null }
    private fun optJsonArray(s: String): JSONArray? = try { JSONArray(s) } catch (_: Exception) { null }
    private fun optString(json: String, key: String): String = optJsonObject(json)?.optString(key) ?: ""
}
