package com.example

import com.example.data.ArcaCatalogData
import com.example.data.BravoCatalogData
import com.example.security.BiometricCheckoutAuthenticator
import com.example.security.BiometricCriticalOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun catalog_has_750_unique_skus_24_sections_and_real_images() {
        val bravoProducts = BravoCatalogData.BRAVO_PRODUCTS
        val arcaProducts = ArcaCatalogData.ARCA_PRODUCTS
        val bravoSections = BravoCatalogData.BRAVO_SECTIONS
        val arcaSections = ArcaCatalogData.ARCA_SECTIONS

        assertEquals(500, bravoProducts.size)
        assertEquals(250, arcaProducts.size)
        assertEquals(16, bravoSections.size)
        assertEquals(8, arcaSections.size)

        val bravoSectionNames = bravoSections.map { it.name }.toSet()
        val arcaSectionNames = arcaSections.map { it.name }.toSet()

        assertEquals(500, bravoProducts.map { it.sku }.toSet().size)
        assertEquals(250, arcaProducts.map { it.sku }.toSet().size)

        val allowedDomains = listOf(
            "https://images.openfoodfacts.org/",
            "https://images.openproductsfacts.org/",
            "https://images.openbeautyfacts.org/",
            "https://images.openpetfoodfacts.org/",
            "https://www.themealdb.com/images/ingredients/",
            "https://www.thecocktaildb.com/images/ingredients/",
            "https://cdn.dummyjson.com/product-images/"
        )

        bravoProducts.forEach { p ->
            assertTrue("Orphaned Bravo category for ${p.sku}: ${p.category}", p.category in bravoSectionNames)
            val url = p.imageUrl.orEmpty()
            assertTrue("Invalid or generic imageUrl for ${p.sku}: $url", allowedDomains.any { url.startsWith(it) })
        }
        arcaProducts.forEach { p ->
            assertTrue("Orphaned Arca category for ${p.sku}: ${p.category}", p.category in arcaSectionNames)
            val url = p.imageUrl.orEmpty()
            assertTrue("Invalid or generic imageUrl for ${p.sku}: $url", allowedDomains.any { url.startsWith(it) })
        }
    }

    @Test
    fun biometric_authenticator_generates_valid_tokens_for_all_critical_operations() {
        val payToken = BiometricCheckoutAuthenticator.generateVerificationToken(
            BiometricCriticalOperation.CONFIRM_PAYMENT_AND_ORDER,
            amountRd = 1480
        )
        val brvToken = BiometricCheckoutAuthenticator.generateVerificationToken(
            BiometricCriticalOperation.AUTHORIZE_BRAVISIMA_REDEMPTION,
            amountRd = 340
        )
        val ulkToken = BiometricCheckoutAuthenticator.generateVerificationToken(
            BiometricCriticalOperation.UNLOCK_BRAVISIMA_CARD,
            amountRd = 340
        )
        val cardToken = BiometricCheckoutAuthenticator.generateVerificationToken(
            BiometricCriticalOperation.CHANGE_PAYMENT_METHOD,
            amountRd = 1480
        )
        val aiToken = BiometricCheckoutAuthenticator.generateVerificationToken(
            BiometricCriticalOperation.CONFIRM_AI_ORDER,
            amountRd = 1329
        )

        assertTrue(payToken.startsWith("BIO-PAY-"))
        assertTrue(brvToken.startsWith("BIO-BRV-"))
        assertTrue(ulkToken.startsWith("BIO-ULK-"))
        assertTrue(cardToken.startsWith("BIO-CRD-"))
        assertTrue(aiToken.startsWith("BIO-BIA-"))
    }
}
