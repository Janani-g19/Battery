package com.example.chargelove

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAX_MESSAGE_LENGTH = 160

/** Everything that changes between "cozy" and "dramatic". */
private data class Mood(
    val background: Color,
    val card: Color,
    val accent: Color,
    val onAccent: Color,
    val text: Color,
    val emoji: String,
    val headline: String,
    val subline: String,
)

private fun moodFor(b: BatteryState): Mood = when {
    b.isCharging-> Mood(
        background = Color(0xFFE6F4EA), card = Color(0xFFCDEBD3),
        accent = Color(0xFF5DB075), onAccent = Color.White, text = Color(0xFF1F4D2E),
        emoji = "🥰",
        headline = "Ahh, that's the good stuff",
        subline = "Thank you for plugging in, my love 💚 Your phone feels so cozy right now.",
    )
    b.level > 20 -> Mood(
        background = Color(0xFFFFF8E1), card = Color(0xFFFFECB3),
        accent = Color(0xFFFFB74D), onAccent = Color(0xFF4E342E), text = Color(0xFF5D4037),
        emoji = "🙂",
        headline = "Battery's doing fine",
        subline = "No worries yet. Keep being wonderful.",
    )
    b.level > 10 -> Mood(
        background = Color(0xFFFFE0B2), card = Color(0xFFFFCC80),
        accent = Color(0xFFFB8C00), onAccent = Color.White, text = Color(0xFF5D3A1A),
        emoji = "😟",
        headline = "Um… we're getting low",
        subline = "A charger would be really nice right about now.",
    )
    b.level > 5 -> Mood(
        background = Color(0xFFFFCCBC), card = Color(0xFFFFAB91),
        accent = Color(0xFFE64A19), onAccent = Color.White, text = Color(0xFF4E1F0F),
        emoji = "😰",
        headline = "Danger zone!",
        subline = "Your phone is sweating. Please find a cable.",
    )
    b.level > 3 -> Mood(
        background = Color(0xFFEF9A9A), card = Color(0xFFE57373),
        accent = Color(0xFFB71C1C), onAccent = Color.White, text = Color(0xFF3B0A0A),
        emoji = "😱",
        headline = "CODE RED",
        subline = "It's writing its will. PLUG. IT. IN.",
    )
    else -> Mood(
        background = Color(0xFF3E2723), card = Color(0xFF5D4037),
        accent = Color(0xFFFF8A80), onAccent = Color(0xFF3E2723), text = Color(0xFFFFEBEE),
        emoji = "💀",
        headline = "It was nice knowing you",
        subline = "Last words: \"charger…\"",
    )
}

/** Live battery state for Compose, updated on every ACTION_BATTERY_CHANGED. */
@Composable
fun rememberBatteryState(): State<BatteryState> {
    val context = LocalContext.current
    val state = remember {
        mutableStateOf(
            BatteryState.from(
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            )
        )
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                state.value = BatteryState.from(intent)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

@Composable
fun BatteryDashboardScreen() {
    val context = LocalContext.current
    val battery by rememberBatteryState()
    val prefs = remember { AppPreferences(context) }

    var savedMessage by remember { mutableStateOf(prefs.customMessage) }
    var draft by remember { mutableStateOf(savedMessage) }

    val mood = moodFor(battery)
    val anim = tween<Color>(durationMillis = 450)
    val background by animateColorAsState(mood.background, anim, label = "bg")
    val card by animateColorAsState(mood.card, anim, label = "card")
    val accent by animateColorAsState(mood.accent, anim, label = "accent")
    val onAccent by animateColorAsState(mood.onAccent, anim, label = "onAccent")
    val textColor by animateColorAsState(mood.text, anim, label = "text")

    // Gentle heartbeat for the emoji; faster and bigger when things are dire.
    val pulse = rememberInfiniteTransition(label = "pulse")
    val emojiScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (battery.isCharging) 1.06f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (battery.isCharging) 1600 else 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "emojiScale",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Crossfade(targetState = mood.emoji, label = "emoji") { emoji ->
            Text(emoji, fontSize = 104.sp, modifier = Modifier.scale(emojiScale))
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (battery.isCharging) "${battery.level}% ⚡" else "${battery.level}%",
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
        )

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { battery.level / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp)),
            color = accent,
            trackColor = card,
        )

        Spacer(Modifier.height(24.dp))

        Crossfade(targetState = mood.headline to mood.subline, label = "copy") { (headline, sub) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    headline,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(sub, fontSize = 16.sp, color = textColor, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(28.dp))

        if (battery.isCharging) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = card, contentColor = textColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "You take such good care of your phone (and of me) 💚\nNow go be cozy. I'll be here.",
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            }
        } else {
            val dirty = draft.trim() != savedMessage
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = card, contentColor = textColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("💌 Message for the 20% nudge", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(MAX_MESSAGE_LENGTH) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        supportingText = { Text("${draft.length}/$MAX_MESSAGE_LENGTH") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = textColor.copy(alpha = 0.4f),
                            cursorColor = accent,
                            focusedSupportingTextColor = textColor,
                            unfocusedSupportingTextColor = textColor.copy(alpha = 0.7f),
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { NotificationHelper.showAlert(context, 20, battery.level) },
                        ) { Text("Send a test nudge", color = textColor) }

                        Button(
                            enabled = dirty && draft.isNotBlank(),
                            onClick = {
                                prefs.customMessage = draft
                                savedMessage = prefs.customMessage
                                draft = savedMessage
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = onAccent,
                            ),
                        ) { Text(if (dirty) "Save" else "Saved ✓") }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}