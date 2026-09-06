package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    UtilityDialog(onDismiss = onDismiss, swipeAnywhere = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = SubtitleGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Condivisioni esaurite",
                color = PureWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Hai usato le tue 3 condivisioni di oggi.\nLa scarsità rende ogni scelta più significativa.",
                color = SubtitleGray,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Slot visivi
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF3A3A3A))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = SubtitleGray.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Nuovi slot disponibili tra $minutesUntilReset",
                    color = SubtitleGray.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
