package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VersionInfo
import com.example.ui.theme.BlackCard
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SubtitleGray

@Composable
fun UpdateBanner(
    update: VersionInfo?,
    downloadProgress: Int?,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visible = update != null || downloadProgress != null

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BlackCard)
                .border(1.dp, SpotifyGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = if (downloadProgress != null) 4.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (downloadProgress != null) "Download in corso…" else "Aggiornamento disponibile",
                                color = PureWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (update != null) {
                                Text(
                                    text = "v${update.versionName}",
                                    color = SubtitleGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (downloadProgress == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Installa",
                                color = SpotifyGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onInstall() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Chiudi",
                                    tint = SubtitleGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "$downloadProgress%",
                            color = SpotifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }

                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = SpotifyGreen,
                        trackColor = CharcoalBorder
                    )
                }
            }
        }
    }
}
