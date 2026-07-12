package com.shop.agent.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "stock") val stock: Int,
    @ColumnInfo(name = "barcode") val barcode: String = ""
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "items") val items: String, // JSON compact
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun all(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE :q || '%' OR barcode LIKE :q || '%' ORDER BY name ASC")
    fun search(q: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Insert
    suspend fun insert(p: Product): Long

    @Update
    suspend fun update(p: Product)

    @Delete
    suspend fun delete(p: Product)

    @Query("UPDATE products SET stock = stock - :qty WHERE id = :id")
    suspend fun decrementStock(id: Long, qty: Int)

    @Query("UPDATE products SET stock = stock + :qty WHERE id = :id")
    suspend fun incrementStock(id: Long, qty: Int)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun all(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE total LIKE :q || '%' OR items LIKE :q || '%' ORDER BY timestamp DESC")
    fun search(q: String): Flow<List<Sale>>

    @Insert
    suspend fun insert(s: Sale): Long

    @Query("DELETE FROM sales")
    suspend fun clear()
}

@Database(entities = [Product::class, Sale::class], version = 1, exportSchema = false)
abstract class ShopDb : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
}
