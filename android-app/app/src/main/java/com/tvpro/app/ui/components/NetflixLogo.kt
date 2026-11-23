package com.tvpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.tvpro.app.ui.theme.Inter
import com.tvpro.app.ui.theme.LaMovieRed

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LaMovieLogo(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = "La",
            color = Color.White,
            fontFamily = Inter,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = ".",
            color = LaMovieRed,
            fontFamily = Inter,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Movie",
            color = Color.White,
            fontFamily = Inter,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(LaMovieRed)
                .padding(horizontal = 4.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = "HD",
                color = Color.White,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun NetflixLogo(modifier: Modifier = Modifier) {
    LaMovieLogo(modifier = modifier)
}
