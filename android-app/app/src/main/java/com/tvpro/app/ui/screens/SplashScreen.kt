package com.tvpro.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.tvpro.app.ui.theme.BebasNeue
import com.tvpro.app.ui.theme.Inter
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    // Luxury Design System Colors
    val deepBackground = Color(0xFF06080C)
    val warmIvory = Color(0xFFF5F2EB)
    val softSilver = Color(0xFFD6D0C4)
    val mutedSlate = Color(0xFF9CA3AF)
    val cinematicRed = Color(0xFFE50914)
    val crimsonDark = Color(0xFF8B0000)

    // Entrance and Exit Alpha
    val contentAlpha by animateFloatAsState(
        targetValue = when {
            isExiting -> 0f
            startAnimation -> 1f
            else -> 0f
        },
        animationSpec = tween(
            durationMillis = if (isExiting) 350 else 900,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "splashAlpha"
    )

    // Entrance Scale
    val contentScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.92f,
        animationSpec = tween(
            durationMillis = 1100,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "splashScale"
    )

    // Breathing Ambient Glow
    val infiniteTransition = rememberInfiniteTransition(label = "ambientPulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Minimalist Loading Bar Progress
    val progressAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        isExiting = true
        delay(350)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepBackground),
        contentAlignment = Alignment.Center
    ) {
        // 1. Subtle Atmospheric Radial Glow Behind the Brand Mark
        Box(
            modifier = Modifier
                .size(540.dp, 360.dp)
                .graphicsLayer {
                    alpha = contentAlpha * glowPulse
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cinematicRed.copy(alpha = 0.16f),
                            Color(0xFF1E2433).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Central Editorial Brand Identity
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                }
        ) {
            // TV PRO Brand Emblem
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // "TV" Luxury Capsule Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.36f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            Brush.linearGradient(
                                listOf(cinematicRed, crimsonDark)
                            )
                        )
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TV",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontFamily = BebasNeue,
                        letterSpacing = 2.sp
                    )
                }

                // "PRO" Monumental Typography
                Text(
                    text = "PRO",
                    color = warmIvory,
                    fontSize = 56.sp,
                    fontFamily = BebasNeue,
                    letterSpacing = 4.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Subtitle / Tagline with flanking hairlines
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )

                Text(
                    text = "EXPERIENCIA CINEMATOGRÁFICA",
                    color = mutedSlate,
                    fontSize = 11.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.5.sp
                )

                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
            }

            Spacer(modifier = Modifier.height(38.dp))

            // 3. Minimalist Luxury Progress Indicator
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnim)
                        .background(
                            Brush.horizontalGradient(
                                listOf(cinematicRed, warmIvory)
                            )
                        )
                )
            }
        }
    }
}
