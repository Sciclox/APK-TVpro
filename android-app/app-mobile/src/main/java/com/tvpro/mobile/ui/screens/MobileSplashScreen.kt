package com.tvpro.mobile.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.tvpro.mobile.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MobileSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    // Entrance and Exit Alpha
    val contentAlpha by animateFloatAsState(
        targetValue = when {
            isExiting -> 0f
            startAnimation -> 1f
            else -> 0f
        },
        animationSpec = tween(
            durationMillis = if (isExiting) 300 else 700,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "splashAlpha"
    )

    // Entrance Scale
    val contentScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.94f,
        animationSpec = tween(
            durationMillis = 850,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "splashScale"
    )

    // Breathing Ambient Glow
    val infiniteTransition = rememberInfiniteTransition(label = "ambientPulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Minimalist Loading Bar Progress
    val progressAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1400)
        isExiting = true
        delay(300)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LaMovieDarkBg),
        contentAlignment = Alignment.Center
    ) {
        // 1. Subtle Atmospheric Radial Glow Behind the Brand Mark (Mobile Portrait Scaled)
        Box(
            modifier = Modifier
                .size(320.dp, 280.dp)
                .graphicsLayer {
                    alpha = contentAlpha * glowPulse
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF263044).copy(alpha = 0.28f),
                            WarmIvory.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Central Editorial Brand Identity (Mobile Portrait Scaled)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .padding(horizontal = 24.dp)
        ) {
            // TV PRO Brand Emblem
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "TV" Luxury Capsule Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
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
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF242731), Color(0xFF141822))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "TV",
                        color = WarmIvory,
                        fontSize = 32.sp,
                        fontFamily = BebasNeue,
                        letterSpacing = 1.5.sp
                    )
                }

                // "PRO" Monumental Typography
                Text(
                    text = "PRO",
                    color = WarmIvory,
                    fontSize = 42.sp,
                    fontFamily = BebasNeue,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle / Tagline with flanking hairlines
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.20f))
                )

                Text(
                    text = "EXPERIENCIA CINEMATOGRÁFICA",
                    color = MutedSlate,
                    fontSize = 9.5.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.2.sp
                )

                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.20f))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Minimalist Luxury Progress Indicator
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnim)
                        .background(
                            Brush.horizontalGradient(
                                listOf(SoftSilver, WarmIvory)
                            )
                        )
                )
            }
        }
    }
}
