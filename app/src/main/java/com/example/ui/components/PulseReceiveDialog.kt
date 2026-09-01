package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ActivePulse
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray

/**
 * Dialog immersiva dedicata a UN Pulse ricevuto. Swipe-down per chiudere (niente tasto).
 * All'apertura parte in automatico vibrazione + onda; "Risenti" le rilancia.
 */
@Composable
fun PulseReceiveDialog(pulse: ActivePulse, onDismiss: () -> Unit) {
    val accent = pulseAccentFor(pulse.senderId)
    val context = androidx.compose.ui.platform.LocalContext.current
    var trigger by remember(pulse.samples) { mutableIntStateOf(0) }
    var audioPath by remember(pulse.audioId) { androidx.compose.runtime.mutableStateOf<String?>(null) }

    // Scarica la voce (se presente) e poi avvia: audio + vibrazione + onda insieme.
    LaunchedEffect(pulse.audioId, pulse.samples) {
        if (!pulse.audioId.isNullOrBlank() || !pulse.audioUrl.isNullOrBlank()) {
            com.example.data.FirebaseRepository.fetchPulseAudioToFile(context, pulse.audioId ?: "", { path ->
                audioPath = path
                if (path == null) android.widget.Toast.makeText(context, "Pulse: voce non scaricata", android.widget.Toast.LENGTH_SHORT).show()
                trigger += 1
            }, audioUrl = pulse.audioUrl)
        } else {
            android.widget.Toast.makeText(context, "Pulse: nessuna voce allegata", android.widget.Toast.LENGTH_SHORT).show()
            trigger += 1
        }
    }

    UtilityDialog(onDismiss = onDismiss, swipeAnywhere = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .statusBarsPadding()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PULSE",
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "da ${pulse.senderName.ifBlank { "un amico" }}",
                color = PureWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Foto profilo circondata dall'onda che pulsa in sync con la vibrazione
            PulseWavePlayer(
                samples = pulse.samples,
                accent = accent,
                playTrigger = trigger,
                modifier = Modifier.size(280.dp),
                audioFilePath = audioPath
            ) {
                AsyncImage(
                    model = pulse.avatarUrl,
                    contentDescription = pulse.senderName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Risenti
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { trigger++ }
                    )
                    .padding(horizontal = 26.dp, vertical = 12.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Text("Risenti", color = PureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "scorri giù per chiudere",
                color = SubtitleGray.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}
