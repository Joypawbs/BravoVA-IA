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
    var step by rememberSaveable { mutableStateOf(0) }
    var store by rememberSaveable { mutableStateOf("BRAVO") }
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
        // Arriba: progreso (1 de 3) y "Saltar"
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paso ${step + 1} de 3",
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
            repeat(3) { i ->
                Box(
                    Modifier
                        .height(4.dp)
                        .width(if (i == step) 28.dp else 14.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (i <= step) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "welcome_step"
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (s == 0) {
                    WelcomeTitle("¿Dónde quieres comprar?", "Puedes cambiar de tienda cuando quieras desde el logo del inicio.")
                    Spacer(Modifier.height(20.dp))
                    StoreChoice(
                        logo = Res.drawable.logo_bravo_official,
                        title = "Supermercados Bravo",
                        subtitle = "Compra del súper con envío o recogida",
                        selected = store == "BRAVO",
                        tag = "welcome_store_bravo"
                    ) { store = "BRAVO" }
                    Spacer(Modifier.height(12.dp))
                    StoreChoice(
                        logo = Res.drawable.logo_arca_official,
                        title = "El Arca Petshop",
                        subtitle = "Nutrición y cuidado para tus mascotas",
                        selected = store == "ARCA",
                        tag = "welcome_store_arca"
                    ) { store = "ARCA" }
                } else {
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
            }
        }

        Button(
            onClick = {
                when {
                    // El Arca tiene una sola tienda: no hace falta elegir sucursal
                    step == 0 && store == "ARCA" -> onFinish(store, null)
                    step == 0 -> step = 1
                    else -> onFinish(store, branch)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("welcome_next")
        ) {
            Text(
                text = if (step == 0) "Continuar" else if (branch == null) "Elegir después" else "Continuar",
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
private fun StoreChoice(
    logo: DrawableResource,
    title: String,
    subtitle: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(logo), contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary))
            }
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
