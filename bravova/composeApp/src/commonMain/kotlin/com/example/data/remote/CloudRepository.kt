package com.example.data.remote

import com.example.model.BravisimaCard
import com.example.model.BravisimaMovement
import com.example.model.CartItem
import com.example.model.OrderRecord
import com.example.model.ShoppingList
import com.example.model.UserProfile
import com.example.platform.PlatformContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

/** Platform-specific Firestore error logger (returns a JSON description). */
expect fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String

/** Cloud sync backend: Firestore on Android, local-only on iOS until Firebase iOS is wired. */
expect fun createCloudRepository(context: PlatformContext): CloudRepository

object CloudRepositoryIds {
    const val PRIMARY_CARD_ID = "primary"
    const val MAIN_PROFILE_ID = "main"

    fun generateBravisimaCardNumber(seed: String): String {
        val hash = kotlin.math.abs(seed.hashCode())
        val seg1 = 1000 + (hash % 9000)
        val seg2 = 1000 + ((hash / 7 + 4219) % 9000)
        val seg3 = 1000 + ((hash / 13 + 7234) % 9000)
        return "BRV-$seg1-$seg2-$seg3"
    }
}

interface CloudRepository {
    fun observeUserProfile(userId: String, profileId: String = CloudRepositoryIds.MAIN_PROFILE_ID): Flow<UserProfile?>
    suspend fun saveOrUpdateUserProfile(
        userId: String,
        profile: UserProfile,
        activeBranch: String? = null,
        profileId: String = CloudRepositoryIds.MAIN_PROFILE_ID
    ): Result<UserProfile>

    fun observeCartItems(userId: String): Flow<List<CartItem>>
    suspend fun getUserCartItems(userId: String): Result<List<CartItem>>
    suspend fun setCartItemQuantity(userId: String, sku: String, qty: Int, storeMode: String? = null): Result<Unit>
    suspend fun clearUserCart(userId: String): Result<Unit>

    fun observeOrders(userId: String): Flow<List<OrderRecord>>
    fun observeOrderById(userId: String, orderId: String): Flow<OrderRecord?>
    suspend fun createOrder(userId: String, order: OrderRecord): Result<String>
    suspend fun updateOrderDeliveryStatus(
        userId: String,
        orderId: String,
        newStatus: String,
        updatedDateLabel: String? = null
    ): Result<Unit>

    fun observeShoppingLists(userId: String): Flow<List<ShoppingList>>
    suspend fun saveShoppingList(userId: String, list: ShoppingList): Result<String>
    suspend fun deleteShoppingList(userId: String, listId: String): Result<Unit>

    fun observeBravisimaCard(userId: String, cardId: String = CloudRepositoryIds.PRIMARY_CARD_ID): Flow<BravisimaCard?>
    suspend fun saveOrUpdateBravisimaCard(userId: String, card: BravisimaCard): Result<BravisimaCard>
    fun observeBravisimaMovements(userId: String): Flow<List<BravisimaMovement>>
    suspend fun recordBravisimaMovement(userId: String, movement: BravisimaMovement): Result<String>

    suspend fun seedInitialDataIfEmpty(
        userId: String,
        defaultOrders: List<OrderRecord>,
        defaultLists: List<ShoppingList>,
        defaultCard: BravisimaCard? = null,
        defaultMovements: List<BravisimaMovement> = emptyList(),
        defaultProfile: UserProfile? = null,
        activeBranch: String? = null
    )
}

/**
 * Cloud backend that keeps nothing remotely. The ViewModel already persists
 * everything locally (Room + preferences), so the app stays fully usable.
 */
class LocalOnlyCloudRepository : CloudRepository {
    override fun observeUserProfile(userId: String, profileId: String): Flow<UserProfile?> = emptyFlow()
    override suspend fun saveOrUpdateUserProfile(userId: String, profile: UserProfile, activeBranch: String?, profileId: String) =
        Result.success(profile)

    override fun observeCartItems(userId: String): Flow<List<CartItem>> = emptyFlow()
    override suspend fun getUserCartItems(userId: String): Result<List<CartItem>> = Result.success(emptyList())
    override suspend fun setCartItemQuantity(userId: String, sku: String, qty: Int, storeMode: String?) = Result.success(Unit)
    override suspend fun clearUserCart(userId: String) = Result.success(Unit)

    override fun observeOrders(userId: String): Flow<List<OrderRecord>> = emptyFlow()
    override fun observeOrderById(userId: String, orderId: String): Flow<OrderRecord?> = emptyFlow()
    override suspend fun createOrder(userId: String, order: OrderRecord) = Result.success(order.id)
    override suspend fun updateOrderDeliveryStatus(userId: String, orderId: String, newStatus: String, updatedDateLabel: String?) =
        Result.success(Unit)

    override fun observeShoppingLists(userId: String): Flow<List<ShoppingList>> = emptyFlow()
    override suspend fun saveShoppingList(userId: String, list: ShoppingList) = Result.success(list.id)
    override suspend fun deleteShoppingList(userId: String, listId: String) = Result.success(Unit)

    override fun observeBravisimaCard(userId: String, cardId: String): Flow<BravisimaCard?> = emptyFlow()
    override suspend fun saveOrUpdateBravisimaCard(userId: String, card: BravisimaCard) = Result.success(card)
    override fun observeBravisimaMovements(userId: String): Flow<List<BravisimaMovement>> = emptyFlow()
    override suspend fun recordBravisimaMovement(userId: String, movement: BravisimaMovement) = Result.success(movement.id)

    override suspend fun seedInitialDataIfEmpty(
        userId: String,
        defaultOrders: List<OrderRecord>,
        defaultLists: List<ShoppingList>,
        defaultCard: BravisimaCard?,
        defaultMovements: List<BravisimaMovement>,
        defaultProfile: UserProfile?,
        activeBranch: String?
    ) = Unit
}
