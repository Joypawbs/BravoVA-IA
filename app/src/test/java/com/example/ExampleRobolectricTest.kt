package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.OrderDeliveryStage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context and verify order delivery stage mapping`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BravoVa", appName)

    assertEquals(OrderDeliveryStage.RECEIVED, OrderDeliveryStage.fromStatusString("Orden recibida"))
    assertEquals(OrderDeliveryStage.PREPARED, OrderDeliveryStage.fromStatusString("Envío preparado"))
    assertEquals(OrderDeliveryStage.IN_TRANSIT, OrderDeliveryStage.fromStatusString("En camino"))
    assertEquals(OrderDeliveryStage.ARRIVING, OrderDeliveryStage.fromStatusString("Llegando a tu dirección"))
    assertEquals(OrderDeliveryStage.DELIVERED, OrderDeliveryStage.fromStatusString("Entregado"))
    assertEquals(OrderDeliveryStage.IN_TRANSIT, OrderDeliveryStage.PREPARED.nextStage())
  }
}
