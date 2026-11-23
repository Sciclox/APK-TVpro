package com.tvpro.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Official TV Pro / LaMovie Color Palette (Exact match with TV version)
val LaMovieDarkBg = Color(0xFF03090F)
val LaMovieCardBg = Color(0xFF242731)
val LaMovieSurface = Color(0xFF1A1C22)
val LaMovieMuted = Color(0xFF787D88)
val LaMovieBorder = Color(0xFF2E3342)
val LaMovieRed = Color(0xFFFF4343)

// Luxury Monochromatic Cinema Tokens (Directly mirroring the TV app)
val WarmIvory = Color(0xFFF5F2EB)       // Primary high-prominence text, active pills, play buttons
val SoftSilver = Color(0xFFD6D0C4)      // Secondary badges, LAT capsule, metadata
val MutedSlate = Color(0xFF9CA3AF)      // Unselected tabs, synopsis, tertiary info
val DeepObsidian = Color(0xFF06080C)    // Pure cinematic dark tone
val PillSurface = Color(0xD90B0E14)     // Translucent dark glass for badges & inactive pills
val SubtleBorder = Color.White.copy(alpha = 0.14f) // Glassmorphism micro-border
val CardGradientStart = Color(0xFF242731)
val CardGradientMid = Color(0xFF161922)
val CardGradientEnd = Color(0xFF0D0F14)

// Direct aliases
val TvProDarkBg = LaMovieDarkBg
val TvProPureBlack = Color(0xFF03090F)
val TvProSurface = LaMovieSurface
val TvProCardBg = LaMovieCardBg
val TvProBorder = LaMovieBorder
val TvProMuted = LaMovieMuted
val TvProWhite = Color(0xFFFFFFFF)
val TvProYellow = Color(0xFFFFC107)
val TvProRed = LaMovieRed

// Legacy compatibility
val NetflixRed = LaMovieRed
val NetflixDarkRed = Color(0xFFD63636)
val NetflixBlack = LaMovieDarkBg
val NetflixPureBlack = Color(0xFF03090F)
val NetflixDarkGrey = LaMovieSurface
val NetflixMediumGrey = LaMovieCardBg
val NetflixLightGrey = LaMovieMuted
val NetflixWhite = Color(0xFFFFFFFF)
