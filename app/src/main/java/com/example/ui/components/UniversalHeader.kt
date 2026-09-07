package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.PureWhite

enum class NavigationPage {
    FEED,
    LIVE
}

/**
 * Universal Top Header — solo logo centrato "m" + scritta dinamica "feed" / "live".
 * Search e profilo si trovano nella bottom nav bar.
 */
@Composable
fun UniversalHeader(
    currentPage: NavigationPage,
    currentUser: User,
    onHeaderCenterClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("universal_header"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHeaderCenterClick
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PureWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "m",
                        color = BlackPitch,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (currentPage == NavigationPage.FEED) "feed" else "live",
                    color = PureWhite.copy(alpha = 0.95f),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Cursive,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 1.dp, bottom = 2.dp)
                )
            }
        }
    }
}
