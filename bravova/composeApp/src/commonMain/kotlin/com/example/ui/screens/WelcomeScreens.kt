package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BravoCatalogData
import com.example.resources.Res
import com.example.resources.logo_arca_official
import com.example.resources.logo_bravo_official
import com.example.ui.theme.PlusJakartaSans
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Bienvenida de la primera vez (pasos 1 y 2 de 3; el 3 es la pantalla de entrar que ya existía).
 *
 * Para qué: que la app arranque en la tienda y la sucursal correctas, sin pedir permisos.
 * La ubicación se pide solo cuando hace falta (al calcular la entrega), no aquí.
 * [onFinish] recibe la tienda ("BRAVO" o "ARCA") y la sucursal elegida (o null si la salta).
 */
@Composable
fun WelcomeFlow(onFinish: (store: String, branch: String?) -> Unit) {
    // La app abre siempre en Supermercados Bravo; El Arca se elige luego desde el logo del inicio.
    // Por eso la bienvenida solo pregunta la sucursal (paso 1 de 2; el 2 es la pantalla de entrar).
    val store = "BRAVO"
    var branch by rememberSaveable { mutableStateOf<String?>(null) }
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 12.dp)
            .testTag("welcome_flow")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paso 1 de 2",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { onFinish(store, branch) },
                modifier = Modifier.heightIn(min = 44.dp).testTag("welcome_skip")
            ) { Text("Saltar", fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { i ->
                Box(
                    Modifier
                        .height(4.dp)
                        .width(if (i == 0) 28.dp else 14.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (i == 0) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            WelcomeTitle(
                "¿Cuál es tu Bravo más cercano?",
                "Así calculamos la entrega y te mostramos lo que hay en tu sucursal. No te pedimos la ubicación ahora."
            )
            Spacer(Modifier.height(16.dp))
            BravoCatalogData.BRAVO_BRANCHES.forEach { b ->
                BranchRow(b, selected = branch == b) { branch = b }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Puedes cambiarla luego en Cuenta > Direcciones.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // "Saltar" (arriba) es la única salida sin sucursal; antes este botón decía "Elegir después"
        // y hacía lo mismo, con dos salidas iguales en pantalla. Ahora se activa al elegir una.
        Button(
            onClick = { onFinish(store, branch) },
            enabled = branch != null,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("welcome_next")
        ) {
            Text(
                text = "Continuar",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun WelcomeTitle(title: String, body: String) {
    Text(
        text = title,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(Modifier.height(8.dp))
    Text(text = body, fontSize = 15.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BranchRow(name: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .heightIn(min = 52.dp)
            .semantics { this.selected = selected }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sin elegir: aro con borde (el relleno gris al 25 % casi no se veía sobre la tarjeta blanca)
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .then(
                        if (selected) Modifier.background(accent)
                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary))
            }
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
