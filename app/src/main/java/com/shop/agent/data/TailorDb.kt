package com.shop.agent.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Entité client. */
data class Client(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val notes: String = ""
)

/** Entité mesures (1:1 avec un client). Toutes les valeurs en cm. */
data class Measurement(
    val clientId: Long,
    val cou: Double = 0.0,
    val epaules: Double = 0.0,
    val poitrine: Double = 0.0,
    val bras: Double = 0.0,
    val taille: Double = 0.0,
    val hanches: Double = 0.0,
    val longueurJambe: Double = 0.0
)

/** Statuts de commande. */
enum class OrderStatus(val label: String) {
    EN_ATTENTE("En attente"),
    COUPE("Coupe"),
    COUTURE("Couture"),
    ESSAYAGE("Essayage"),
    LIVRE("Livré")
}

/** Entité commande (1:N avec un client). */
data class Order(
    val id: Long = 0,
    val clientId: Long,
    val garmentType: String,
    val size: Int,
    val status: OrderStatus,
    val fabricM: Double = 0.0,
    val createdAt: String = ""
)

/** Helper SQLite (aucune dépendance KSP/Room -> build robuste). */
class TailorDb(context: Context) : SQLiteOpenHelper(context, "tailor_agent.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE clients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT,
                email TEXT,
                notes TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE measurements (
                client_id INTEGER PRIMARY KEY,
                cou REAL, epaules REAL, poitrine REAL, bras REAL,
                taille REAL, hanches REAL, longueur_jambe REAL,
                FOREIGN KEY(client_id) REFERENCES clients(id) ON DELETE CASCADE)"""
        )
        db.execSQL(
            """CREATE TABLE orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                client_id INTEGER,
                garment_type TEXT,
                size INTEGER,
                status TEXT,
                fabric_m REAL,
                created_at TEXT,
                FOREIGN KEY(client_id) REFERENCES clients(id) ON DELETE CASCADE)"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS orders")
        db.execSQL("DROP TABLE IF EXISTS measurements")
        db.execSQL("DROP TABLE IF EXISTS clients")
        onCreate(db)
    }

    // ---- Clients ----
    fun insertClient(c: Client): Long {
        val v = ContentValues().apply {
            put("name", c.name)
            put("phone", c.phone)
            put("email", c.email)
            put("notes", c.notes)
        }
        return writableDatabase.insert("clients", null, v)
    }

    fun updateClient(c: Client) {
        val v = ContentValues().apply {
            put("name", c.name)
            put("phone", c.phone)
            put("email", c.email)
            put("notes", c.notes)
        }
        writableDatabase.update("clients", v, "id=?", arrayOf(c.id.toString()))
    }

    fun deleteClient(id: Long) {
        writableDatabase.delete("clients", "id=?", arrayOf(id.toString()))
    }

    fun getAllClients(): List<Client> {
        val list = mutableListOf<Client>()
        val cur = readableDatabase.rawQuery("SELECT id,name,phone,email,notes FROM clients ORDER BY name", null)
        cur.use {
            while (it.moveToNext()) {
                list.add(Client(it.getLong(0), it.getString(1), it.getString(2) ?: "", it.getString(3) ?: "", it.getString(4) ?: ""))
            }
        }
        return list
    }

    fun getClient(id: Long): Client? {
        val cur = readableDatabase.rawQuery("SELECT id,name,phone,email,notes FROM clients WHERE id=?", arrayOf(id.toString()))
        cur.use {
            if (it.moveToFirst()) return Client(it.getLong(0), it.getString(1), it.getString(2) ?: "", it.getString(3) ?: "", it.getString(4) ?: "")
        }
        return null
    }

    // ---- Measurements ----
    fun upsertMeasurement(m: Measurement) {
        writableDatabase.delete("measurements", "client_id=?", arrayOf(m.clientId.toString()))
        val v = ContentValues().apply {
            put("client_id", m.clientId)
            put("cou", m.cou); put("epaules", m.epaules); put("poitrine", m.poitrine)
            put("bras", m.bras); put("taille", m.taille); put("hanches", m.hanches)
            put("longueur_jambe", m.longueurJambe)
        }
        writableDatabase.insert("measurements", null, v)
    }

    fun getMeasurement(clientId: Long): Measurement? {
        val cur = readableDatabase.rawQuery("SELECT * FROM measurements WHERE client_id=?", arrayOf(clientId.toString()))
        cur.use {
            if (it.moveToFirst()) {
                return Measurement(
                    it.getLong(it.getColumnIndexOrThrow("client_id")),
                    it.getDouble(it.getColumnIndexOrThrow("cou")),
                    it.getDouble(it.getColumnIndexOrThrow("epaules")),
                    it.getDouble(it.getColumnIndexOrThrow("poitrine")),
                    it.getDouble(it.getColumnIndexOrThrow("bras")),
                    it.getDouble(it.getColumnIndexOrThrow("taille")),
                    it.getDouble(it.getColumnIndexOrThrow("hanches")),
                    it.getDouble(it.getColumnIndexOrThrow("longueur_jambe"))
                )
            }
        }
        return null
    }

    // ---- Orders ----
    fun insertOrder(o: Order): Long {
        val v = ContentValues().apply {
            put("client_id", o.clientId)
            put("garment_type", o.garmentType)
            put("size", o.size)
            put("status", o.status.name)
            put("fabric_m", o.fabricM)
            put("created_at", o.createdAt)
        }
        return writableDatabase.insert("orders", null, v)
    }

    fun updateOrderStatus(id: Long, status: OrderStatus) {
        val v = ContentValues().apply { put("status", status.name) }
        writableDatabase.update("orders", v, "id=?", arrayOf(id.toString()))
    }

    fun deleteOrder(id: Long) {
        writableDatabase.delete("orders", "id=?", arrayOf(id.toString()))
    }

    fun getOrdersForClient(clientId: Long): List<Order> {
        val list = mutableListOf<Order>()
        val cur = readableDatabase.rawQuery(
            "SELECT id,client_id,garment_type,size,status,fabric_m,created_at FROM orders WHERE client_id=? ORDER BY id DESC",
            arrayOf(clientId.toString())
        )
        cur.use {
            while (it.moveToNext()) {
                list.add(
                    Order(
                        it.getLong(0), it.getLong(1), it.getString(2), it.getInt(3),
                        OrderStatus.valueOf(it.getString(4)), it.getDouble(5),
                        it.getString(6) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun getAllOrders(): List<Order> {
        val list = mutableListOf<Order>()
        val cur = readableDatabase.rawQuery(
            "SELECT id,client_id,garment_type,size,status,fabric_m,created_at FROM orders ORDER BY id DESC", null
        )
        cur.use {
            while (it.moveToNext()) {
                list.add(
                    Order(
                        it.getLong(0), it.getLong(1), it.getString(2), it.getInt(3),
                        OrderStatus.valueOf(it.getString(4)), it.getDouble(5),
                        it.getString(6) ?: ""
                    )
                )
            }
        }
        return list
    }
}
