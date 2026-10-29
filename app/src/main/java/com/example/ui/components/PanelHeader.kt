package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/**
 * Header uniforme per i pannelli overlay (notifiche, lista chat, chat, impostazioni):
 * freccia indietro sempre nella STESSA posizione (start) e titolo alla stessa altezza.
 * La freccia chiude con la transizione inversa (LocalPanelDismiss). La riga è anche la
 * maniglia di trascinamento (swipe per chiudere). I chiamanti mettono sopra solo
 * `statusBarsPadding() + padding(top = 12.dp)` così tutti partono alla stessa quota.
 */
@Composable
fun PanelHeaderBase(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val dismiss = LocalPanelDismiss.current
    Row(
        modifier = LocalDialogDragHandle.current
            .then(modifier)
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = dismiss, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
                tint = PureWhite,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        content()
    }
}

/** Variante con titolo testuale (+ badge opzionale), per notifiche/lista chat/impostazioni. */
@Composable
fun PanelHeader(
    title: String,
    badge: Int = 0,
    modifier: Modifier = Modifier
) {
    PanelHeaderBase(modifier = modifier) {
        Text(
            text = title,
            color = PureWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
        if (badge > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.toString(),
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
