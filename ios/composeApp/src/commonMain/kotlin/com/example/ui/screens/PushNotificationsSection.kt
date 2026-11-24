package com.example.ui.screens

import com.example.platform.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FcmTopicSubscription
import com.example.model.PushNotificationCategory
import com.example.model.PushNotificationRecord
import com.example.notifications.BravoPushNotificationCenter
import com.example.ui.components.AnimatedPurchaseButton
import com.example.ui.components.DiagonalMetadataStrip
import com.example.ui.components.HapticFeedbackManager
import com.example.ui.components.HapticStyle
import com.example.ui.components.TranslucentCard
import com.example.ui.components.performCrispHaptic
import com.example.ui.theme.BravoAmber
import com.example.ui.theme.PlusJakartaSans

@Composable
fun FcmPushControlAndFeedCard(
    fcmToken: String,
    fcmTopics: List<FcmTopicSubscription>,
    pushFeed: List<PushNotificationRecord>,
    darkMode: Boolean,
    primaryColor: Color,
    onToggleTopic: (topicKey: String, enabled: Boolean) -> Unit,
    onTriggerOfferPush: () -> Unit,
    onTriggerOrderPush: () -> Unit,
    onSelectNotification: (PushNotificationRecord) -> Unit,
    onMarkAllRead: () -> Unit
) {
    val context = currentPlatformContext()
    val view = currentPlatformView()
    var hasSystemPermission by remember {
        mutableStateOf(BravoPushNotificationCenter.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberPermissionLauncher { granted ->
        hasSystemPermission = granted
        if (granted) {
            onTriggerOrderPush()
        }
    }

    val unreadCount = remember(pushFeed) { pushFeed.count { !it.isRead } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TranslucentCard(
            darkMode = darkMode,
            cornerRadius = 22.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fcm_push_control_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(primaryColor, BravoAmber))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            DiagonalMetadataStrip(
                                items = listOf("ALERTAS EN TIEMPO REAL", "Google Sync"),
                                slashColor = primaryColor,
                                fontSizeSp = 10f
                            )
                            Text(
                                text = "Canales Push: Ofertas y Pedidos",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (hasSystemPermission) Color(0xFFECFDF5) else primaryColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    if (!hasSystemPermission && PlatformInfo.sdkInt >= AndroidVersions.TIRAMISU) {
                                        notificationPermissionLauncher.launch(AppPermission.POST_NOTIFICATIONS)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (hasSystemPermission) Color(0xFF047857) else primaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (hasSystemPermission) "Alertas Activas" else "Activar Permiso",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                color = if (hasSystemPermission) Color(0xFF047857) else primaryColor
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "CANAL DE NOTIFICACIONES EN VIVO ACTIVO",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.5.sp,
                            color = primaryColor
                        )
                        Text(
                            text = fcmToken,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                fcmTopics.forEach { topic ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topic.title,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = topic.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = topic.enabled,
                            onCheckedChange = { enabled ->
                                performCrispHaptic(view, HapticStyle.TICK)
                                onToggleTopic(topic.topicKey, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = primaryColor
                            ),
                            modifier = Modifier.testTag("fcm_topic_switch_${topic.topicKey}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            HapticFeedbackManager.onConfirmAction(view)
                            if (!hasSystemPermission && PlatformInfo.sdkInt >= AndroidVersions.TIRAMISU) {
                                notificationPermissionLauncher.launch(AppPermission.POST_NOTIFICATIONS)
                            }
                            onTriggerOfferPush()
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.3.dp, primaryColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trigger_offer_push_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Probar Oferta Push",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            color = primaryColor,
                            maxLines = 1
                        )
                    }

                    AnimatedPurchaseButton(
                        text = "Estado Pedido Push",
                        icon = Icons.Default.LocalShipping,
                        primaryColor = primaryColor,
                        onClick = {
                            HapticFeedbackManager.onConfirmAction(view)
                            if (!hasSystemPermission && PlatformInfo.sdkInt >= AndroidVersions.TIRAMISU) {
                                notificationPermissionLauncher.launch(AppPermission.POST_NOTIFICATIONS)
                            }
                            onTriggerOrderPush()
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "trigger_order_push_button"
                    )
                }
            }
        }

        TranslucentCard(
            darkMode = darkMode,
            cornerRadius = 22.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fcm_notifications_inbox_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                        Text(
                            text = "Bandeja de Alertas Push ($unreadCount nuevas)",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        DiagonalMetadataStrip(
                            items = listOf("Toca una alerta para abrir la oferta o el mapa en vivo"),
                            slashColor = primaryColor,
                            fontSizeSp = 10.5f
                        )
                    }

                    if (unreadCount > 0) {
                        Surface(
                            onClick = {
                                performCrispHaptic(view, HapticStyle.TICK)
                                onMarkAllRead()
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = primaryColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Leídas",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }

                pushFeed.take(8).forEachIndexed { idx, item ->
                    val isOrder = item.category == PushNotificationCategory.ORDER_STATUS
                    val accent = if (isOrder) Color(0xFF059669) else primaryColor

                    if (idx > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (!item.isRead) accent.copy(alpha = 0.08f) else Color.Transparent
                            )
                            .border(
                                width = if (!item.isRead) 1.dp else 0.dp,
                                color = if (!item.isRead) accent.copy(alpha = 0.28f) else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                HapticFeedbackManager.onNavigationClick(view)
                                onSelectNotification(item)
                            }
                            .padding(12.dp)
                            .testTag("push_notification_item_${item.id}"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!item.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = accent.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = if (isOrder) "PEDIDO EN TIEMPO REAL" else "OFERTA EXCLUSIVA",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.5.sp,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.timestampLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = accent
                            )
                        }

                        if (item.progressPct != null) {
                            LinearProgressIndicator(
                                progress = { (item.progressPct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(99.dp)),
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
