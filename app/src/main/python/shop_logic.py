# shop_logic.py - logique metier cote Python (Chaquopy)
# Genere des stats de vente et gere la nomenclature produits.
import json
from datetime import datetime


def format_receipt(sale_json: str) -> str:
    """Transforme le JSON d'une vente en ticket lisible."""
    items = json.loads(sale_json)
    lines = ["=== SHOP AGENT ===", datetime.now().strftime("%Y-%m-%d %H:%M")]
    total = 0.0
    for it in items:
        sub = it["price"] * it["qty"]
        total += sub
        lines.append(f"${it['name']} x${it['qty']} = ${sub:.2f} FCFA")
    lines.append(f"TOTAL: ${total:.2f} FCFA")
    return "
".join(lines)


def low_stock(products: list, threshold: int = 5) -> list:
    """Retourne les produits sous le seuil de stock."""
    return [p for p in products if p.get("stock", 0) < threshold]


if __name__ == "__main__":
    print(format_receipt('[{"name":"Huile","price":1500,"qty":2}]'))
