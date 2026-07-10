package com.shop.agent.engine

import com.shop.agent.data.Product
import com.shop.agent.data.SaleItem

/**
 * Moteur métier du Tailleur — version Kotlin pur (équivalent du moteur Python).
 * Calcul du métrage par type de vêtement, gradation des tailles,
 * export JSON/PDF. Aucune dépendance native.
 */

object TailorEngine {

    // Taux de métrage de base par type de vêtement (en mètres pour la taille de référence M)
    private val FABRIC_RATES = mapOf(
        "chemise" to 1.8,
        "pantalon" to 1.4,
        "costume" to 3.2,
        "robe" to 2.6,
        "jupe" to 1.2,
        "veste" to 2.0,
        "manteau" to 3.8
    )

    // Décrément/increment de métrage par écart de taille (une taille = +/- 8% autour de la réf M)
    private const val SIZE_STEP_FACTOR = 0.08

    /**
     * Calcule le métrage nécessaire pour un vêtement.
     * @param garmentType chemise|pantalon|costume|robe|jupe|veste|manteau
     * @param size taille client (ex: "S","M","L","XL" ou 36..52)
     * @param quantity nombre de pièces
     */
    fun calculateFabric(garmentType: String, size: String, quantity: Int = 1): Double {
        val base = FABRIC_RATES[garmentType.lowercase()]
            ?: throw IllegalArgumentException("Type de vêtement inconnu: $garmentType")
        val sizeFactor = sizeFactor(size)
        val perPiece = base * sizeFactor
        return Math.round(perPiece * quantity * 100.0) / 100.0
    }

    /**
     * Facteur de métrage selon la taille (M = référence 1.0).
     * Tailles lettres: XS=0.85, S=0.92, M=1.0, L=1.08, XL=1.16, XXL=1.24
     * Tailles numériques: écart de 2 points autour de 42 => +/- 4% par point.
     */
    private fun sizeFactor(size: String): Double {
        return when (size.uppercase()) {
            "XS" -> 0.85
            "S" -> 0.92
            "M" -> 1.0
            "L" -> 1.08
            "XL" -> 1.16
            "XXL" -> 1.24
            else -> {
                val num = size.toIntOrNull()
                if (num != null) {
                    val delta = (num - 42) / 2.0
                    1.0 + delta * SIZE_STEP_FACTOR
                } else 1.0
            }
        }
    }

    /**
     * Gradation d'un tableau de mesures selon un écart de taille.
     * @param measurements map nom->valeur (cm)
     * @param delta nombre de tailles d'écart (+1 = une taille au-dessus de la réf)
     * @return map gradée
     */
    fun grade(measurements: Map<String, Double>, delta: Int): Map<String, Double> {
        val factor = 1.0 + delta * SIZE_STEP_FACTOR
        return measurements.mapValues { Math.round(it.value * factor * 10.0) / 10.0 }
    }

    /**
     * Export JSON des données d'une commande.
     */
    fun exportJson(order: Map<String, Any>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        order.entries.forEachIndexed { i, (k, v) ->
            val comma = if (i < order.size - 1) "," else ""
            sb.append("  \"$k\": ${jsonValue(v)}$comma\n")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun jsonValue(v: Any): String {
        return when (v) {
            is String -> "\"${v.replace("\"", "\\\"")}\""
            is Map<*, *> -> exportJson(v as Map<String, Any>)
            else -> v.toString()
        }
    }

    /**
     * Export PDF minimal (writer pur, aucune lib native).
     * Génère un PDF valide contenant le texte fourni ligne par ligne.
     */
    fun exportPdf(title: String, lines: List<String>): ByteArray {
        val content = mutableListOf<String>()
        content.add("BT /F1 16 Tf 50 780 Td ($title) Tj ET")
        var y = 750
        for (line in lines) {
            content.add("BT /F1 11 Tf 50 $y Td (${escape(line)}) Tj ET")
            y -= 18
        }
        val objects = mutableListOf<String>()
        objects.add("<< /Type /Catalog /Pages 2 0 R >>")
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>")
        objects.add("<< /Length ${content.joinToString("\n").toByteArray().size} >>\nstream\n${content.joinToString("\n")}\nendstream")
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")

        val out = StringBuilder()
        out.append("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objects.forEachIndexed { idx, obj ->
            offsets.add(out.length)
            out.append("${idx + 1} 0 obj\n$obj\nendobj\n")
        }
        val xrefPos = out.length
        out.append("xref\n0 ${objects.size + 1}\n")
        out.append("0000000000 65535 f \n")
        offsets.forEach { out.append("%010d 00000 n \n".format(it)) }
        out.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        out.append("startxref\n$xrefPos\n%%EOF")
        return out.toString().toByteArray(Charsets.ISO_8859_1)
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

    // ===================== RETAIL / BOUTIQUE =====================

    /**
     * Calcule le montant total d'une vente = somme(ligne.prix_unitaire * quantité - remise).
     */
    fun calculateSaleTotal(items: List<SaleItem>): Double {
        var total = 0.0
        for (it in items) total += it.unitPrice * it.quantity - it.discount
        return Math.round(total * 100.0) / 100.0
    }

    /**
     * Calcule le stock restant après une vente (décrémente le stock du produit).
     * Garantit un stock >= 0.
     */
    fun stockAfterSale(product: Product, items: List<SaleItem>): Int {
        val sold = items.filter { it.productId == product.id }.sumOf { it.quantity }
        return maxOf(0, product.stock - sold)
    }

    /**
     * Vrai si le stock est sous (ou égal) au seuil d'alerte.
     */
    fun isLowStock(product: Product): Boolean = product.stock <= product.alertThreshold

    /**
     * Marge unitaire d'un produit (prix_vente - prix_achat).
     */
    fun margin(product: Product): Double = Math.round((product.sellPrice - product.buyPrice) * 100.0) / 100.0

    /**
     * Vrai si le stock est suffisant pour la quantité demandée (pour une vente).
     */
    fun canSell(product: Product, quantity: Int): Boolean = product.stock >= quantity
}
