package com.tvpro.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.mobile.ui.theme.Inter
import com.tvpro.mobile.ui.theme.TvProRed

@Composable
fun TvProLogo(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = "TV",
            color = Color.White,
            fontFamily = Inter,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = ".",
            color = TvProRed,
            fontFamily = Inter,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "PRO",
            color = Color.White,
            fontFamily = Inter,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(TvProRed)
                .padding(horizontal = 4.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = "HD",
                color = Color.White,
                fontSize = 8.5.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}
