package com.tvpro.mobile.ui.screens

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.mobile.ui.components.MobileMovieCard
import com.tvpro.mobile.ui.components.cleanHtml
import com.tvpro.mobile.ui.components.cleanTitle
import com.tvpro.mobile.ui.components.cleanEpisodeTitle
import com.tvpro.mobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    movie: MovieItem,
    onBack: () -> Unit,
    onPlay: (MovieItem, EpisodeItem?, String?) -> Unit,
    onMovieClick: (MovieItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialCached = remember(movie.url) { MovieRepository.getCachedDetail(movie.url, movie.title) }
    var movieDetail by remember(movie.url) { mutableStateOf<MovieDetailResponse?>(initialCached) }
    var isLoading by remember(movie.url) { mutableStateOf(initialCached == null) }
    var hasNetworkError by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var isInMyList by remember { mutableStateOf(WatchHistoryManager.isInMyList(context, movie.url)) }
    var savedHistory by remember(movie.url) { mutableStateOf(WatchHistoryManager.getMovieHistory(context, movie.url)) }
    var isLiked by remember {
        mutableStateOf(
            context.getSharedPreferences("tvpro_ratings", android.content.Context.MODE_PRIVATE)
                .getBoolean("liked_${movie.url}", false)
        )
    }
    val initialResumeSeason = remember(movie.url) {
        val h = WatchHistoryManager.getMovieHistory(context, movie.url)
        val s = h?.seasonNumber
        if (s != null && s > 0) s else 1
    }
    var selectedSeason by remember(movie.url) { mutableIntStateOf(initialResumeSeason) }
    var showSeasonSheet by remember { mutableStateOf(false) }
    var seasonEpisodes by remember(movie.url) {
        mutableStateOf<List<EpisodeItem>>(
            if (initialResumeSeason == 1) initialCached?.episodes ?: emptyList()
            else emptyList()
        )
    }
    var isSeasonLoading by remember(movie.url) { mutableStateOf(false) }
    var isOverviewExpanded by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("episodes") } // "episodes" or "similar"

    val cleanTitle = remember(movie.title, movieDetail?.title) {
        (movieDetail?.title?.takeIf { it.isNotBlank() } ?: movie.title).cleanTitle()
    }

    val isSeries = remember(movieDetail, movie) {
        movieDetail?.isSeries == true ||
            movie.type == "series" ||
            movie.type == "anime" ||
            movie.type == "drama" ||
            movie.url.contains("/series/") ||
            movie.url.contains("/tvshows/") ||
            movie.url.contains("/animes/") ||
            movie.url.contains("/novelas/")
    }

    LaunchedEffect(movie.url, reloadTrigger) {
        val history = WatchHistoryManager.getMovieHistory(context, movie.url)
        savedHistory = history
        val cached = MovieRepository.getCachedDetail(movie.url, movie.title)
        if (cached != null) {
            movieDetail = cached
            isLoading = false
            hasNetworkError = false
            if (selectedSeason == 1 && cached.episodes.isNotEmpty() && seasonEpisodes.isEmpty()) {
                seasonEpisodes = cached.episodes
            }
        } else {
            isLoading = true
        }
        try {
            val detail = MovieRepository.extractMovie(movie, forceRefresh = (reloadTrigger > 0 || (cached?.isSeries == true && cached.seasonsCount <= 1)))
            movieDetail = detail
            hasNetworkError = false
            if (selectedSeason == 1 && detail.episodes.isNotEmpty() && (seasonEpisodes.isEmpty() || seasonEpisodes.firstOrNull()?.seasonNumber != 1)) {
                seasonEpisodes = detail.episodes
            }
        } catch (_: Exception) {
            if (movieDetail == null) {
                hasNetworkError = true
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedSeason, movieDetail?.postId) {
        val detail = movieDetail ?: return@LaunchedEffect
        if (!isSeries) return@LaunchedEffect

        // If Season 1 and detail already has episodes, use them directly
        if (selectedSeason == 1 && detail.episodes.isNotEmpty()) {
            if (seasonEpisodes.isEmpty() || seasonEpisodes.firstOrNull()?.seasonNumber != 1) {
                seasonEpisodes = detail.episodes
            }
            return@LaunchedEffect
        }

        val postIdInt = detail.postId?.toIntOrNull() ?: movie.postId ?: -1
        if (postIdInt <= 0) return@LaunchedEffect

        isSeasonLoading = true
        try {
            val eps = withContext(Dispatchers.IO) {
                MovieRepository.fetchEpisodesForSeason(
                    postId = postIdInt,
                    season = selectedSeason,
                    fallbackTitle = detail.title ?: movie.title,
                    fallbackBackdrop = detail.backdrop ?: movie.backdrop ?: movie.image,
                    fallbackGallery = detail.gallery ?: emptyList()
                )
            }
            seasonEpisodes = eps
        } catch (_: Exception) {
        } finally {
            isSeasonLoading = false
        }
    }

    val totalSeasons = remember(movieDetail, seasonEpisodes) {
        val fromDetail = movieDetail?.seasonsCount ?: 1
        val fromEpisodes = seasonEpisodes.maxOfOrNull { it.seasonNumber } ?: 1
        val fromDetailEps = movieDetail?.episodes?.maxOfOrNull { it.seasonNumber } ?: 1
        val fromRepo = (movieDetail?.postId?.toIntOrNull() ?: movie.postId)?.let { MovieRepository.getSeasonsCount(it) } ?: 1
        maxOf(fromDetail, fromEpisodes, fromDetailEps, fromRepo, 1)
    }

    val backdropUrl = remember(movieDetail, movie) {
        val raw = movieDetail?.backdrop ?: movie.backdrop ?: movie.image
        raw.replace("/t/p/original/", "/t/p/w1280/")
            .replace("/t/p/w500/", "/t/p/w1280/")
            .replace("/t/p/w780/", "/t/p/w1280/")
    }

    // Dynamic ambient palette glow
    var ambientColor by remember { mutableStateOf(Color(0xFF1E2433)) }
    val animatedAmbient by animateColorAsState(
        targetValue = ambientColor,
        animationSpec = tween(800),
        label = "detailAmbient"
    )

    LaunchedEffect(backdropUrl) {
        if (backdropUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(backdropUrl)
                        .size(240, 135)
                        .allowHardware(false)
                        .target { drawable ->
                            val bitmap = (drawable as? BitmapDrawable)?.bitmap
                            if (bitmap != null && !bitmap.isRecycled) {
                                Palette.from(bitmap).generate { palette ->
                                    val vibrant = palette?.dominantSwatch?.rgb ?: palette?.vibrantSwatch?.rgb
                                    vibrant?.let {
                                        val hsl = FloatArray(3)
                                        ColorUtils.colorToHSL(it, hsl)
                                        hsl[2] = 0.50f
                                        ambientColor = Color(ColorUtils.HSLToColor(hsl))
                                    }
                                }
                            }
                        }
                        .build()
                    loader.enqueue(req)
                } catch (_: Exception) {}
            }
        }
    }

    val isSimilarLoading = isLoading && movieDetail?.similarMovies.isNullOrEmpty()
    val similar = remember(movieDetail, movie.url, isSeries, isLoading) {
        val fromDetail = movieDetail?.similarMovies ?: emptyList()
        if (fromDetail.isNotEmpty()) fromDetail
        else if (!isLoading) {
            val pool = when {
                isSeries -> (MovieRepository.localSeriesCatalog + MovieRepository.localDramaCatalog)
                movie.type == "anime" || movie.url.contains("/animes/") -> MovieRepository.localAnimeCatalog
                else -> MovieRepository.localCatalog
            }
            val activeGenres = movieDetail?.genres?.takeIf { it.isNotEmpty() } ?: movie.genres
            val matching = pool.filter { it.url != movie.url && it.genres.any { g -> activeGenres.any { ag -> ag.equals(g, ignoreCase = true) } } }
            if (matching.isNotEmpty()) matching.take(10) else pool.filter { it.url != movie.url && !it.title.equals(cleanTitle, ignoreCase = true) }.take(10)
        } else {
            emptyList()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvProDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Edge-to-edge Backdrop Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backdropUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = cleanTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Ambient glow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        animatedAmbient.copy(alpha = 0.20f),
                                        Color.Transparent
                                    ),
                                    startY = 50f
                                )
                            )
                    )

                    // Dark gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x9906080C),
                                        Color.Transparent,
                                        Color(0x8806080C),
                                        TvProDarkBg
                                    ),
                                    startY = 0f
                                )
                            )
                    )

                    // Top Bar: Back & Share
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Mira ${movie.title} en TV Pro")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Compartir"))
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. Info, Badges & Actions
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Logo or Title
                    val logoUrl = movieDetail?.logo ?: movie.logo
                    if (!logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = cleanTitle,
                            modifier = Modifier
                                .height(56.dp)
                                .fillMaxWidth(0.6f),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = cleanTitle,
                            fontFamily = BebasNeue,
                            fontSize = 34.sp,
                            color = WarmIvory,
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Metadata Badges Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // TMDB Pill
                        val rating = movieDetail?.rating ?: movie.rating ?: "8.3"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(PillSurface)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(3.5.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "TMDB",
                                    color = MutedSlate,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Inter
                                )
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = TvProYellow,
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = rating,
                                    color = WarmIvory,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Inter
                                )
                            }
                        }

                        // Year Pill
                        val year = movieDetail?.releaseDate?.take(4)
                            ?: movie.year
                            ?: Regex("\\((\\d{4})\\)").find(movieDetail?.title ?: movie.title)?.groupValues?.get(1)
                            ?: "2024"
                        Text(
                            text = year,
                            color = MutedSlate,
                            fontSize = 12.sp,
                            fontFamily = Inter
                        )

                        // Quality Pill
                        val quality = movie.quality ?: "4K Ultra HD"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF161B26))
                                .border(1.dp, WarmIvory.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = if (quality.contains("4K", ignoreCase = true)) "4K" else "HD",
                                color = WarmIvory,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter
                            )
                        }

                        // Audio Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(PillSurface)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "LAT",
                                color = SoftSilver,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter
                            )
                        }

                        // Runtime / Seasons
                        val durationText = if (isSeries) {
                            val count = totalSeasons
                            if (count > 1) "$count Temporadas" else "1 Temporada"
                        } else {
                            movieDetail?.runtime ?: movie.runtime ?: "1h 52m"
                        }
                        Text(
                            text = "• $durationText",
                            color = MutedSlate,
                            fontSize = 12.sp,
                            fontFamily = Inter
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val hasStreams = remember(movieDetail, isSeries, seasonEpisodes, movie.postId) {
                        if (movieDetail == null) true
                        else if (isSeries) seasonEpisodes.isNotEmpty() || movieDetail?.episodes?.isNotEmpty() == true || (movie.postId ?: 0) > 0
                        else movieDetail?.streams?.isNotEmpty() == true || (movie.postId ?: 0) > 0 || (movieDetail?.postId?.toIntOrNull() ?: 0) > 0
                    }

                    if (!isLoading && hasNetworkError) {
                        Surface(
                            color = Color(0xFF1E232A),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reloadTrigger++ }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Error de red al sincronizar • Toca para reintentar",
                                    color = WarmIvory,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else if (!isLoading && !hasStreams) {
                        Surface(
                            color = Color(0xFF1E232A),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Aún no disponible",
                                    color = WarmIvory,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // Play & Trailer Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val historyItem = savedHistory
                            val hasResumePosition = historyItem != null && historyItem.playbackPositionMs > 10_000L && historyItem.playbackPositionMs < (historyItem.durationMs - 60_000L)
                            val isCurrentSeasonInHistory = historyItem?.seasonNumber == selectedSeason
                            val playButtonText = when {
                                isSeries -> {
                                    if (hasResumePosition && isCurrentSeasonInHistory && historyItem?.seasonNumber != null && historyItem.episodeNumber != null) {
                                        "Continuar T${historyItem.seasonNumber}:E${historyItem.episodeNumber}"
                                    } else {
                                        "Reproducir T$selectedSeason:E1"
                                    }
                                }
                                hasResumePosition -> "Continuar viendo"
                                else -> "Reproducir"
                            }

                            Button(
                                onClick = {
                                    val targetEpisode = if (isSeries) {
                                        if (hasResumePosition && isCurrentSeasonInHistory && historyItem?.seasonNumber != null && historyItem.episodeNumber != null) {
                                            seasonEpisodes.find { it.seasonNumber == historyItem.seasonNumber && it.episodeNumber == historyItem.episodeNumber }
                                                ?: movieDetail?.episodes?.find { it.seasonNumber == historyItem.seasonNumber && it.episodeNumber == historyItem.episodeNumber }
                                                ?: seasonEpisodes.firstOrNull()
                                        } else {
                                            seasonEpisodes.firstOrNull() ?: EpisodeItem(
                                                id = 0,
                                                episodeNumber = 1,
                                                seasonNumber = selectedSeason,
                                                title = "Episodio 1",
                                                overview = "",
                                                image = backdropUrl,
                                                runtime = "45m"
                                            )
                                        }
                                    } else null
                                    onPlay(movie, targetEpisode, null)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = playButtonText,
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = playButtonText,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Inter,
                                    fontSize = 15.sp
                                )
                            }

                            // Trailer Button
                            OutlinedButton(
                                onClick = {
                                    val trailerUrl = movieDetail?.trailer?.takeIf { it.isNotBlank() }
                                        ?: "https://www.youtube.com/results?search_query=${Uri.encode(movie.title + " trailer oficial latino")}"
                                    val finalUrl = if (trailerUrl.startsWith("http")) trailerUrl else "https://www.youtube.com/watch?v=$trailerUrl"
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No se pudo abrir el trailer", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmIvory),
                                border = BorderStroke(1.dp, SubtleBorder),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = "Trailer",
                                    tint = WarmIvory,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Trailer",
                                    color = WarmIvory,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Inter,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Icons Row: Mi Lista, Calificar, Compartir, Descargar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mi Lista
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    isInMyList = WatchHistoryManager.toggleMyList(context, movie)
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isInMyList) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Mi Lista",
                                tint = if (isInMyList) WarmIvory else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isInMyList) "Guardado" else "Mi Lista",
                                color = if (isInMyList) WarmIvory else MutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter
                            )
                        }

                        // Calificar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    isLiked = !isLiked
                                    context.getSharedPreferences("tvpro_ratings", android.content.Context.MODE_PRIVATE)
                                        .edit()
                                        .putBoolean("liked_${movie.url}", isLiked)
                                        .apply()
                                    Toast.makeText(
                                        context,
                                        if (isLiked) "¡Añadido a tus recomendaciones!" else "Calificación removida",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                contentDescription = "Calificar",
                                tint = if (isLiked) TvProYellow else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isLiked) "Te gusta" else "Calificar",
                                color = if (isLiked) TvProYellow else MutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter
                            )
                        }

                        // Compartir
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Mira ${movie.title} en TV Pro")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir"))
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Compartir",
                                color = MutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter
                            )
                        }

                        // Descargar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    Toast.makeText(context, "Descargas offline próximamente en TV Pro", Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Descargar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Descargar",
                                color = MutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Overview Synopsis
                    val overview = (movieDetail?.overview?.takeIf { it.isNotBlank() } ?: movie.overview)?.cleanHtml()
                    if (!overview.isNullOrBlank()) {
                        Text(
                            text = overview,
                            color = WarmIvory.copy(alpha = 0.88f),
                            fontSize = 13.sp,
                            fontFamily = Inter,
                            lineHeight = 19.sp,
                            maxLines = if (isOverviewExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { isOverviewExpanded = !isOverviewExpanded }
                        )
                        if (!isOverviewExpanded && overview.length > 120) {
                            Text(
                                text = "Más información...",
                                color = MutedSlate,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter,
                                modifier = Modifier
                                    .clickable { isOverviewExpanded = true }
                                    .padding(top = 2.dp)
                            )
                        }
                    }

                    // Genres Row
                    val genres = movieDetail?.genres?.ifEmpty { movie.genres } ?: movie.genres
                    if (genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Géneros: " + genres.joinToString(", "),
                            color = MutedSlate,
                            fontSize = 11.5.sp,
                            fontFamily = Inter
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
            }

            // 3. Series Episodes Section
            if (isSeries) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodios",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = (-0.2).sp,
                                color = WarmIvory
                            )

                            // Seasons Dropdown / Selector Button
                            if (totalSeasons > 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PillSurface)
                                        .border(1.dp, SubtleBorder, RoundedCornerShape(6.dp))
                                        .clickable { showSeasonSheet = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Temporada $selectedSeason",
                                        color = WarmIvory,
                                        fontSize = 12.5.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Cambiar temporada",
                                        tint = SoftSilver,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isSeasonLoading) {
                    items(4) {
                        MobileEpisodeCardSkeleton()
                    }
                } else if (seasonEpisodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay episodios disponibles para la Temporada $selectedSeason",
                                color = MutedSlate,
                                fontSize = 13.sp,
                                fontFamily = Inter,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Episodes list items
                    items(seasonEpisodes) { episode ->
                        MobileEpisodeCard(
                            episode = episode,
                            seriesTitle = cleanTitle,
                            onClick = { onPlay(movie, episode, null) }
                        )
                    }
                }
            }

            // 4. Similar / Recommended Titles
            if (isSimilarLoading || similar.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "TÍTULOS SIMILARES",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.2).sp,
                        color = WarmIvory,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isSimilarLoading) {
                            items(5) {
                                MobileMovieCardSkeleton()
                            }
                        } else {
                            items(similar) { simMovie ->
                                MobileMovieCard(
                                    movie = simMovie,
                                    onClick = {
                                        onMovieClick(simMovie)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        // Bottom Sheet for Season Selection
        if (showSeasonSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSeasonSheet = false },
                containerColor = TvProDarkBg,
                scrimColor = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                dragHandle = {
                    BottomSheetDefaults.DragHandle(color = SubtleBorder)
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp)
                ) {
                    Text(
                        text = "Temporadas",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WarmIvory,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        (1..totalSeasons).forEach { sNum ->
                            val isSelected = sNum == selectedSeason
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedSeason != sNum) {
                                            selectedSeason = sNum
                                        }
                                        showSeasonSheet = false
                                    }
                                    .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                    .padding(horizontal = 20.dp, vertical = 15.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Temporada $sNum",
                                    color = if (isSelected) WarmIvory else SoftSilver,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = Inter,
                                    fontSize = 15.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionada",
                                        tint = TvProYellow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MobileEpisodeCardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ep_skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF131720),
            Color(0xFF242B3A),
            Color(0xFF131720)
        ),
        start = Offset(translateAnim - 150f, translateAnim - 150f),
        end = Offset(translateAnim + 150f, translateAnim + 150f)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(135.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmerBrush)
                .border(BorderStroke(1.dp, SubtleBorder.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

@Composable
fun MobileMovieCardSkeleton(
    modifier: Modifier = Modifier,
    cardWidth: androidx.compose.ui.unit.Dp = 135.dp,
    cardHeight: androidx.compose.ui.unit.Dp = 195.dp
) {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF131720),
            Color(0xFF242B3A),
            Color(0xFF131720)
        ),
        start = Offset(translateAnim - 150f, translateAnim - 150f),
        end = Offset(translateAnim + 150f, translateAnim + 150f)
    )

    val cardShape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier.width(cardWidth)
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(cardShape)
                .background(shimmerBrush)
                .border(BorderStroke(1.dp, SubtleBorder.copy(alpha = 0.4f)), shape = cardShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(11.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
    }
}

@Composable
fun MobileEpisodeCard(
    episode: EpisodeItem,
    seriesTitle: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localContext = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with play overlay
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(6.dp))
                    .background(TvProCardBg)
            ) {
                AsyncImage(
                    model = episode.image,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0x9906080C))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            val epCleanTitle = remember(episode.title, seriesTitle, episode.episodeNumber) {
                cleanEpisodeTitle(episode.title, seriesTitle, episode.episodeNumber)
            }
            val displayEpText = if (epCleanTitle.startsWith("Episodio", ignoreCase = true) || epCleanTitle.startsWith("Capítulo", ignoreCase = true)) {
                epCleanTitle
            } else {
                "${episode.episodeNumber}. $epCleanTitle"
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayEpText,
                    color = WarmIvory,
                    fontSize = 13.5.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.runtime,
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontFamily = Inter
                )
            }

            IconButton(onClick = {
                Toast.makeText(localContext, "Descargas de episodios próximamente en TV Pro", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Descargar",
                    tint = MutedSlate,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Episode synopsis if available
        if (episode.overview.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = episode.overview.cleanHtml(),
                color = MutedSlate,
                fontSize = 11.5.sp,
                fontFamily = Inter,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
