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

// ===================== RETAIL / BOUTIQUE =====================

/** Catégories de produits. */
enum class ProductCategory(val label: String) {
    VETEMENT("Vêtement"),
    CHAUSSURE("Chaussure"),
    ACCESSOIRE("Accessoire"),
    AUTRE("Autre")
}

/** Modes de paiement d'une vente. */
enum class PaymentMode(val label: String) {
    ESPECE("Espèce"),
    CARTE("Carte"),
    MOBILE_MONEY("Mobile Money"),
    VIREMENT("Virement")
}

/** Type de mouvement de stock. */
enum class MovementType(val label: String) {
    ENTREE("Entrée"),
    SORTIE("Sortie")
}

/** Entité catégorie (table categories). */
data class Category(
    val id: Long = 0,
    val name: String,
    val description: String = ""
)

/** Entité produit. */
data class Product(
    val id: Long = 0,
    val name: String,
    val category: ProductCategory = ProductCategory.AUTRE,
    val description: String = "",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Int = 0,
    val alertThreshold: Int = 0,
    val barcode: String? = null,
    val addedAt: String = ""
)

/** Entité client boutique (réutilise la structure Client existante via Customer). */
data class Customer(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String? = null,
    val address: String? = null,
    val registeredAt: String = ""
)

/** Entité vente (comptoir ou liée à un client). */
data class Sale(
    val id: Long = 0,
    val customerId: Long? = null,
    val saleDate: String = "",
    val totalAmount: Double = 0.0,
    val paymentMode: PaymentMode = PaymentMode.ESPECE,
    val note: String = ""
)

/** Entité ligne de vente. */
data class SaleItem(
    val id: Long = 0,
    val saleId: Long = 0,
    val productId: Long = 0,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0
) {
    val lineTotal: Double get() = unitPrice * quantity - discount
}

/** Entité mouvement de stock. */
data class StockMovement(
    val id: Long = 0,
    val productId: Long = 0,
    val type: MovementType = MovementType.ENTREE,
    val quantity: Int = 0,
    val date: String = "",
    val reason: String = ""
)

/** Entité fournisseur (optionnel). */
data class Supplier(
    val id: Long = 0,
    val name: String,
    val contact: String = "",
    val productsSupplied: String = ""
)

/** Helper SQLite (aucune dépendance KSP/Room -> build robuste). */
class TailorDb(context: Context) : SQLiteOpenHelper(context, "tailor_agent.db", null, 2) {

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
        // ---- Boutique / Retail ----
        db.execSQL(
            """CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT,
                description TEXT,
                buy_price REAL,
                sell_price REAL,
                stock INTEGER DEFAULT 0,
                alert_threshold INTEGER DEFAULT 0,
                barcode TEXT,
                added_at TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT,
                email TEXT,
                address TEXT,
                registered_at TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER,
                sale_date TEXT,
                total_amount REAL,
                payment_mode TEXT,
                note TEXT,
                FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL)"""
        )
        db.execSQL(
            """CREATE TABLE sale_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER,
                product_id INTEGER,
                quantity INTEGER,
                unit_price REAL,
                discount REAL DEFAULT 0,
                FOREIGN KEY(sale_id) REFERENCES sales(id) ON DELETE CASCADE,
                FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE SET NULL)"""
        )
        db.execSQL(
            """CREATE TABLE stock_movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER,
                type TEXT,
                quantity INTEGER,
                date TEXT,
                reason TEXT,
                FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE)"""
        )
        db.execSQL(
            """CREATE TABLE suppliers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                contact TEXT,
                products_supplied TEXT)"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS suppliers")
        db.execSQL("DROP TABLE IF EXISTS stock_movements")
        db.execSQL("DROP TABLE IF EXISTS sale_items")
        db.execSQL("DROP TABLE IF EXISTS sales")
        db.execSQL("DROP TABLE IF EXISTS customers")
        db.execSQL("DROP TABLE IF EXISTS products")
        db.execSQL("DROP TABLE IF EXISTS categories")
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

    // ============ CATÉGORIES ============
    fun insertCategory(c: Category): Long {
        val v = ContentValues().apply {
            put("name", c.name)
            put("description", c.description)
        }
        return writableDatabase.insert("categories", null, v)
    }

    fun getAllCategories(): List<Category> {
        val list = mutableListOf<Category>()
        val cur = readableDatabase.rawQuery("SELECT id,name,description FROM categories ORDER BY name", null)
        cur.use {
            while (it.moveToNext()) {
                list.add(Category(it.getLong(0), it.getString(1), it.getString(2) ?: ""))
            }
        }
        return list
    }

    fun deleteCategory(id: Long) {
        writableDatabase.delete("categories", "id=?", arrayOf(id.toString()))
    }

    // ============ PRODUITS ============
    fun insertProduct(p: Product): Long {
        val v = ContentValues().apply {
            put("name", p.name)
            put("category", p.category.name)
            put("description", p.description)
            put("buy_price", p.buyPrice)
            put("sell_price", p.sellPrice)
            put("stock", p.stock)
            put("alert_threshold", p.alertThreshold)
            put("barcode", p.barcode)
            put("added_at", p.addedAt)
        }
        return writableDatabase.insert("products", null, v)
    }

    fun updateProduct(p: Product) {
        val v = ContentValues().apply {
            put("name", p.name)
            put("category", p.category.name)
            put("description", p.description)
            put("buy_price", p.buyPrice)
            put("sell_price", p.sellPrice)
            put("stock", p.stock)
            put("alert_threshold", p.alertThreshold)
            put("barcode", p.barcode)
            put("added_at", p.addedAt)
        }
        writableDatabase.update("products", v, "id=?", arrayOf(p.id.toString()))
    }

    fun deleteProduct(id: Long) {
        writableDatabase.delete("products", "id=?", arrayOf(id.toString()))
    }

    fun getAllProducts(): List<Product> {
        val list = mutableListOf<Product>()
        val cur = readableDatabase.rawQuery(
            "SELECT id,name,category,description,buy_price,sell_price,stock,alert_threshold,barcode,added_at FROM products ORDER BY name", null
        )
        cur.use {
            while (it.moveToNext()) {
                list.add(Product(
                    it.getLong(0), it.getString(1),
                    ProductCategory.valueOf(it.getString(2) ?: "AUTRE"),
                    it.getString(3) ?: "", it.getDouble(4), it.getDouble(5),
                    it.getInt(6), it.getInt(7), it.getString(8),
                    it.getString(9) ?: ""
                ))
            }
        }
        return list
    }

    fun getProduct(id: Long): Product? {
        val cur = readableDatabase.rawQuery(
            "SELECT id,name,category,description,buy_price,sell_price,stock,alert_threshold,barcode,added_at FROM products WHERE id=?", arrayOf(id.toString())
        )
        cur.use {
            if (it.moveToFirst()) return Product(
                it.getLong(0), it.getString(1),
                ProductCategory.valueOf(it.getString(2) ?: "AUTRE"),
                it.getString(3) ?: "", it.getDouble(4), it.getDouble(5),
                it.getInt(6), it.getInt(7), it.getString(8), it.getString(9) ?: ""
            )
        }
        return null
    }

    /** Produits sous le seuil d'alerte (stock <= alert_threshold). */
    fun getLowStockProducts(): List<Product> {
        return getAllProducts().filter { it.stock <= it.alertThreshold }
    }

    fun updateProductStock(id: Long, newStock: Int) {
        val v = ContentValues().apply { put("stock", newStock) }
        writableDatabase.update("products", v, "id=?", arrayOf(id.toString()))
    }

    // ============ CLIENTS BOUTIQUE ============
    fun insertCustomer(c: Customer): Long {
        val v = ContentValues().apply {
            put("name", c.name)
            put("phone", c.phone)
            put("email", c.email)
            put("address", c.address)
            put("registered_at", c.registeredAt)
        }
        return writableDatabase.insert("customers", null, v)
    }

    fun updateCustomer(c: Customer) {
        val v = ContentValues().apply {
            put("name", c.name)
            put("phone", c.phone)
            put("email", c.email)
            put("address", c.address)
            put("registered_at", c.registeredAt)
        }
        writableDatabase.update("customers", v, "id=?", arrayOf(c.id.toString()))
    }

    fun deleteCustomer(id: Long) {
        writableDatabase.delete("customers", "id=?", arrayOf(id.toString()))
    }

    fun getAllCustomers(): List<Customer> {
        val list = mutableListOf<Customer>()
        val cur = readableDatabase.rawQuery("SELECT id,name,phone,email,address,registered_at FROM customers ORDER BY name", null)
        cur.use {
            while (it.moveToNext()) {
                list.add(Customer(
                    it.getLong(0), it.getString(1), it.getString(2) ?: "",
                    it.getString(3), it.getString(4), it.getString(5) ?: ""
                ))
            }
        }
        return list
    }

    // ============ VENTES ============
    fun insertSale(s: Sale): Long {
        val v = ContentValues().apply {
            put("customer_id", s.customerId)
            put("sale_date", s.saleDate)
            put("total_amount", s.totalAmount)
            put("payment_mode", s.paymentMode.name)
            put("note", s.note)
        }
        return writableDatabase.insert("sales", null, v)
    }

    fun insertSaleItem(it: SaleItem): Long {
        val v = ContentValues().apply {
            put("sale_id", it.saleId)
            put("product_id", it.productId)
            put("quantity", it.quantity)
            put("unit_price", it.unitPrice)
            put("discount", it.discount)
        }
        return writableDatabase.insert("sale_items", null, v)
    }

    fun getAllSales(): List<Sale> {
        val list = mutableListOf<Sale>()
        val cur = readableDatabase.rawQuery("SELECT id,customer_id,sale_date,total_amount,payment_mode,note FROM sales ORDER BY id DESC", null)
        cur.use {
            while (it.moveToNext()) {
                val cid = it.getLong(1)
                list.add(Sale(
                    it.getLong(0),
                    if (cid > 0) cid else null,
                    it.getString(2) ?: "", it.getDouble(3),
                    PaymentMode.valueOf(it.getString(4) ?: "ESPECE"),
                    it.getString(5) ?: ""
                ))
            }
        }
        return list
    }

    fun getSaleItems(saleId: Long): List<SaleItem> {
        val list = mutableListOf<SaleItem>()
        val cur = readableDatabase.rawQuery(
            "SELECT id,sale_id,product_id,quantity,unit_price,discount FROM sale_items WHERE sale_id=?", arrayOf(saleId.toString())
        )
        cur.use {
            while (it.moveToNext()) {
                list.add(SaleItem(
                    it.getLong(0), it.getLong(1), it.getLong(2), it.getInt(3), it.getDouble(4), it.getDouble(5)
                ))
            }
        }
        return list
    }

    /** Ventes du jour (date = aujourd'hui, format yyyy-MM-dd). */
    fun getSalesForToday(today: String): List<Sale> {
        val list = mutableListOf<Sale>()
        val cur = readableDatabase.rawQuery("SELECT id,customer_id,sale_date,total_amount,payment_mode,note FROM sales WHERE sale_date=? ORDER BY id DESC", arrayOf(today))
        cur.use {
            while (it.moveToNext()) {
                val cid = it.getLong(1)
                list.add(Sale(
                    it.getLong(0),
                    if (cid > 0) cid else null,
                    it.getString(2) ?: "", it.getDouble(3),
                    PaymentMode.valueOf(it.getString(4) ?: "ESPECE"),
                    it.getString(5) ?: ""
                ))
            }
        }
        return list
    }

    /** Chiffre d'affaires sur une période [from, to] (dates yyyy-MM-dd incluses). */
    fun getRevenueBetween(from: String, to: String): Double {
        var total = 0.0
        val cur = readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE sale_date BETWEEN ? AND ?", arrayOf(from, to)
        )
        cur.use { if (it.moveToFirst()) total = it.getDouble(0) }
        return total
    }

    /** Chiffre d'affaires total. */
    fun getTotalRevenue(): Double {
        var total = 0.0
        val cur = readableDatabase.rawQuery("SELECT COALESCE(SUM(total_amount),0) FROM sales", null)
        cur.use { if (it.moveToFirst()) total = it.getDouble(0) }
        return total
    }

    // ============ MOUVEMENTS DE STOCK ============
    fun insertStockMovement(m: StockMovement): Long {
        val v = ContentValues().apply {
            put("product_id", m.productId)
            put("type", m.type.name)
            put("quantity", m.quantity)
            put("date", m.date)
            put("reason", m.reason)
        }
        return writableDatabase.insert("stock_movements", null, v)
    }

    fun getAllStockMovements(): List<StockMovement> {
        val list = mutableListOf<StockMovement>()
        val cur = readableDatabase.rawQuery("SELECT id,product_id,type,quantity,date,reason FROM stock_movements ORDER BY id DESC", null)
        cur.use {
            while (it.moveToNext()) {
                list.add(StockMovement(
                    it.getLong(0), it.getLong(1),
                    MovementType.valueOf(it.getString(2) ?: "ENTREE"),
                    it.getInt(3), it.getString(4) ?: "", it.getString(5) ?: ""
                ))
            }
        }
        return list
    }

    // ============ FOURNISSEURS ============
    fun insertSupplier(s: Supplier): Long {
        val v = ContentValues().apply {
            put("name", s.name)
            put("contact", s.contact)
            put("products_supplied", s.productsSupplied)
        }
        return writableDatabase.insert("suppliers", null, v)
    }

    fun updateSupplier(s: Supplier) {
        val v = ContentValues().apply {
            put("name", s.name)
            put("contact", s.contact)
            put("products_supplied", s.productsSupplied)
        }
        writableDatabase.update("suppliers", v, "id=?", arrayOf(s.id.toString()))
    }

    fun deleteSupplier(id: Long) {
        writableDatabase.delete("suppliers", "id=?", arrayOf(id.toString()))
    }

    fun getAllSuppliers(): List<Supplier> {
        val list = mutableListOf<Supplier>()
        val cur = readableDatabase.rawQuery("SELECT id,name,contact,products_supplied FROM suppliers ORDER BY name", null)
        cur.use {
            while (it.moveToNext()) {
                list.add(Supplier(it.getLong(0), it.getString(1), it.getString(2) ?: "", it.getString(3) ?: ""))
            }
        }
        return list
    }
}
