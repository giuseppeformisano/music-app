package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray
import java.util.Calendar

@Composable
fun ShareLimitSheet(onDismiss: () -> Unit) {
    val minutesUntilReset = run {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val diff = midnight.timeInMillis - now.timeInMillis
        val hours = diff / 3_600_000
        val mins = (diff % 3_600_000) / 60_000
        if (hours > 0) "${hours}h ${mins}min" else "${mins}min"
    }

    UtilityOverlay(onDismiss = onDismiss, swipeAnywhere = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Freccia indietro in alto a sinistra — stessa posizione degli altri pannelli.
            val dismissPanel = LocalPanelDismiss.current
            IconButton(
                onClick = { dismissPanel() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp, start = 4.dp)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro",
                    tint = PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Messaggio essenziale, centrato.
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Hai già condiviso oggi.\nOgni scelta conta.",
                    color = PureWhite,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Torna tra $minutesUntilReset",
                    color = SubtitleGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
