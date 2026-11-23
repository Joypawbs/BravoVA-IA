package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.BravisimaCard
import com.example.model.BravisimaMovement
import com.example.model.CartItem
import com.example.model.OrderRecord
import com.example.model.ShoppingList
import com.example.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class FirestoreUserProfileDocument(
    @DocumentId val id: String = "main",
    val userId: String = "",
    val name: String = "Cliente Bravísimo",
    val email: String = "cliente@bravova.com.do",
    val phone: String = "809-555-0192",
    val cedula: String = "001-1928374-5",
    val bravisimaNumber: String = "BRV-8842-9901-7234",
    val rncNit: String = "131-88492-1",
    val ncfType: String = "B01 - Crédito Fiscal",
    val activeBranch: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainModel(): UserProfile = UserProfile(
        name = name.ifBlank { "Cliente Bravísimo" },
        email = email.ifBlank { "cliente@bravova.com.do" },
        phone = phone.ifBlank { "809-555-0192" },
        cedula = cedula.ifBlank { "001-1928374-5" },
        bravisimaNumber = bravisimaNumber.ifBlank { "BRV-8842-9901-7234" },
        rncNit = rncNit.ifBlank { "131-88492-1" },
        ncfType = ncfType.ifBlank { "B01 - Crédito Fiscal" }
    )
}

@IgnoreExtraProperties
data class FirestoreCartItemDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sku: String = "",
    val qty: Int = 1,
    val storeMode: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainModel(): CartItem = CartItem(
        sku = sku.ifBlank { id },
        qty = qty.coerceIn(1, 999)
    )
}

@IgnoreExtraProperties
data class FirestoreOrderDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val status: String = "",
    val badgeNum: Int = 0,
    val date: String = "",
    val total: Double = 0.0,
    val branch: String = "",
    val skus: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainModel(): OrderRecord = OrderRecord(
        id = id,
        status = status,
        badgeNum = badgeNum,
        date = date,
        total = total,
        branch = branch,
        skus = skus
    )
}

@IgnoreExtraProperties
data class FirestoreShoppingListDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val skus: List<String> = emptyList(),
    val aiNote: String? = null,
    val isAiGenerated: Boolean = false,
    val updatedAtLabel: String = "Sincronizada",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainModel(): ShoppingList = ShoppingList(
        id = id,
        name = name,
        skus = skus,
        aiNote = aiNote,
        isAiGenerated = isAiGenerated,
        updatedAtLabel = updatedAtLabel
    )
}

@IgnoreExtraProperties
data class FirestoreBravisimaCardDocument(
    @DocumentId val id: String = "primary",
    val userId: String = "",
    val cardNumber: String = "BRV-8842-9901-7234",
    val holderName: String = "Cliente Bravísimo",
    val holderEmail: String? = null,
    val balance: Int = 145,
    val lifetimeEarned: Int = 320,
    val status: String = "active",
    val autoRedeemInCart: Boolean = false,
    val monthlyGoal: Int = 500,
    val preferredCategory: String = "Todo Bravo",
    val claimedPromoCodes: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainModel(): BravisimaCard = BravisimaCard(
        cardId = id.ifBlank { "primary" },
        cardNumber = cardNumber.ifBlank { "BRV-8842-9901-7234" },
        holderName = holderName.ifBlank { "Cliente Bravísimo" },
        holderEmail = holderEmail,
        balance = balance.coerceAtLeast(0),
        lifetimeEarned = lifetimeEarned.coerceAtLeast(0),
        isLocked = status.equals("locked", ignoreCase = true),
        autoRedeemInCart = autoRedeemInCart,
        monthlyGoal = monthlyGoal.coerceIn(100, 50000),
        preferredCategory = preferredCategory.ifBlank { "Todo Bravo" },
        claimedPromoCodes = claimedPromoCodes
    )
}

@IgnoreExtraProperties
data class FirestoreBravisimaMovementDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val dateLabel: String = "Hoy",
    val amount: Int = 0,
    val movementType: String = "earn",
    val orderId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomainModel(): BravisimaMovement = BravisimaMovement(
        id = id,
        title = title,
        subtitle = subtitle,
        dateLabel = dateLabel,
        amount = amount,
        movementType = movementType,
        orderId = orderId
    )
}

class BravoFirestoreRepository(
    private val db: FirebaseFirestore,
    private val currentUidProvider: () -> String? = {
        FirebaseAuth.getInstance().currentUser?.uid
    }
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        { FirebaseAuth.getInstance().currentUser?.uid }
    )

    /**
     * Validates that there is an active authenticated user in FirebaseAuth
     * (`FirebaseAuth.getInstance().currentUser?.uid`) and that it strictly matches
     * the target `requestedUserId` before executing any Firestore CRUD or stream operation.
     * Prevents cross-account data access or leakage of Tarjeta Bravísima credentials.
     */
    internal fun requireAuthenticatedUserId(
        requestedUserId: String,
        operation: OperationType,
        targetPath: String
    ): String {
        val cleanRequestedUid = requestedUserId.trim()
        if (cleanRequestedUid.isEmpty()) {
            val ex = FirebaseFirestoreException(
                "Operación bloqueada: el ID de usuario solicitado está vacío para $targetPath.",
                FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
            handleFirestoreError(ex, operation, targetPath)
            throw ex
        }

        val currentAuthUid = runCatching { currentUidProvider()?.trim() }.getOrNull()
        if (currentAuthUid.isNullOrEmpty()) {
            val ex = FirebaseFirestoreException(
                "Operación denegada: no existe sesión autenticada activa (FirebaseAuth.getInstance().currentUser?.uid es null) para ${operation.value} en $targetPath.",
                FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
            handleFirestoreError(ex, operation, targetPath)
            throw ex
        }

        if (currentAuthUid != cleanRequestedUid) {
            val ex = FirebaseFirestoreException(
                "Operación bloqueada por seguridad: intento de acceso cruzado entre cuentas (UID autenticado '$currentAuthUid' != UID solicitado '$cleanRequestedUid') en $targetPath.",
                FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
            handleFirestoreError(ex, operation, targetPath)
            throw ex
        }

        return currentAuthUid
    }

    /**
     * Verifies that the active session still belongs to [expectedUid] when a snapshot or
     * document payload is received, and that the document's internal `userId` field matches
     * [expectedUid], preventing any cross-account leakage of Tarjeta Bravísima or profile data.
     */
    private fun verifyDocumentOwnership(
        documentUserId: String?,
        expectedUid: String,
        operation: OperationType,
        targetPath: String
    ) {
        val liveAuthUid = runCatching { currentUidProvider()?.trim() }.getOrNull()
        if (liveAuthUid.isNullOrEmpty() || liveAuthUid != expectedUid) {
            val ex = FirebaseFirestoreException(
                "Sesión finalizada o cambiada durante ${operation.value} en $targetPath. Se abortó la entrega de datos para evitar filtraciones entre cuentas.",
                FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
            handleFirestoreError(ex, operation, targetPath)
            throw ex
        }

        val cleanDocUid = documentUserId?.trim().orEmpty()
        if (cleanDocUid.isNotEmpty() && cleanDocUid != expectedUid) {
            val ex = FirebaseFirestoreException(
                "Inconsistencia de propietario detectada en $targetPath: el documento pertenece a '$cleanDocUid' pero la sesión activa es '$expectedUid'.",
                FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
            handleFirestoreError(ex, operation, targetPath)
            throw ex
        }
    }

    private fun profileCollection(userId: String) =
        db.collection("users").document(userId).collection("profile")

    private fun cartCollection(userId: String) =
        db.collection("users").document(userId).collection("cart")

    private fun ordersCollection(userId: String) =
        db.collection("users").document(userId).collection("orders")

    private fun shoppingListsCollection(userId: String) =
        db.collection("users").document(userId).collection("shoppingLists")

    private fun bravisimaCardsCollection(userId: String) =
        db.collection("users").document(userId).collection("bravisimaCards")

    private fun bravisimaMovementsCollection(userId: String) =
        db.collection("users").document(userId).collection("bravisimaMovements")

    fun observeUserProfile(userId: String, profileId: String = MAIN_PROFILE_ID): Flow<UserProfile?> = flow {
        val cleanProfileId = profileId.ifBlank { MAIN_PROFILE_ID }
        val targetPath = "users/${userId.trim()}/profile/$cleanProfileId"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.GET, targetPath)
        val docRef = profileCollection(verifiedUid).document(cleanProfileId)

        emitAll(
            docRef.snapshots().map { snapshot ->
                verifyDocumentOwnership(
                    documentUserId = snapshot.getString("userId"),
                    expectedUid = verifiedUid,
                    operation = OperationType.GET,
                    targetPath = docRef.path
                )
                snapshot.toObject(
                    FirestoreUserProfileDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.takeIf { it.userId.isBlank() || it.userId == verifiedUid }?.toDomainModel()
            }
        )
    }

    suspend fun getUserProfile(userId: String, profileId: String = MAIN_PROFILE_ID): Result<UserProfile?> = runCatching {
        val cleanProfileId = profileId.ifBlank { MAIN_PROFILE_ID }
        val targetPath = "users/${userId.trim()}/profile/$cleanProfileId"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.GET, targetPath)
        val docRef = profileCollection(verifiedUid).document(cleanProfileId)
        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return@runCatching null
            verifyDocumentOwnership(
                documentUserId = snapshot.getString("userId"),
                expectedUid = verifiedUid,
                operation = OperationType.GET,
                targetPath = docRef.path
            )
            snapshot.toObject(
                FirestoreUserProfileDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.toDomainModel()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveOrUpdateUserProfile(
        userId: String,
        profile: UserProfile,
        activeBranch: String? = null,
        profileId: String = MAIN_PROFILE_ID
    ): Result<UserProfile> = runCatching {
        val cleanId = profileId.ifBlank { MAIN_PROFILE_ID }
        val targetPath = "users/${userId.trim()}/profile/$cleanId"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.WRITE, targetPath)
        val docRef = profileCollection(verifiedUid).document(cleanId)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                verifyDocumentOwnership(
                    documentUserId = existingSnap.getString("userId"),
                    expectedUid = verifiedUid,
                    operation = OperationType.UPDATE,
                    targetPath = docRef.path
                )
            }
            val cleanName = profile.name.trim().ifBlank { "Cliente Bravísimo" }.take(100)
            val cleanEmail = profile.email.trim().ifBlank { "cliente@bravova.com.do" }.take(160)
            val cleanPhone = profile.phone.trim().ifBlank { "809-555-0192" }.take(40)
            val cleanCedula = profile.cedula.trim().ifBlank { "001-1928374-5" }.take(40)
            val expectedCardForUid = generateBravisimaCardNumber(verifiedUid)
            val rawBravisima = profile.bravisimaNumber.trim().uppercase()
            val cleanBravisima = if (
                rawBravisima.matches(Regex("^BRV-[0-9A-Z\\-]+$")) &&
                rawBravisima.length in 6..32
            ) {
                rawBravisima
            } else {
                expectedCardForUid
            }
            val cleanRnc = profile.rncNit.trim().ifBlank { "131-88492-1" }.take(40)
            val cleanNcf = profile.ncfType.trim().ifBlank { "B01 - Crédito Fiscal" }.take(60)
            val cleanBranch = activeBranch?.trim()?.takeIf { it.isNotBlank() }?.take(120)

            if (!existingSnap.exists()) {
                val createPayload = mapOf(
                    "userId" to verifiedUid,
                    "name" to cleanName,
                    "email" to cleanEmail,
                    "phone" to cleanPhone,
                    "cedula" to cleanCedula,
                    "bravisimaNumber" to cleanBravisima,
                    "rncNit" to cleanRnc,
                    "ncfType" to cleanNcf,
                    "activeBranch" to cleanBranch,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "name" to cleanName,
                    "email" to cleanEmail,
                    "phone" to cleanPhone,
                    "cedula" to cleanCedula,
                    "rncNit" to cleanRnc,
                    "ncfType" to cleanNcf,
                    "activeBranch" to cleanBranch,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updatePayload).await()
            }
            profile.copy(
                name = cleanName,
                email = cleanEmail,
                phone = cleanPhone,
                cedula = cleanCedula,
                bravisimaNumber = existingSnap.getString("bravisimaNumber") ?: cleanBravisima,
                rncNit = cleanRnc,
                ncfType = cleanNcf
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeCartItems(userId: String): Flow<List<CartItem>> = flow {
        val targetPath = "users/${userId.trim()}/cart"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.LIST, targetPath)
        val colRef = cartCollection(verifiedUid)

        emitAll(
            colRef.snapshots().map { snapshot ->
                verifyDocumentOwnership(
                    documentUserId = verifiedUid,
                    expectedUid = verifiedUid,
                    operation = OperationType.LIST,
                    targetPath = colRef.path
                )
                val docs = snapshot.toObjects(
                    FirestoreCartItemDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                docs.filter { it.userId.isBlank() || it.userId == verifiedUid }
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                    .map { it.toDomainModel() }
            }
        )
    }

    suspend fun getUserCartItems(userId: String): Result<List<CartItem>> = runCatching {
        val targetPath = "users/${userId.trim()}/cart"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.LIST, targetPath)
        val colRef = cartCollection(verifiedUid)
        try {
            val snapshot = colRef.get().await()
            verifyDocumentOwnership(
                documentUserId = verifiedUid,
                expectedUid = verifiedUid,
                operation = OperationType.LIST,
                targetPath = colRef.path
            )
            snapshot.toObjects(
                FirestoreCartItemDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).filter { it.userId.isBlank() || it.userId == verifiedUid }
                .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                .map { it.toDomainModel() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun setCartItemQuantity(
        userId: String,
        sku: String,
        qty: Int,
        storeMode: String? = null
    ): Result<Unit> = runCatching {
        val cleanSku = sku.trim().uppercase().take(40)
        require(cleanSku.isNotBlank()) { "SKU must not be blank" }
        val targetPath = "users/${userId.trim()}/cart/$cleanSku"
        val opType = if (qty <= 0) OperationType.DELETE else OperationType.WRITE
        val verifiedUid = requireAuthenticatedUserId(userId, opType, targetPath)
        val docRef = cartCollection(verifiedUid).document(cleanSku)
        try {
            if (qty <= 0) {
                docRef.delete().await()
            } else {
                val boundedQty = qty.coerceIn(1, 999)
                val cleanStore = storeMode?.trim()?.uppercase()?.takeIf { it in listOf("BRAVO", "ARCA") }
                    ?: if (cleanSku.startsWith("ARC-")) "ARCA" else "BRAVO"
                val existingSnap = docRef.get().await()
                if (!existingSnap.exists()) {
                    val createPayload = mapOf(
                        "userId" to verifiedUid,
                        "sku" to cleanSku,
                        "qty" to boundedQty,
                        "storeMode" to cleanStore,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(createPayload).await()
                } else {
                    verifyDocumentOwnership(
                        documentUserId = existingSnap.getString("userId"),
                        expectedUid = verifiedUid,
                        operation = OperationType.UPDATE,
                        targetPath = docRef.path
                    )
                    val updatePayload = mapOf(
                        "qty" to boundedQty,
                        "storeMode" to cleanStore,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updatePayload).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, opType, docRef.path)
            throw e
        }
    }

    suspend fun clearUserCart(userId: String): Result<Unit> = runCatching {
        val targetPath = "users/${userId.trim()}/cart"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.DELETE, targetPath)
        val colRef = cartCollection(verifiedUid)
        try {
            val snapshot = colRef.get().await()
            for (doc in snapshot.documents) {
                verifyDocumentOwnership(
                    documentUserId = doc.getString("userId"),
                    expectedUid = verifiedUid,
                    operation = OperationType.DELETE,
                    targetPath = doc.reference.path
                )
                doc.reference.delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            throw e
        }
    }

    fun observeOrders(userId: String): Flow<List<OrderRecord>> = flow {
        val targetPath = "users/${userId.trim()}/orders"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.LIST, targetPath)
        val colRef = ordersCollection(verifiedUid)

        emitAll(
            colRef.snapshots().map { snapshot ->
                verifyDocumentOwnership(
                    documentUserId = verifiedUid,
                    expectedUid = verifiedUid,
                    operation = OperationType.LIST,
                    targetPath = colRef.path
                )
                val docs = snapshot.toObjects(
                    FirestoreOrderDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                docs.filter { it.userId.isBlank() || it.userId == verifiedUid }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    .map { it.toDomainModel() }
            }
        )
    }

    fun observeShoppingLists(userId: String): Flow<List<ShoppingList>> = flow {
        val targetPath = "users/${userId.trim()}/shoppingLists"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.LIST, targetPath)
        val colRef = shoppingListsCollection(verifiedUid)

        emitAll(
            colRef.snapshots().map { snapshot ->
                verifyDocumentOwnership(
                    documentUserId = verifiedUid,
                    expectedUid = verifiedUid,
                    operation = OperationType.LIST,
                    targetPath = colRef.path
                )
                val docs = snapshot.toObjects(
                    FirestoreShoppingListDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                docs.filter { it.userId.isBlank() || it.userId == verifiedUid }
                    .sortedWith(
                        compareBy<FirestoreShoppingListDocument> { if (it.id == "l1") 0 else 1 }
                            .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    ).map { it.toDomainModel() }
            }
        )
    }

    suspend fun createOrder(userId: String, order: OrderRecord): Result<String> = runCatching {
        val cleanId = order.id.ifBlank { "ORD-${System.currentTimeMillis()}" }
        val targetPath = "users/${userId.trim()}/orders/$cleanId"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.CREATE, targetPath)
        val docRef = ordersCollection(verifiedUid).document(cleanId)
        val payload = mapOf(
            "userId" to verifiedUid,
            "status" to order.status.take(80),
            "badgeNum" to order.badgeNum.coerceIn(0, 500),
            "date" to order.date.take(80),
            "total" to order.total.coerceIn(0.0, 1000000.0),
            "branch" to order.branch.take(120),
            "skus" to order.skus.take(50),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            cleanId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getUserOrders(userId: String): Result<List<OrderRecord>> = runCatching {
        val targetPath = "users/${userId.trim()}/orders"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.LIST, targetPath)
        val colRef = ordersCollection(verifiedUid)
        try {
            val snapshot = colRef.get().await()
            verifyDocumentOwnership(
                documentUserId = verifiedUid,
                expectedUid = verifiedUid,
                operation = OperationType.LIST,
                targetPath = colRef.path
            )
            snapshot.toObjects(
                FirestoreOrderDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).filter { it.userId.isBlank() || it.userId == verifiedUid }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                .map { it.toDomainModel() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    fun observeOrderById(userId: String, orderId: String): Flow<OrderRecord?> = flow {
        val targetPath = "users/${userId.trim()}/orders/${orderId.trim()}"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.GET, targetPath)
        val docRef = ordersCollection(verifiedUid).document(orderId.trim())

        emitAll(
            docRef.snapshots().map { snapshot ->
                verifyDocumentOwnership(
                    documentUserId = snapshot.getString("userId"),
                    expectedUid = verifiedUid,
                    operation = OperationType.GET,
                    targetPath = docRef.path
                )
                snapshot.toObject(
                    FirestoreOrderDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.takeIf { it.userId.isBlank() || it.userId == verifiedUid }?.toDomainModel()
            }
        )
    }

    suspend fun getOrderById(userId: String, orderId: String): Result<OrderRecord?> = runCatching {
        val targetPath = "users/${userId.trim()}/orders/${orderId.trim()}"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.GET, targetPath)
        val docRef = ordersCollection(verifiedUid).document(orderId.trim())
        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return@runCatching null
            verifyDocumentOwnership(
                documentUserId = snapshot.getString("userId"),
                expectedUid = verifiedUid,
                operation = OperationType.GET,
                targetPath = docRef.path
            )
            snapshot.toObject(
                FirestoreOrderDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.toDomainModel()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateOrderDeliveryStatus(
        userId: String,
        orderId: String,
        newStatus: String,
        updatedDateLabel: String? = null
    ): Result<Unit> = runCatching {
        val targetPath = "users/${userId.trim()}/orders/${orderId.trim()}"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.UPDATE, targetPath)
        val docRef = ordersCollection(verifiedUid).document(orderId.trim())
        val payload = buildMap<String, Any> {
            put("status", newStatus.trim().ifBlank { "Envío preparado" }.take(80))
            if (!updatedDateLabel.isNullOrBlank()) {
                put("date", updatedDateLabel.trim().take(80))
            }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun saveShoppingList(userId: String, list: ShoppingList): Result<String> = runCatching {
        val cleanId = list.id.ifBlank { "list_${System.currentTimeMillis()}" }
        val targetPath = "users/${userId.trim()}/shoppingLists/$cleanId"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.WRITE, targetPath)
        val docRef = shoppingListsCollection(verifiedUid).document(cleanId)
        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val createPayload = mapOf(
                    "userId" to verifiedUid,
                    "name" to list.name.trim().ifBlank { "Mi Lista" }.take(120),
                    "skus" to list.skus.distinct().take(50),
                    "aiNote" to list.aiNote?.take(500),
                    "isAiGenerated" to list.isAiGenerated,
                    "updatedAtLabel" to list.updatedAtLabel.ifBlank { "Sincronizada" }.take(120),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createPayload).await()
            } else {
                verifyDocumentOwnership(
                    documentUserId = existingSnap.getString("userId"),
                    expectedUid = verifiedUid,
                    operation = OperationType.UPDATE,
                    targetPath = docRef.path
                )
                val updatePayload = mapOf(
                    "name" to list.name.trim().ifBlank { "Mi Lista" }.take(120),
                    "skus" to list.skus.distinct().take(50),
                    "aiNote" to list.aiNote?.take(500),
                    "isAiGenerated" to list.isAiGenerated,
                    "updatedAtLabel" to list.updatedAtLabel.ifBlank { "Sincronizada" }.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updatePayload).await()
            }
            cleanId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getUserShoppingLists(userId: String): Result<List<ShoppingList>> = runCatching {
        val targetPath = "users/${userId.trim()}/shoppingLists"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.LIST, targetPath)
        val colRef = shoppingListsCollection(verifiedUid)
        try {
            val snapshot = colRef.get().await()
            verifyDocumentOwnership(
                documentUserId = verifiedUid,
                expectedUid = verifiedUid,
                operation = OperationType.LIST,
                targetPath = colRef.path
            )
            snapshot.toObjects(
                FirestoreShoppingListDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).filter { it.userId.isBlank() || it.userId == verifiedUid }
                .sortedWith(
                    compareBy<FirestoreShoppingListDocument> { if (it.id == "l1") 0 else 1 }
                        .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                ).map { it.toDomainModel() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun getShoppingListById(userId: String, listId: String): Result<ShoppingList?> = runCatching {
        val targetPath = "users/${userId.trim()}/shoppingLists/${listId.trim()}"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.GET, targetPath)
        val docRef = shoppingListsCollection(verifiedUid).document(listId.trim())
        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return@runCatching null
            verifyDocumentOwnership(
                documentUserId = snapshot.getString("userId"),
                expectedUid = verifiedUid,
                operation = OperationType.GET,
                targetPath = docRef.path
            )
            snapshot.toObject(
                FirestoreShoppingListDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.toDomainModel()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun deleteShoppingList(userId: String, listId: String): Result<Unit> = runCatching {
        val targetPath = "users/${userId.trim()}/shoppingLists/${listId.trim()}"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.DELETE, targetPath)
        val docRef = shoppingListsCollection(verifiedUid).document(listId.trim())
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeBravisimaCard(userId: String, cardId: String = PRIMARY_CARD_ID): Flow<BravisimaCard?> = flow {
        val cleanCardId = cardId.ifBlank { PRIMARY_CARD_ID }
        val targetPath = "users/${userId.trim()}/bravisimaCards/$cleanCardId"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.GET, targetPath)
        val docRef = bravisimaCardsCollection(verifiedUid).document(cleanCardId)

        emitAll(
            docRef.snapshots().map { snapshot ->
                verifyDocumentOwnership(
                    documentUserId = snapshot.getString("userId"),
                    expectedUid = verifiedUid,
                    operation = OperationType.GET,
                    targetPath = docRef.path
                )
                snapshot.toObject(
                    FirestoreBravisimaCardDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.takeIf { it.userId.isBlank() || it.userId == verifiedUid }?.toDomainModel()
            }
        )
    }

    fun observeBravisimaMovements(userId: String): Flow<List<BravisimaMovement>> = flow {
        val targetPath = "users/${userId.trim()}/bravisimaMovements"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.LIST, targetPath)
        val colRef = bravisimaMovementsCollection(verifiedUid)

        emitAll(
            colRef.snapshots().map { snapshot ->
                verifyDocumentOwnership(
                    documentUserId = verifiedUid,
                    expectedUid = verifiedUid,
                    operation = OperationType.LIST,
                    targetPath = colRef.path
                )
                val docs = snapshot.toObjects(
                    FirestoreBravisimaMovementDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                docs.filter { it.userId.isBlank() || it.userId == verifiedUid }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    .map { it.toDomainModel() }
            }
        )
    }

    suspend fun getBravisimaCard(userId: String, cardId: String = PRIMARY_CARD_ID): Result<BravisimaCard?> = runCatching {
        val cleanCardId = cardId.ifBlank { PRIMARY_CARD_ID }
        val targetPath = "users/${userId.trim()}/bravisimaCards/$cleanCardId"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.GET, targetPath)
        val docRef = bravisimaCardsCollection(verifiedUid).document(cleanCardId)
        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return@runCatching null
            verifyDocumentOwnership(
                documentUserId = snapshot.getString("userId"),
                expectedUid = verifiedUid,
                operation = OperationType.GET,
                targetPath = docRef.path
            )
            snapshot.toObject(
                FirestoreBravisimaCardDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.takeIf { it.userId.isBlank() || it.userId == verifiedUid }?.toDomainModel()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveOrUpdateBravisimaCard(userId: String, card: BravisimaCard): Result<BravisimaCard> = runCatching {
        val cleanId = card.cardId.ifBlank { PRIMARY_CARD_ID }
        val targetPath = "users/${userId.trim()}/bravisimaCards/$cleanId"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.WRITE, targetPath)
        val docRef = bravisimaCardsCollection(verifiedUid).document(cleanId)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                verifyDocumentOwnership(
                    documentUserId = existingSnap.getString("userId"),
                    expectedUid = verifiedUid,
                    operation = OperationType.UPDATE,
                    targetPath = docRef.path
                )
            }
            val cleanHolder = card.holderName.trim().ifBlank { "Cliente Bravísimo" }.take(100)
            val cleanEmail = card.holderEmail?.trim()?.takeIf { it.isNotBlank() }?.take(160)
            val cleanStatus = if (card.isLocked) "locked" else "active"
            val cleanCategory = card.preferredCategory.trim().ifBlank { "Todo Bravo" }.take(80)
            val cleanCodes = card.claimedPromoCodes.distinct().take(20).map { it.take(40) }
            val resolvedCardNumber = existingSnap.getString("cardNumber")
                ?: card.cardNumber.trim().takeIf {
                    it.matches(Regex("^BRV-[0-9A-Z\\-]+$")) && it.length in 6..32
                }
                ?: generateBravisimaCardNumber(verifiedUid)

            if (!existingSnap.exists()) {
                val createPayload = mapOf(
                    "userId" to verifiedUid,
                    "cardNumber" to resolvedCardNumber.take(32),
                    "holderName" to cleanHolder,
                    "holderEmail" to cleanEmail,
                    "balance" to card.balance.coerceIn(0, 1000000),
                    "lifetimeEarned" to card.lifetimeEarned.coerceIn(0, 5000000),
                    "status" to cleanStatus,
                    "autoRedeemInCart" to card.autoRedeemInCart,
                    "monthlyGoal" to card.monthlyGoal.coerceIn(100, 50000),
                    "preferredCategory" to cleanCategory,
                    "claimedPromoCodes" to cleanCodes,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createPayload).await()
            } else {
                val existingLifetime = existingSnap.getLong("lifetimeEarned")?.toInt() ?: 0
                val safeLifetime = card.lifetimeEarned.coerceAtLeast(existingLifetime).coerceIn(0, 5000000)
                val updatePayload = mapOf(
                    "holderName" to cleanHolder,
                    "holderEmail" to cleanEmail,
                    "balance" to card.balance.coerceIn(0, 1000000),
                    "lifetimeEarned" to safeLifetime,
                    "status" to cleanStatus,
                    "autoRedeemInCart" to card.autoRedeemInCart,
                    "monthlyGoal" to card.monthlyGoal.coerceIn(100, 50000),
                    "preferredCategory" to cleanCategory,
                    "claimedPromoCodes" to cleanCodes,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updatePayload).await()
            }
            card.copy(
                cardId = cleanId,
                cardNumber = resolvedCardNumber
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun recordBravisimaMovement(userId: String, movement: BravisimaMovement): Result<String> = runCatching {
        val cleanId = movement.id.ifBlank { "mov_${System.currentTimeMillis()}" }
        val targetPath = "users/${userId.trim()}/bravisimaMovements/$cleanId"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.CREATE, targetPath)
        val docRef = bravisimaMovementsCollection(verifiedUid).document(cleanId)
        val cleanType = when (movement.movementType.lowercase()) {
            "earn", "redeem", "bonus", "adjustment" -> movement.movementType.lowercase()
            else -> if (movement.amount >= 0) "earn" else "redeem"
        }
        val payload = mapOf(
            "userId" to verifiedUid,
            "title" to movement.title.trim().ifBlank { "Movimiento Bravísima" }.take(120),
            "subtitle" to movement.subtitle.trim().ifBlank { "Tarjeta Digital" }.take(120),
            "dateLabel" to movement.dateLabel.trim().ifBlank { "Hoy" }.take(80),
            "amount" to movement.amount.coerceIn(-500000, 500000),
            "movementType" to cleanType,
            "orderId" to movement.orderId?.trim()?.takeIf { it.isNotBlank() }?.take(80),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            docRef.set(payload).await()
            cleanId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getUserBravisimaMovements(userId: String): Result<List<BravisimaMovement>> = runCatching {
        val targetPath = "users/${userId.trim()}/bravisimaMovements"
        val verifiedUid = requireAuthenticatedUserId(userId, OperationType.LIST, targetPath)
        val colRef = bravisimaMovementsCollection(verifiedUid)
        try {
            val snapshot = colRef.get().await()
            verifyDocumentOwnership(
                documentUserId = verifiedUid,
                expectedUid = verifiedUid,
                operation = OperationType.LIST,
                targetPath = colRef.path
            )
            snapshot.toObjects(
                FirestoreBravisimaMovementDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).filter { it.userId.isBlank() || it.userId == verifiedUid }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                .map { it.toDomainModel() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun seedInitialDataIfEmpty(
        userId: String,
        defaultOrders: List<OrderRecord>,
        defaultLists: List<ShoppingList>,
        defaultCard: BravisimaCard? = null,
        defaultMovements: List<BravisimaMovement> = emptyList(),
        defaultProfile: UserProfile? = null,
        activeBranch: String? = null
    ) {
        try {
            val verifiedUid = requireAuthenticatedUserId(userId, OperationType.WRITE, "users/${userId.trim()}")
            if (defaultProfile != null) {
                val profileSnap = profileCollection(verifiedUid).document(MAIN_PROFILE_ID).get().await()
                if (!profileSnap.exists()) {
                    val safeProfile = defaultProfile.copy(
                        bravisimaNumber = generateBravisimaCardNumber(verifiedUid)
                    )
                    saveOrUpdateUserProfile(verifiedUid, safeProfile, activeBranch)
                }
            }
            val ordersSnap = ordersCollection(verifiedUid).limit(1).get().await()
            if (ordersSnap.isEmpty) {
                defaultOrders.forEach { order ->
                    createOrder(verifiedUid, order)
                }
            }
            val listsSnap = shoppingListsCollection(verifiedUid).limit(1).get().await()
            if (listsSnap.isEmpty) {
                defaultLists.forEach { list ->
                    saveShoppingList(verifiedUid, list)
                }
            }
            if (defaultCard != null) {
                val cardSnap = bravisimaCardsCollection(verifiedUid).document(PRIMARY_CARD_ID).get().await()
                if (!cardSnap.exists()) {
                    val safeCard = defaultCard.copy(
                        cardId = PRIMARY_CARD_ID,
                        cardNumber = generateBravisimaCardNumber(verifiedUid)
                    )
                    saveOrUpdateBravisimaCard(verifiedUid, safeCard)
                }
            }
            if (defaultMovements.isNotEmpty()) {
                val movSnap = bravisimaMovementsCollection(verifiedUid).limit(1).get().await()
                if (movSnap.isEmpty) {
                    defaultMovements.forEach { mov ->
                        recordBravisimaMovement(verifiedUid, mov)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId")
        }
    }

    companion object {
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
}
