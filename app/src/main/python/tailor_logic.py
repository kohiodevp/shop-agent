"""
tailor_logic.py — Moteur métier du Tailor Agent (exécuté via Chaquopy).

Responsabilités :
  * Calcul du métrage de tissu nécessaire selon le type de vêtement.
  * Gradation des tailles (calcul des deltas par point de mesure).
  * Export des fiches client/mesures en JSON et en PDF (writer pur-Python,
    zéro dépendance native -> garanti installable sur Android via Chaquopy).

Toutes les fonctions sont pensées pour être appelées depuis Kotlin :
    Python.getInstance().getModule("tailor_logic").callAttr("calculate_fabric", ...)
"""

import json
import zlib
import struct
from datetime import datetime, date


# --------------------------------------------------------------------------- #
# 1. MODÈLES DE MÉTRAGE PAR TYPE DE VÊTEMENT
# --------------------------------------------------------------------------- #
# Unités : le métrage est exprimé en METRES (m) de tissu d'une laize (largeur)
# standard de 1.50 m. Les coefficients intègrent les chutes, ourlets et doublures.
GARMENT_MODELS = {
    "chemise": {
        "label": "Chemise",
        "base_m": 1.60,          # consommation de base pour une taille M
        "per_size_m": 0.06,      # incrément par taille au-dessus de M (38)
        "base_size": 38,
        "laize_m": 1.50,
    },
    "pantalon": {
        "label": "Pantalon",
        "base_m": 1.30,
        "per_size_m": 0.05,
        "base_size": 40,
        "laize_m": 1.50,
    },
    "costume": {
        "label": "Costume (veste + pantalon)",
        "base_m": 3.20,
        "per_size_m": 0.12,
        "base_size": 48,
        "laize_m": 1.50,
    },
    "veste": {
        "label": "Veste seule",
        "base_m": 1.90,
        "per_size_m": 0.08,
        "base_size": 48,
        "laize_m": 1.50,
    },
    "robe": {
        "label": "Robe",
        "base_m": 2.40,
        "per_size_m": 0.10,
        "base_size": 38,
        "laize_m": 1.50,
    },
    "jupe": {
        "label": "Jupe",
        "base_m": 0.90,
        "per_size_m": 0.04,
        "base_size": 38,
        "laize_m": 1.50,
    },
}


def list_garments():
    """Retourne la liste des types de vêtements disponibles."""
    return sorted(GARMENT_MODELS.keys())


def calculate_fabric(garment_type, size, options=None):
    """
    Calcule le métrage de tissu nécessaire.

    :param garment_type: clé de GARMENT_MODELS (ex. 'chemise')
    :param size: taille numérique (ex. 40)
    :param options: dict optionnel {'doublure': bool, 'ourlet_double': bool}
    :return: dict {garment_type, label, size, laize_m, fabric_m, notes}
    """
    model = GARMENT_MODELS.get(garment_type)
    if model is None:
        raise ValueError("Type de vêtement inconnu: %s" % garment_type)

    options = options or {}
    delta_sizes = max(0, size - model["base_size"])
    fabric = model["base_m"] + delta_sizes * model["per_size_m"]

    notes = []
    if options.get("doublure"):
        fabric += 0.40
        notes.append("+0.40 m (doublure)")
    if options.get("ourlet_double"):
        fabric += 0.15
        notes.append("+0.15 m (ourlet double)")

    # Arrondi au centimètre supérieur pour la commande tissu
    fabric = round(fabric + 0.005, 2)
    return {
        "garment_type": garment_type,
        "label": model["label"],
        "size": size,
        "laize_m": model["laize_m"],
        "fabric_m": fabric,
        "notes": notes,
    }


# --------------------------------------------------------------------------- #
# 2. GRADATION DES TAILLES
# --------------------------------------------------------------------------- #
# Deltas (en cm) par point de mesure pour UNE taille au-dessus de la base.
# Négatifs = plus petit. Source : gradation tailleur standard (Europe).
GRADING_DELTAS = {
    "cou": 1.0,
    "poitrine": 4.0,
    "epaules": 1.5,
    "bras": 1.0,
    "taille": 4.0,
    "hanches": 4.0,
    "longueur_jambe": 1.5,
}

BASE_SIZE = "M"  # taille de référence pour les deltas ci-dessus


def grade_size(measurements, target_size, base_measurements=None):
    """
    Gradate une fiche de mesures vers une taille cible.

    :param measurements: dict des mesures de base {point: cm}
    :param target_size: int taille cible (ex. 42)
    :param base_measurements: dict optionnel des mesures de la base_size
    :return: dict {target_size, graded: {point: cm}, deltas: {point: cm}}
    """
    base_measurements = base_measurements or measurements
    # On gradue selon l'écart de taille par rapport à la taille de référence.
    # Pas de 2 tailles = 1 unité de delta.
    step = 2
    ref_size = base_measurements.get("__size__", target_size)
    if isinstance(ref_size, str):
        ref_size = target_size
    units = (target_size - ref_size) / step

    graded = {}
    deltas = {}
    for point, delta_per_unit in GRADING_DELTAS.items():
        base_val = float(measurements.get(point, 0.0))
        d = delta_per_unit * units
        graded[point] = round(base_val + d, 1)
        deltas[point] = round(d, 1)

    return {
        "target_size": target_size,
        "base_size": ref_size,
        "graded": graded,
        "deltas": deltas,
    }


# --------------------------------------------------------------------------- #
# 3. EXPORT JSON
# --------------------------------------------------------------------------- #
def export_measurements_json(client, measurements, orders=None):
    """
    Sérialise une fiche complète en JSON (string).
    :param client: dict {id, name, phone, email, notes}
    :param measurements: dict {point: cm, ...}
    :param orders: list de dict commandes
    """
    payload = {
        "schema": "tailor-agent/measurements/v1",
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "client": client,
        "measurements": measurements,
        "orders": orders or [],
    }
    return json.dumps(payload, ensure_ascii=False, indent=2)


# --------------------------------------------------------------------------- #
# 4. EXPORT PDF — writer minimal pur-Python (aucune dépendance)
# --------------------------------------------------------------------------- #
def _pdf_escape(text):
    return (text.replace("\\", r"\\")
                .replace("(", r"\(")
                .replace(")", r"\)"))


def _pdf_text_obj(lines, font_size=11):
    """Construit le contenu d'un stream de texte pour une page A4."""
    out = ["BT", "/F1 %d Tf" % font_size, "50 790 Td", "14 TL"]
    first = True
    for line in lines:
        if first:
            out.append("(%s) Tj" % _pdf_escape(line))
            first = False
        else:
            out.append("T* (%s) Tj" % _pdf_escape(line))
    out.append("ET")
    return "\n".join(out)


def export_measurements_pdf(client, measurements, orders=None):
    """
    Génère un PDF minimal (1+ pages) contenant la fiche client.
    Retourne les bytes du fichier PDF.
    """
    orders = orders or []
    now = datetime.now().strftime("%Y-%m-%d %H:%M")

    lines = []
    lines.append("FICHE DE MESURES - TAILOR AGENT")
    lines.append("Genere le : %s" % now)
    lines.append("=" * 40)
    lines.append("Client       : %s" % client.get("name", ""))
    lines.append("Telephone    : %s" % client.get("phone", ""))
    lines.append("Email        : %s" % client.get("email", ""))
    lines.append("Notes        : %s" % client.get("notes", ""))
    lines.append("=" * 40)
    lines.append("MESURES (cm)")
    for point in ["cou", "epaules", "poitrine", "bras", "taille", "hanches", "longueur_jambe"]:
        if point in measurements:
            lines.append("  %-18s : %s" % (point, measurements[point]))
    lines.append("=" * 40)
    lines.append("COMMANDES (%d)" % len(orders))
    for o in orders:
        lines.append("  [%s] %s - %s" % (o.get("status", "?"),
                                        o.get("garment_type", ""),
                                        o.get("size", "")))
        if o.get("fabric_m"):
            lines.append("      tissu: %s m" % o.get("fabric_m"))

    # Découpe en pages de 46 lignes max
    pages = [lines[i:i + 46] for i in range(0, len(lines), 46)] or [lines]

    objects = []
    font_id = 3
    page_ids = []
    content_ids = []
    next_id = 4
    for _ in pages:
        cid = next_id
        next_id += 1
        pid = next_id
        next_id += 1
        content_ids.append(cid)
        page_ids.append(pid)

    catalog = b"<< /Type /Catalog /Pages 2 0 R >>"
    kids = " ".join("%d 0 R" % pid for pid in page_ids)
    pages_obj = ("<< /Type /Pages /Count %d /Kids [%s] >>" %
                 (len(page_ids), kids)).encode()
    font_obj = b"<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>"

    obj_map = {1: catalog, 2: pages_obj, 3: font_obj}
    for i, cid in enumerate(content_ids):
        data = _enc(pages[i])
        stream = b"<< /Length %d >>\nstream\n%s\nendstream" % (len(data), data)
        obj_map[cid] = stream
    for i, pid in enumerate(page_ids):
        obj_map[pid] = ("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                        "/Resources << /Font << /F1 3 0 R >> >> /Contents %d 0 R >>"
                        % content_ids[i]).encode()

    buf = bytearray()
    buf += b"%PDF-1.4\n"
    offsets = [0] * next_id
    offsets[0] = 0
    for oid in range(1, next_id):
        offsets[oid] = len(buf)
        obj = b"%d 0 obj\n%s\nendobj\n" % (oid, obj_map[oid])
        buf += obj
    xref_pos = len(buf)
    n = next_id
    xref = bytearray()
    xref += b"xref\n"
    xref += ("%d %d\n" % (0, n)).encode()
    xref += b"0000000000 65535 f \n"
    for oid in range(1, n):
        xref += ("%010d 00000 n \n" % offsets[oid]).encode()
    xref += b"trailer\n"
    xref += ("<< /Size %d /Root 1 0 R >>\n" % n).encode()
    xref += b"startxref\n%d\n%%%%EOF" % xref_pos
    buf += xref
    return bytes(buf)


def _enc(lines):
    return _pdf_text_obj(lines, font_size=11).encode("latin-1", "replace")


# --------------------------------------------------------------------------- #
# 5. HELPERS KOTLIN <-> PYTHON
# --------------------------------------------------------------------------- #
def summarize_client(client_json, measurements_json, orders_json):
    """Fonction glue utilisée par l'UI Kotlin pour produire un résumé texte."""
    client = json.loads(client_json) if isinstance(client_json, str) else client_json
    measurements = json.loads(measurements_json) if isinstance(measurements_json, str) else measurements_json
    orders = json.loads(orders_json) if isinstance(orders_json, str) else orders_json
    lines = []
    lines.append("Client: %s" % client.get("name", "?"))
    lines.append("Mesures: %s" % ", ".join("%s=%s" % (k, v) for k, v in measurements.items()))
    lines.append("Commandes: %d" % len(orders))
    for o in orders:
        fabric = calculate_fabric(o.get("garment_type", "chemise"), int(o.get("size", 38)))
        lines.append("  - %s taille %s -> %s m" %
                     (o.get("garment_type"), o.get("size"), fabric["fabric_m"]))
    return "\n".join(lines)


if __name__ == "__main__":
    m = {"cou": 38, "epaules": 44, "poitrine": 100, "bras": 62,
         "taille": 84, "hanches": 98, "longueur_jambe": 102}
    print(calculate_fabric("chemise", 40))
    print(grade_size(m, 42))
    print(export_measurements_json({"name": "Test"}, m))
    pdf = export_measurements_pdf({"name": "Test"}, m)
    print("PDF bytes:", len(pdf))
