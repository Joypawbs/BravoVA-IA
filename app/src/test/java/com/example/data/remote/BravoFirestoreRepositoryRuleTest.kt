package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.BravisimaCard
import com.example.model.BravisimaMovement
import com.example.model.OrderRecord
import com.example.model.ShoppingList
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class BravoFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var repository: BravoFirestoreRepository

    @Before
    fun setUpRepository() {
        repository = BravoFirestoreRepository(firestore)
    }

    @Test
    fun getUserProfile_and_Cart_whenUnauthenticated_returnsPermissionDenied(): Unit = runBlocking {
        auth.signOut()

        val profileResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getUserProfile("some-user-uid")
            }
        }
        assertTrue("Expected failure for unauthenticated profile read", profileResult.isFailure)
        val profileEx = profileResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", profileEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, profileEx?.code)

        val cartResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getUserCartItems("some-user-uid")
            }
        }
        assertTrue("Expected failure for unauthenticated cart read", cartResult.isFailure)
        val cartEx = cartResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", cartEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, cartEx?.code)

        val cardResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getBravisimaCard("some-user-uid")
            }
        }
        assertTrue("Expected failure for unauthenticated BravisimaCard read", cardResult.isFailure)
        val cardEx = cardResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", cardEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, cardEx?.code)
    }

    @Test
    fun repositoryUidValidation_blocksMismatchedUserId_andPreventsBravisimaCardLeakage(): Unit = runBlocking {
        val aliceUid = signInTestUser("alice.bravisima.guard@bravova.com.do")
        assertEquals(aliceUid, FirebaseAuth.getInstance().currentUser?.uid)

        val aliceCardNumber = BravoFirestoreRepository.generateBravisimaCardNumber(aliceUid)
        val aliceCard = BravisimaCard(
            cardId = BravoFirestoreRepository.PRIMARY_CARD_ID,
            cardNumber = aliceCardNumber,
            holderName = "Alice Titular",
            holderEmail = "alice.bravisima.guard@bravova.com.do",
            balance = 480,
            lifetimeEarned = 920
        )

        // Alice saves her own Tarjeta Bravisima
        val aliceSave = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.saveOrUpdateBravisimaCard(aliceUid, aliceCard)
            }
        }
        assertTrue(aliceSave.isSuccess)

        // Switch session to Bob
        val bobUid = signInTestUser("bob.bravisima.guard@bravova.com.do")
        assertEquals(bobUid, FirebaseAuth.getInstance().currentUser?.uid)
        assertNotEquals(aliceUid, bobUid)

        // Bob attempting to read Alice's BravisimaCard via repository is blocked by currentUser?.uid validation
        val bobReadAliceCard = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getBravisimaCard(aliceUid)
            }
        }
        assertTrue(bobReadAliceCard.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            bobReadAliceCard.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // Bob attempting to overwrite Alice's BravisimaCard is blocked
        val bobWriteAliceCard = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.saveOrUpdateBravisimaCard(aliceUid, aliceCard.copy(balance = 0))
            }
        }
        assertTrue(bobWriteAliceCard.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            bobWriteAliceCard.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // Bob attempting to observe Alice's BravisimaCard stream fails immediately with PERMISSION_DENIED
        try {
            withContext(Dispatchers.IO) {
                withTimeout(3000L) {
                    repository.observeBravisimaCard(aliceUid).first()
                }
            }
            fail("Expected observeBravisimaCard for another user's UID to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }

        // Bob attempting to record a movement on Alice's card is blocked
        val bobMovementAttempt = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.recordBravisimaMovement(
                    aliceUid,
                    BravisimaMovement(
                        id = "mov_unauthorized_1",
                        title = "Canje no autorizado",
                        subtitle = "Intento cruzado",
                        dateLabel = "Hoy",
                        amount = -200,
                        movementType = "redeem"
                    )
                )
            }
        }
        assertTrue(bobMovementAttempt.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            bobMovementAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun userProfile_and_Cart_whenOwnerAuthenticated_succeedsAndEnforcesRules(): Unit = runBlocking {
        val ownerUid = signInTestUser("valeria.profile.cart@bravova.com.do")
        val cardNumber = BravoFirestoreRepository.generateBravisimaCardNumber(ownerUid)

        val initialProfile = UserProfile(
            name = "Valeria Gómez",
            email = "valeria.profile.cart@bravova.com.do",
            phone = "809-555-0192",
            cedula = "001-1849201-7",
            bravisimaNumber = cardNumber,
            rncNit = "101-01992-1",
            ncfType = "Crédito Fiscal (B01)"
        )

        val saveProfileResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.saveOrUpdateUserProfile(ownerUid, initialProfile, "Bravo Naco")
            }
        }
        assertTrue(
            "Expected saveOrUpdateUserProfile success but got: ${saveProfileResult.exceptionOrNull()}",
            saveProfileResult.isSuccess
        )

        val getProfileResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getUserProfile(ownerUid)
            }
        }
        assertTrue(getProfileResult.isSuccess)
        val fetchedProfile = getProfileResult.getOrNull()
        assertNotNull(fetchedProfile)
        assertEquals("Valeria Gómez", fetchedProfile?.name)
        assertEquals(cardNumber, fetchedProfile?.bravisimaNumber)
        assertEquals("Crédito Fiscal (B01)", fetchedProfile?.ncfType)

        // Update profile fields while preserving immutable bravisimaNumber
        val updatedProfile = initialProfile.copy(
            name = "Valeria Gómez de Santos",
            phone = "809-555-9900"
        )
        val updateProfileResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.saveOrUpdateUserProfile(ownerUid, updatedProfile, "Bravo Churchill")
            }
        }
        assertTrue(
            "Expected profile update success but got: ${updateProfileResult.exceptionOrNull()}",
            updateProfileResult.isSuccess
        )
        val fetchedUpdatedProfile = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getUserProfile(ownerUid)
            }
        }.getOrNull()
        assertEquals("Valeria Gómez de Santos", fetchedUpdatedProfile?.name)
        assertEquals(cardNumber, fetchedUpdatedProfile?.bravisimaNumber)

        // Add items to cart
        val addBrvResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.setCartItemQuantity(ownerUid, "BRV-001", 3, "BRAVO")
            }
        }
        assertTrue("Expected cart add BRV-001 to succeed: ${addBrvResult.exceptionOrNull()}", addBrvResult.isSuccess)

        val addArcResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.setCartItemQuantity(ownerUid, "ARC-005", 2, "ARCA")
            }
        }
        assertTrue("Expected cart add ARC-005 to succeed: ${addArcResult.exceptionOrNull()}", addArcResult.isSuccess)

        // List cart items
        val listCartResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getUserCartItems(ownerUid)
            }
        }
        assertTrue(listCartResult.isSuccess)
        val cartItems = listCartResult.getOrDefault(emptyList())
        assertEquals(2, cartItems.size)
        assertEquals(3, cartItems.find { it.sku == "BRV-001" }?.qty)
        assertEquals(2, cartItems.find { it.sku == "ARC-005" }?.qty)

        // Update cart item quantity
        val updateQtyResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.setCartItemQuantity(ownerUid, "BRV-001", 5, "BRAVO")
            }
        }
        assertTrue(updateQtyResult.isSuccess)
        val updatedCartItems = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getUserCartItems(ownerUid)
            }
        }.getOrDefault(emptyList())
        assertEquals(5, updatedCartItems.find { it.sku == "BRV-001" }?.qty)

        // Remove single item by setting qty = 0
        val removeSingleResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.setCartItemQuantity(ownerUid, "ARC-005", 0, "ARCA")
            }
        }
        assertTrue(removeSingleResult.isSuccess)

        // Clear remaining cart
        val clearCartResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.clearUserCart(ownerUid)
            }
        }
        assertTrue(clearCartResult.isSuccess)
        val emptyCart = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getUserCartItems(ownerUid)
            }
        }.getOrDefault(emptyList())
        assertTrue(emptyCart.isEmpty())
    }

    @Test
    fun createAndGetOrder_whenOwnerAuthenticated_succeeds(): Unit = runBlocking {
        val ownerUid = signInTestUser("owner.orders@bravova.com.do")
        val orderId = "ORD-RULE-${System.currentTimeMillis()}"
        val order = OrderRecord(
            id = orderId,
            status = "En camino • Repartidor asignado",
            badgeNum = 3,
            date = "Hoy, 2:30 PM",
            total = 1850.0,
            branch = "Bravo Naco",
            skus = listOf("BRV-001", "BRV-002", "BRV-003")
        )

        val createResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.createOrder(ownerUid, order)
            }
        }
        assertTrue("Expected createOrder to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val getResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getOrderById(ownerUid, orderId)
            }
        }
        assertTrue(getResult.isSuccess)
        assertEquals("Bravo Naco", getResult.getOrNull()?.branch)

        val updateStatusResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.updateOrderDeliveryStatus(ownerUid, orderId, "Entregado en puerta", "Hoy, 3:05 PM")
            }
        }
        assertTrue(updateStatusResult.isSuccess)
    }

    @Test
    fun saveAndDeleteShoppingList_whenOwnerAuthenticated_succeeds(): Unit = runBlocking {
        val ownerUid = signInTestUser("owner.lists@bravova.com.do")
        val listId = "list_rule_${System.currentTimeMillis()}"
        val shoppingList = ShoppingList(
            id = listId,
            name = "Cena Familiar",
            skus = listOf("BRV-010", "BRV-015"),
            aiNote = "Generada con Brav IA",
            isAiGenerated = true,
            updatedAtLabel = "Sincronizada ahora"
        )

        val saveResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.saveShoppingList(ownerUid, shoppingList)
            }
        }
        assertTrue("Expected saveShoppingList to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val deleteResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.deleteShoppingList(ownerUid, listId)
            }
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun bravisimaCardAndMovements_whenOwnerAuthenticated_succeedsAndProtectsCard(): Unit = runBlocking {
        val ownerUid = signInTestUser("owner.bravisima@bravova.com.do")
        val cardNumber = BravoFirestoreRepository.generateBravisimaCardNumber(ownerUid)

        val card = BravisimaCard(
            cardId = BravoFirestoreRepository.PRIMARY_CARD_ID,
            cardNumber = cardNumber,
            holderName = "Camila Santos",
            holderEmail = "owner.bravisima@bravova.com.do",
            balance = 245,
            lifetimeEarned = 520,
            isLocked = false,
            autoRedeemInCart = true,
            monthlyGoal = 800,
            preferredCategory = "Frutas y vegetales",
            claimedPromoCodes = listOf("BRAVO25")
        )

        val saveCardResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.saveOrUpdateBravisimaCard(ownerUid, card)
            }
        }
        assertTrue("Expected saveOrUpdateBravisimaCard to succeed: ${saveCardResult.exceptionOrNull()}", saveCardResult.isSuccess)

        val getCardResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.getBravisimaCard(ownerUid)
            }
        }
        assertTrue(getCardResult.isSuccess)
        assertEquals(cardNumber, getCardResult.getOrNull()?.cardNumber)
        assertEquals(245, getCardResult.getOrNull()?.balance)

        val movement = BravisimaMovement(
            id = "mov_rule_${System.currentTimeMillis()}",
            title = "Compra Bravo Naco",
            subtitle = "Orden #ORD-101 • Retorno 1%",
            dateLabel = "Hoy",
            amount = 45,
            movementType = "earn",
            orderId = "ORD-101"
        )
        val movResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) {
                repository.recordBravisimaMovement(ownerUid, movement)
            }
        }
        assertTrue(movResult.isSuccess)
    }
}
