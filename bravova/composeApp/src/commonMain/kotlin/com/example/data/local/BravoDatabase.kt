package com.example.data.local

import androidx.room.ConstructedBy
import com.example.platform.getDatabaseBuilder
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.model.CatalogSection
import com.example.model.OrderRecord
import com.example.model.Product
import com.example.model.ShoppingList
import com.example.model.StoreMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.platform.Date
import com.example.platform.JSONArray
import com.example.platform.JSONObject
import com.example.platform.Locale
import com.example.platform.PlatformContext
import com.example.platform.SimpleDateFormat
import com.example.platform.System
import com.example.platform.sharedPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val sku: String,
    val qty: Int
)

@Entity(tableName = "app_prefs")
data class AppPrefEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "cached_products")
data class CachedProductEntity(
    @PrimaryKey val sku: String,
    val name: String,
    val price: Int,
    val category: String,
    val unit: String,
    val isBravo: Boolean,
    val artType: String,
    val colorHex: String,
    val badge: String,
    val imageUrl: String?,
    val sortOrder: Int,
    val cachedAtEpochMs: Long
) {
    fun toDomainProduct(): Product = Product(
        sku = sku,
        name = name,
        price = price,
        category = category,
        unit = unit,
        isBravo = isBravo,
        artType = artType,
        colorHex = colorHex,
        badge = badge,
        imageUrl = imageUrl
    )
}

@Entity(tableName = "cached_catalog_sections")
data class CachedCatalogSectionEntity(
    @PrimaryKey val sectionKey: String,
    val storeMode: String,
    val id: String,
    val name: String,
    val iconType: String,
    val sortOrder: Int,
    val cachedAtEpochMs: Long
) {
    fun toDomainSection(): CatalogSection = CatalogSection(
        id = id,
        name = name,
        iconType = iconType
    )
}

@Dao
interface BravoDao {
    @Query("SELECT * FROM cart_items")
    fun observeCartItems(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE sku = :sku")
    suspend fun deleteCartItem(sku: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("SELECT value FROM app_prefs WHERE `key` = :key LIMIT 1")
    suspend fun getPref(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPref(pref: AppPrefEntity)

    @Query("SELECT * FROM cached_products WHERE isBravo = :isBravo ORDER BY sortOrder ASC")
    fun observeCachedProductsForStore(isBravo: Boolean): Flow<List<CachedProductEntity>>

    @Query("SELECT * FROM cached_products ORDER BY isBravo DESC, sortOrder ASC")
    fun observeAllCachedProducts(): Flow<List<CachedProductEntity>>

    @Query("SELECT * FROM cached_catalog_sections WHERE storeMode = :storeMode ORDER BY sortOrder ASC")
    fun observeCachedSectionsForStore(storeMode: String): Flow<List<CachedCatalogSectionEntity>>

    @Query("SELECT COUNT(*) FROM cached_products")
    suspend fun getCachedProductCount(): Int

    @Query("SELECT COUNT(*) FROM cached_catalog_sections")
    suspend fun getCachedSectionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedProducts(products: List<CachedProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedSections(sections: List<CachedCatalogSectionEntity>)

    @Query("DELETE FROM cached_products")
    suspend fun clearCachedProducts()

    @Query("DELETE FROM cached_catalog_sections")
    suspend fun clearCachedSections()
}

@Database(
    entities = [
        CartItemEntity::class,
        AppPrefEntity::class,
        CachedProductEntity::class,
        CachedCatalogSectionEntity::class
    ],
    version = 2,
    exportSchema = false
)
@ConstructedBy(BravoDatabaseConstructor::class)
abstract class BravoDatabase : RoomDatabase() {
    abstract fun bravoDao(): BravoDao

    companion object {
        private var INSTANCE: BravoDatabase? = null

        fun getInstance(context: PlatformContext): BravoDatabase {
            return INSTANCE ?: getDatabaseBuilder(context)
                .fallbackToDestructiveMigration(true)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
                .also { INSTANCE = it }
        }
    }
}

// Room generates the actual implementations for each platform.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object BravoDatabaseConstructor : RoomDatabaseConstructor<BravoDatabase> {
    override fun initialize(): BravoDatabase
}


class BravoLocalRepository(
    private val dao: BravoDao,
    @Suppress("UNUSED_PARAMETER") context: PlatformContext
) {
    private val sharedPrefs = sharedPrefs("bravova_instant_prefs")

    val cartItems: Flow<List<CartItemEntity>> = dao.observeCartItems()

    fun observeCachedProducts(storeMode: StoreMode): Flow<List<Product>> {
        val isBravo = storeMode == StoreMode.BRAVO
        return dao.observeCachedProductsForStore(isBravo).map { list ->
            list.map { it.toDomainProduct() }
        }
    }

    fun observeAllCachedProducts(): Flow<List<Product>> {
        return dao.observeAllCachedProducts().map { list ->
            list.map { it.toDomainProduct() }
        }
    }

    fun observeCachedSections(storeMode: StoreMode): Flow<List<CatalogSection>> {
        return dao.observeCachedSectionsForStore(storeMode.name).map { list ->
            list.map { it.toDomainSection() }
        }
    }

    companion object {
        private const val CURRENT_CATALOG_IMAGES_VERSION = 11
    }

    suspend fun ensureCatalogCached(
        bravoProducts: List<Product>,
        arcaProducts: List<Product>,
        bravoSections: List<CatalogSection>,
        arcaSections: List<CatalogSection>,
        forceRefresh: Boolean = false
    ): Int {
        val existingProducts = dao.getCachedProductCount()
        val existingSections = dao.getCachedSectionCount()
        val expectedTotal = bravoProducts.size + arcaProducts.size
        val expectedSectionsTotal = bravoSections.size + arcaSections.size
        val cachedImagesVersion = sharedPrefs.getInt("catalog_images_version", 0)
        if (
            !forceRefresh &&
            cachedImagesVersion >= CURRENT_CATALOG_IMAGES_VERSION &&
            existingProducts == expectedTotal &&
            existingSections == expectedSectionsTotal
        ) {
            return existingProducts
        }

        val now = System.currentTimeMillis()
        val productEntities = buildList {
            bravoProducts.forEachIndexed { index, product ->
                add(
                    CachedProductEntity(
                        sku = product.sku,
                        name = product.name,
                        price = product.price,
                        category = product.category,
                        unit = product.unit,
                        isBravo = true,
                        artType = product.artType,
                        colorHex = product.colorHex,
                        badge = product.badge,
                        imageUrl = product.imageUrl,
                        sortOrder = index,
                        cachedAtEpochMs = now
                    )
                )
            }
            arcaProducts.forEachIndexed { index, product ->
                add(
                    CachedProductEntity(
                        sku = product.sku,
                        name = product.name,
                        price = product.price,
                        category = product.category,
                        unit = product.unit,
                        isBravo = false,
                        artType = product.artType,
                        colorHex = product.colorHex,
                        badge = product.badge,
                        imageUrl = product.imageUrl,
                        sortOrder = index,
                        cachedAtEpochMs = now
                    )
                )
            }
        }

        val sectionEntities = buildList {
            bravoSections.forEachIndexed { index, section ->
                add(
                    CachedCatalogSectionEntity(
                        sectionKey = "${StoreMode.BRAVO.name}_${section.id}",
                        storeMode = StoreMode.BRAVO.name,
                        id = section.id,
                        name = section.name,
                        iconType = section.iconType,
                        sortOrder = index,
                        cachedAtEpochMs = now
                    )
                )
            }
            arcaSections.forEachIndexed { index, section ->
                add(
                    CachedCatalogSectionEntity(
                        sectionKey = "${StoreMode.ARCA.name}_${section.id}",
                        storeMode = StoreMode.ARCA.name,
                        id = section.id,
                        name = section.name,
                        iconType = section.iconType,
                        sortOrder = index,
                        cachedAtEpochMs = now
                    )
                )
            }
        }

        dao.clearCachedProducts()
        dao.clearCachedSections()
        dao.upsertCachedProducts(productEntities)
        dao.upsertCachedSections(sectionEntities)

        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
        sharedPrefs.edit()
            .putLong("last_catalog_cache_sync_ms", now)
            .putString("last_catalog_cache_label", formattedTime)
            .putInt("cached_product_total", productEntities.size)
            .putInt("catalog_images_version", CURRENT_CATALOG_IMAGES_VERSION)
            .apply()
        dao.setPref(AppPrefEntity("last_catalog_cache_sync_ms", now.toString()))
        return productEntities.size
    }

    fun getLastCacheSyncLabelSync(): String {
        val time = sharedPrefs.getString("last_catalog_cache_label", null)
        return if (time != null) {
            "Precios e imágenes en caché • Act. $time"
        } else {
            "Precios e imágenes en caché local"
        }
    }

    fun getNpuModeSync(defaultValue: Boolean = true): Boolean {
        return sharedPrefs.getBoolean("npu_mode_enabled", defaultValue)
    }

    suspend fun saveNpuMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("npu_mode_enabled", enabled).apply()
        dao.setPref(AppPrefEntity("npu_mode_enabled", enabled.toString()))
    }

    fun hasExplicitDarkModePrefSync(): Boolean {
        return sharedPrefs.contains("dark_mode_user_explicit")
    }

    fun getGuestModeSync(defaultValue: Boolean = false): Boolean {
        return sharedPrefs.getBoolean("guest_mode_enabled", defaultValue)
    }

    suspend fun saveGuestMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("guest_mode_enabled", enabled).apply()
        dao.setPref(AppPrefEntity("guest_mode_enabled", enabled.toString()))
    }

    suspend fun setCartQty(sku: String, qty: Int) {
        if (qty <= 0) {
            dao.deleteCartItem(sku)
        } else {
            dao.upsertCartItem(CartItemEntity(sku, qty))
        }
    }

    suspend fun clearCart() {
        dao.clearCart()
    }

    suspend fun replaceCart(items: Map<String, Int>) {
        dao.clearCart()
        items.forEach { (sku, qty) ->
            if (qty > 0) {
                dao.upsertCartItem(CartItemEntity(sku, qty))
            }
        }
    }

    suspend fun getBravisimaBalance(defaultBalance: Int = 145): Int {
        return dao.getPref("bravisima_balance")?.toIntOrNull() ?: defaultBalance
    }

    suspend fun saveBravisimaBalance(balance: Int) {
        dao.setPref(AppPrefEntity("bravisima_balance", balance.toString()))
    }

    /** Elección manual del usuario; null significa "seguir el tema del dispositivo". */
    fun getDarkModeOverrideSync(): Boolean? =
        if (sharedPrefs.contains(DARK_MODE_CHOICE_KEY)) sharedPrefs.getBoolean(DARK_MODE_CHOICE_KEY, false) else null

    suspend fun loadDarkModeOverride(): Boolean? =
        dao.getPref(DARK_MODE_CHOICE_KEY)?.toBooleanStrictOrNull() ?: getDarkModeOverrideSync()

    suspend fun saveDarkModeOverride(enabled: Boolean?) {
        if (enabled == null) {
            sharedPrefs.edit().remove(DARK_MODE_CHOICE_KEY).apply()
            dao.setPref(AppPrefEntity(DARK_MODE_CHOICE_KEY, "system"))
        } else {
            sharedPrefs.edit().putBoolean(DARK_MODE_CHOICE_KEY, enabled).apply()
            dao.setPref(AppPrefEntity(DARK_MODE_CHOICE_KEY, enabled.toString()))
        }
    }

    fun getPaletteIdSync(defaultPalette: String = "material_you"): String {
        return sharedPrefs.getString("color_palette_id", defaultPalette) ?: defaultPalette
    }

    suspend fun loadPaletteId(defaultPalette: String = "material_you"): String {
        val fromRoom = dao.getPref("color_palette_id")?.takeIf { it.isNotBlank() }
        val resolved = fromRoom ?: (sharedPrefs.getString("color_palette_id", defaultPalette) ?: defaultPalette)
        sharedPrefs.edit().putString("color_palette_id", resolved).apply()
        return resolved
    }

    suspend fun savePaletteId(paletteId: String) {
        sharedPrefs.edit().putString("color_palette_id", paletteId).apply()
        dao.setPref(AppPrefEntity("color_palette_id", paletteId))
    }

    fun getActiveBranchSync(defaultBranch: String = "Bravo Winston Churchill"): String {
        return sharedPrefs.getString("active_branch", defaultBranch) ?: defaultBranch
    }

    suspend fun loadActiveBranch(defaultBranch: String = "Bravo Winston Churchill"): String {
        val fromRoom = dao.getPref("active_branch")?.takeIf { it.isNotBlank() }
        val resolved = fromRoom ?: (sharedPrefs.getString("active_branch", defaultBranch) ?: defaultBranch)
        sharedPrefs.edit().putString("active_branch", resolved).apply()
        return resolved
    }

    suspend fun saveActiveBranch(branch: String) {
        sharedPrefs.edit().putString("active_branch", branch).apply()
        dao.setPref(AppPrefEntity("active_branch", branch))
    }

    fun getPushNotificationsSync(defaultEnabled: Boolean = true): Boolean {
        return sharedPrefs.getBoolean("push_notifications_enabled", defaultEnabled)
    }

    suspend fun savePushNotifications(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("push_notifications_enabled", enabled).apply()
        dao.setPref(AppPrefEntity("push_notifications_enabled", enabled.toString()))
    }

    suspend fun saveFavorites(skus: List<String>) {
        val joined = skus.joinToString(",")
        sharedPrefs.edit().putString("favorite_skus", joined).apply()
        dao.setPref(AppPrefEntity("favorite_skus", joined))
    }

    suspend fun loadFavorites(defaultSkus: List<String>): List<String> {
        val raw = dao.getPref("favorite_skus") ?: sharedPrefs.getString("favorite_skus", null)
        if (raw == null) return defaultSkus
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    suspend fun saveShoppingLists(lists: List<ShoppingList>) {
        val jsonArray = JSONArray()
        lists.forEach { list ->
            val obj = JSONObject().apply {
                put("id", list.id)
                put("name", list.name)
                put("skus", JSONArray(list.skus))
                put("aiNote", list.aiNote ?: "")
                put("isAiGenerated", list.isAiGenerated)
                put("updatedAtLabel", list.updatedAtLabel)
            }
            jsonArray.put(obj)
        }
        val serialized = jsonArray.toString()
        sharedPrefs.edit().putString("shopping_lists_v2", serialized).apply()
        dao.setPref(AppPrefEntity("shopping_lists_v2", serialized))
    }

    suspend fun loadShoppingLists(defaultLists: List<ShoppingList>): List<ShoppingList> {
        val raw = dao.getPref("shopping_lists_v2") ?: sharedPrefs.getString("shopping_lists_v2", null)
        if (raw.isNullOrBlank()) return defaultLists
        return try {
            val arr = JSONArray(raw)
            val parsed = mutableListOf<ShoppingList>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val skusJson = obj.optJSONArray("skus") ?: JSONArray()
                val skuList = mutableListOf<String>()
                for (j in 0 until skusJson.length()) {
                    val sku = skusJson.optString(j).trim()
                    if (sku.isNotEmpty()) skuList.add(sku)
                }
                val note = obj.optString("aiNote", "").takeIf { it.isNotBlank() }
                parsed.add(
                    ShoppingList(
                        id = obj.optString("id", "l_${i + 1}"),
                        name = obj.optString("name", "Lista ${i + 1}"),
                        skus = skuList,
                        aiNote = note,
                        isAiGenerated = obj.optBoolean("isAiGenerated", false),
                        updatedAtLabel = obj.optString("updatedAtLabel", "Guardada")
                    )
                )
            }
            if (parsed.isEmpty()) defaultLists else parsed
        } catch (_: Exception) {
            defaultLists
        }
    }

    suspend fun saveOrders(orders: List<OrderRecord>) {
        val jsonArray = JSONArray()
        orders.forEach { order ->
            val obj = JSONObject().apply {
                put("id", order.id)
                put("status", order.status)
                put("badgeNum", order.badgeNum)
                put("date", order.date)
                put("total", order.total)
                put("branch", order.branch)
                put("skus", JSONArray(order.skus))
            }
            jsonArray.put(obj)
        }
        val serialized = jsonArray.toString()
        sharedPrefs.edit().putString("local_orders_v1", serialized).apply()
        dao.setPref(AppPrefEntity("local_orders_v1", serialized))
    }

    suspend fun loadOrders(defaultOrders: List<OrderRecord>): List<OrderRecord> {
        val raw = dao.getPref("local_orders_v1") ?: sharedPrefs.getString("local_orders_v1", null)
        if (raw.isNullOrBlank()) return defaultOrders
        return try {
            val arr = JSONArray(raw)
            val parsed = mutableListOf<OrderRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val skusJson = obj.optJSONArray("skus") ?: JSONArray()
                val skuList = mutableListOf<String>()
                for (j in 0 until skusJson.length()) {
                    val s = skusJson.optString(j).trim()
                    if (s.isNotEmpty()) skuList.add(s)
                }
                parsed.add(
                    OrderRecord(
                        id = obj.optString("id", "ORD-${10000 + i}"),
                        status = obj.optString("status", "Entregado"),
                        badgeNum = obj.optInt("badgeNum", skuList.size),
                        date = obj.optString("date", "Reciente"),
                        total = obj.optDouble("total", 0.0),
                        branch = obj.optString("branch", "Bravo Winston Churchill"),
                        skus = skuList
                    )
                )
            }
            if (parsed.isEmpty()) defaultOrders else parsed
        } catch (_: Exception) {
            defaultOrders
        }
    }
}

private const val DARK_MODE_CHOICE_KEY = "dark_mode_choice"
