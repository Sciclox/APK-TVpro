@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.tvpro.mobile.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.tvpro.core.data.api.ApiClient
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.StreamItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.mobile.ui.components.cleanEpisodeTitle
import com.tvpro.mobile.ui.components.cleanHtml
import com.tvpro.mobile.ui.components.cleanTitle
import com.tvpro.mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun Context.findPlayerActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MobilePlayerScreen(
    movie: MovieItem,
    episode: EpisodeItem? = null,
    directStreamUrl: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findPlayerActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val scope = rememberCoroutineScope()

    var currentEpisode by remember { mutableStateOf(episode) }
    var seriesDetailCache by remember { mutableStateOf<MovieDetailResponse?>(MovieRepository.getCachedDetail(movie.url, movie.title)) }

    val allEpisodes = remember(seriesDetailCache) { seriesDetailCache?.episodes ?: emptyList() }
    val isSeriesContent = remember(currentEpisode, seriesDetailCache, movie, allEpisodes) {
        currentEpisode != null ||
        movie.type == "series" ||
        movie.type == "anime" ||
        movie.url.contains("/series/") ||
        movie.url.contains("/animes/") ||
        seriesDetailCache?.isSeries == true ||
        allEpisodes.isNotEmpty()
    }

    val nextEpisode = remember(currentEpisode, allEpisodes) {
        val ep = currentEpisode ?: return@remember null
        val idx = allEpisodes.indexOfFirst { it.id == ep.id || (it.seasonNumber == ep.seasonNumber && it.episodeNumber == ep.episodeNumber) }
        if (idx >= 0 && idx + 1 < allEpisodes.size) allEpisodes[idx + 1] else null
    }

    val previousEpisode = remember(currentEpisode, allEpisodes) {
        val ep = currentEpisode ?: return@remember null
        val idx = allEpisodes.indexOfFirst { it.id == ep.id || (it.seasonNumber == ep.seasonNumber && it.episodeNumber == ep.episodeNumber) }
        if (idx > 0) allEpisodes[idx - 1] else null
    }

    val totalSeasonsCount = remember(seriesDetailCache) {
        val fromDetail = seriesDetailCache?.seasonsCount ?: 1
        val fromRepo = (seriesDetailCache?.postId?.toIntOrNull() ?: movie.postId)?.let { MovieRepository.getSeasonsCount(it) } ?: 1
        maxOf(fromDetail, fromRepo).coerceAtLeast(1)
    }
    val availableSeasons = remember(totalSeasonsCount, allEpisodes) {
        if (totalSeasonsCount > 1) (1..totalSeasonsCount).toList()
        else {
            val seasons = allEpisodes.map { it.seasonNumber }.distinct().sorted()
            if (seasons.isEmpty()) listOf(1) else seasons
        }
    }
    var selectedModalSeason by remember(currentEpisode, availableSeasons) {
        mutableIntStateOf(currentEpisode?.seasonNumber ?: availableSeasons.firstOrNull() ?: 1)
    }
    var modalSeasonEpisodes by remember(movie.url) { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    var isModalSeasonLoading by remember { mutableStateOf(false) }

    val episodesInModalSeason = remember(allEpisodes, modalSeasonEpisodes, selectedModalSeason) {
        if (modalSeasonEpisodes.isNotEmpty() && modalSeasonEpisodes.firstOrNull()?.seasonNumber == selectedModalSeason) {
            modalSeasonEpisodes
        } else {
            val filtered = allEpisodes.filter { it.seasonNumber == selectedModalSeason }
            if (filtered.isNotEmpty()) filtered else modalSeasonEpisodes
        }
    }
    var showEpisodesModal by remember { mutableStateOf(false) }

    var resolvedStreamUrl by remember { mutableStateOf<String?>(directStreamUrl) }
    var streamList by remember { mutableStateOf<List<StreamItem>>(emptyList()) }
    var selectedStreamIndex by remember { mutableIntStateOf(0) }
    var isLoadingStream by remember { mutableStateOf(directStreamUrl == null) }
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resolveRetryCount by remember { mutableIntStateOf(0) }
    var resolveRetryTrigger by remember { mutableIntStateOf(0) }
    var resumeTargetPositionMs by remember { mutableLongStateOf(0L) }
    val failedStreamUrls = remember { mutableStateListOf<String>() }
    var showMovieEndedPrompt by remember { mutableStateOf(false) }
    var isFailoverInFlight by remember { mutableStateOf(false) }

    // Screen Brightness & Volume state (Interactive & Persistent)
    val initialBrightness = remember {
        try {
            val lp = activity?.window?.attributes
            if (lp != null && lp.screenBrightness in 0.01f..1f) {
                lp.screenBrightness
            } else {
                val sysB = android.provider.Settings.System.getInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    128
                )
                (sysB / 255f).coerceIn(0.01f, 1f)
            }
        } catch (e: Exception) {
            0.5f
        }
    }
    var currentBrightness by remember { mutableFloatStateOf(initialBrightness) }

    val initialVolume = remember {
        if (audioManager != null) {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
        } else 0.5f
    }
    var currentVolume by remember { mutableFloatStateOf(initialVolume) }

    var isAdjustingBrightness by remember { mutableStateOf(false) }
    var isAdjustingVolume by remember { mutableStateOf(false) }
    var activeDragType by remember { mutableStateOf<String?>(null) } // "BRIGHTNESS", "VOLUME" or null
    var accumulatedVolumeDelta by remember { mutableFloatStateOf(0f) }
    var controlsInteractionCounter by remember { mutableIntStateOf(0) }

    fun updateBrightness(frac: Float) {
        val clamped = frac.coerceIn(0.01f, 1f)
        currentBrightness = clamped
        val lp = activity?.window?.attributes
        if (lp != null) {
            lp.screenBrightness = clamped
            activity.window.attributes = lp
        }
    }

    fun updateVolume(frac: Float) {
        val clamped = frac.coerceIn(0f, 1f)
        currentVolume = clamped
        if (audioManager != null) {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (maxVol > 0) {
                val targetVolInt = kotlin.math.round(clamped * maxVol).toInt().coerceIn(0, maxVol)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolInt, 0)
                accumulatedVolumeDelta = clamped * maxVol
            }
        }
    }

    var doubleTapSide by remember { mutableStateOf<String?>(null) } // "LEFT" or "RIGHT"
    var doubleTapSeconds by remember { mutableIntStateOf(0) }
    var doubleTapTrigger by remember { mutableIntStateOf(0) }
    var displaySide by remember { mutableStateOf("RIGHT") }
    var displaySeconds by remember { mutableIntStateOf(10) }
    val currentDoubleTapSide by rememberUpdatedState(doubleTapSide)
    val currentDoubleTapSeconds by rememberUpdatedState(doubleTapSeconds)

    // Resize Mode (FIT, ZOOM, FILL)
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Dialog state for server switcher
    var showServerModal by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var showSpeedModal by remember { mutableStateOf(false) }
    var showTrackModal by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentTracks by remember { mutableStateOf<Tracks?>(null) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    // Auto-lock landscape orientation & Fullscreen Immersive Mode (Hide status bar & navigation bar)
    DisposableEffect(activity) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Lock to landscape sensor
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Modern display cutout (render behind camera notch / punch hole)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Hide Status Bar (señal, batería, hora) and Navigation Bar (botones de navegación)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity.requestedOrientation = originalOrientation
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Resolve streaming URL
    LaunchedEffect(movie.url, currentEpisode, resolveRetryTrigger) {
        if (directStreamUrl != null && currentEpisode == null && resolveRetryTrigger == 0) {
            resolvedStreamUrl = directStreamUrl
            isLoadingStream = false
            return@LaunchedEffect
        }
        isLoadingStream = true
        errorMessage = null
        try {
            val force = resolveRetryTrigger > 0
            val ep = currentEpisode
            if (ep != null) {
                val seriesPostId = movie.postId ?: seriesDetailCache?.postId?.toIntOrNull()
                val streams = MovieRepository.resolveEpisodeStreams(
                    episodeId = ep.id,
                    seriesTitle = movie.title,
                    fallbackStreams = emptyList(),
                    seasonNumber = ep.seasonNumber,
                    episodeNumber = ep.episodeNumber,
                    seriesPostId = seriesPostId
                )
                streamList = streams
                val working = streams.firstOrNull { it.url.isNotBlank() }
                if (working != null) {
                    selectedStreamIndex = streams.indexOf(working)
                    resolvedStreamUrl = working.url
                } else {
                    errorMessage = "No se encontraron enlaces de reproducción disponibles para este episodio."
                }
            } else {
                val detail = seriesDetailCache ?: MovieRepository.extractMovie(movie, forceRefresh = force)
                seriesDetailCache = detail
                if (detail.isSeries || detail.episodes.isNotEmpty() || movie.type == "series" || movie.type == "anime") {
                    val savedHistory = WatchHistoryManager.getMovieHistory(context, movie.url)
                    val targetEpisode = if (savedHistory != null && savedHistory.seasonNumber != null && savedHistory.episodeNumber != null) {
                        detail.episodes.find {
                            it.seasonNumber == savedHistory.seasonNumber && it.episodeNumber == savedHistory.episodeNumber
                        } ?: detail.episodes.firstOrNull()
                    } else {
                        detail.episodes.firstOrNull()
                    }

                    if (targetEpisode != null) {
                        // Switch currentEpisode: this naturally re-triggers LaunchedEffect with the resolved episode
                        currentEpisode = targetEpisode
                        return@LaunchedEffect
                    } else {
                        errorMessage = "No se encontraron episodios disponibles para esta serie."
                    }
                } else {
                    streamList = detail.streams
                    val working = detail.streams.firstOrNull { it.url.isNotBlank() }
                    if (working != null) {
                        selectedStreamIndex = detail.streams.indexOf(working)
                        resolvedStreamUrl = working.url
                    } else {
                        errorMessage = "No se encontraron enlaces de reproducción disponibles."
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = "Error al conectar con los servidores (${e.message ?: "Red inestable"}). Toca para reintentar."
        } finally {
            isLoadingStream = false
        }
    }

    // Ensure full series episode list is available in cache
    LaunchedEffect(movie.url) {
        if (seriesDetailCache == null || allEpisodes.isEmpty()) {
            try {
                val detail = MovieRepository.extractMovie(movie)
                seriesDetailCache = detail
            } catch (_: Exception) {}
        }
    }

    // Keep selected modal season aligned with current playing episode
    LaunchedEffect(currentEpisode) {
        val epSeason = currentEpisode?.seasonNumber
        if (epSeason != null && epSeason in availableSeasons) {
            selectedModalSeason = epSeason
        }
    }

    // Load episodes when user switches season inside player episodes modal
    LaunchedEffect(selectedModalSeason, seriesDetailCache?.postId) {
        val detail = seriesDetailCache ?: return@LaunchedEffect
        if (selectedModalSeason == 1 && allEpisodes.isNotEmpty() && modalSeasonEpisodes.isEmpty()) {
            modalSeasonEpisodes = allEpisodes
            return@LaunchedEffect
        }
        val postIdInt = detail.postId?.toIntOrNull() ?: movie.postId ?: -1
        if (postIdInt <= 0) return@LaunchedEffect
        isModalSeasonLoading = true
        try {
            val eps = MovieRepository.fetchEpisodesForSeason(
                postId = postIdInt,
                season = selectedModalSeason,
                fallbackTitle = detail.title ?: movie.title,
                fallbackBackdrop = detail.backdrop ?: movie.backdrop ?: movie.image,
                fallbackGallery = detail.gallery ?: emptyList()
            )
            modalSeasonEpisodes = eps
        } catch (_: Exception) {
        } finally {
            isModalSeasonLoading = false
        }
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(areControlsVisible, isPlaying, isAdjustingBrightness, isAdjustingVolume, showEpisodesModal, showServerModal, showSpeedModal, showTrackModal, controlsInteractionCounter) {
        if (areControlsVisible && isPlaying && !isAdjustingBrightness && !isAdjustingVolume && !showEpisodesModal && !showServerModal && !showSpeedModal && !showTrackModal) {
            delay(4000)
            areControlsVisible = false
        }
    }

    // Double tap feedback auto-hide
    LaunchedEffect(doubleTapTrigger) {
        if (doubleTapTrigger > 0 && doubleTapSide != null) {
            delay(850)
            doubleTapSide = null
            doubleTapSeconds = 0
        }
    }

    // Sync volume and brightness when controls open
    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible) {
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                currentVolume = (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
            }
            val lp = activity?.window?.attributes
            if (lp != null && lp.screenBrightness in 0.01f..1f) {
                currentBrightness = lp.screenBrightness
            }
        }
    }

    // Setup ExoPlayer
    val exoPlayer = remember(context) {
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(ApiClient.streamingOkHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(okHttpDataSourceFactory)

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    fun triggerStreamFailover(failedUrl: String, reason: String = "") {
        if (isFailoverInFlight) return
        isFailoverInFlight = true
        if (failedUrl.isNotBlank() && failedUrl !in failedStreamUrls) {
            failedStreamUrls.add(failedUrl)
        }
        val currentPos = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs)
        if (currentPos > 0L) {
            resumeTargetPositionMs = currentPos
        }

        val remaining = streamList.filter { it.url !in failedStreamUrls && it.url.isNotBlank() && it.url != failedUrl }
        if (remaining.isNotEmpty()) {
            val nextStream = remaining.first()
            selectedStreamIndex = streamList.indexOf(nextStream)
            resolvedStreamUrl = nextStream.url
            isBuffering = true
            errorMessage = null
            scope.launch {
                delay(1200)
                isFailoverInFlight = false
            }
        } else {
            val ep = currentEpisode
            val seriesPostId = movie.postId ?: seriesDetailCache?.postId?.toIntOrNull()
            if (ep != null && seriesPostId != null && seriesPostId > 0 && resolveRetryCount < 2) {
                resolveRetryCount++
                scope.launch {
                    try {
                        isBuffering = true
                        val freshStreams = MovieRepository.resolveEpisodeStreams(
                            episodeId = ep.id,
                            seriesTitle = movie.title,
                            fallbackStreams = emptyList(),
                            seasonNumber = ep.seasonNumber,
                            episodeNumber = ep.episodeNumber,
                            seriesPostId = seriesPostId
                        )
                        val freshWorking = freshStreams.filter { it.url !in failedStreamUrls && it.url.isNotBlank() }
                        if (freshWorking.isNotEmpty()) {
                            streamList = freshStreams
                            val candidate = freshWorking.first()
                            selectedStreamIndex = freshStreams.indexOf(candidate)
                            resolvedStreamUrl = candidate.url
                            errorMessage = null
                            delay(1200)
                            isFailoverInFlight = false
                            return@launch
                        }
                    } catch (_: Exception) {}
                    errorMessage = "Error de conexión con los servidores de streaming disponibles."
                    isFailoverInFlight = false
                }
            } else if (streamList.isEmpty() && resolveRetryCount < 2) {
                resolveRetryCount++
                scope.launch {
                    try {
                        isBuffering = true
                        val detail = MovieRepository.extractMovie(movie, forceRefresh = true)
                        seriesDetailCache = detail
                        val working = detail.streams.filter { it.url !in failedStreamUrls && it.url.isNotBlank() }
                        if (working.isNotEmpty()) {
                            streamList = detail.streams
                            val candidate = working.first()
                            selectedStreamIndex = detail.streams.indexOf(candidate)
                            resolvedStreamUrl = candidate.url
                            errorMessage = null
                            delay(1200)
                            isFailoverInFlight = false
                            return@launch
                        }
                    } catch (_: Exception) {}
                    errorMessage = "Error de conexión con los servidores de streaming disponibles."
                    isFailoverInFlight = false
                }
            } else {
                errorMessage = "Error de conexión con el servidor de streaming. No hay más servidores disponibles."
                isFailoverInFlight = false
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isBuffering = false
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                    if (resolveRetryCount > 0) {
                        resolveRetryCount = 0
                    }
                } else if (state == Player.STATE_ENDED) {
                    isBuffering = false
                    if (currentEpisode != null && nextEpisode != null) {
                        currentEpisode = nextEpisode
                        resolvedStreamUrl = null
                        isLoadingStream = true
                        errorMessage = null
                    } else {
                        showMovieEndedPrompt = true
                        areControlsVisible = false
                        WatchHistoryManager.resetMoviePosition(context, movie.url)
                    }
                }
            }

            override fun onPlayWhenReadyChanged(pwr: Boolean, reason: Int) {
                if (!pwr) {
                    isBuffering = false
                } else if (exoPlayer.playbackState == Player.STATE_BUFFERING) {
                    isBuffering = true
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                triggerStreamFailover(resolvedStreamUrl ?: "", "ExoPlayer: ${error.errorCodeName}")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Track playback position & buffering state sync
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
            if (exoPlayer.playbackState == Player.STATE_BUFFERING && exoPlayer.playWhenReady) {
                if (!isBuffering) isBuffering = true
            } else if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.isPlaying) {
                if (isBuffering) isBuffering = false
            }
            delay(400)
        }
    }

    // Load stream into ExoPlayer
    LaunchedEffect(resolvedStreamUrl) {
        val url = resolvedStreamUrl ?: return@LaunchedEffect
        isBuffering = true
        val uri = Uri.parse(url)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .apply {
                if (url.contains(".m3u8") || url.contains("hls")) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                } else {
                    setMimeType(MimeTypes.VIDEO_MP4)
                }
            }
            .build()

        val savedHistory = WatchHistoryManager.getMovieHistory(context, movie.url)
        val historyPos = if (savedHistory != null && savedHistory.playbackPositionMs > 10_000L && savedHistory.playbackPositionMs < (savedHistory.durationMs - 60_000L)) {
            savedHistory.playbackPositionMs
        } else 0L

        val startPos = if (resumeTargetPositionMs > 0L) {
            val p = resumeTargetPositionMs
            resumeTargetPositionMs = 0L
            p
        } else {
            historyPos
        }

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (startPos > 0L) {
            exoPlayer.seekTo(startPos)
        }
        exoPlayer.play()
    }

    // Save progress on dispose
    DisposableEffect(exoPlayer) {
        onDispose {
            val currentPos = exoPlayer.currentPosition
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (currentPos > 0L && !showMovieEndedPrompt) {
                WatchHistoryManager.saveToHistory(
                    context = context,
                    movie = movie,
                    positionMs = currentPos,
                    durationMs = dur,
                    seasonNumber = currentEpisode?.seasonNumber,
                    episodeNumber = currentEpisode?.episodeNumber,
                    isSeries = currentEpisode != null,
                    postId = movie.postId ?: seriesDetailCache?.postId?.toIntOrNull()
                )
            }
            exoPlayer.release()
        }
    }

    BackHandler {
        if (showMovieEndedPrompt) {
            onBack()
        } else if (showEpisodesModal) {
            showEpisodesModal = false
        } else if (showServerModal) {
            showServerModal = false
        } else if (showSpeedModal) {
            showSpeedModal = false
        } else if (showTrackModal) {
            showTrackModal = false
        } else {
            onBack()
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvProPureBlack)
            .pointerInput(isScreenLocked) {
                detectTapGestures(
                    onTap = { offset ->
                        if (isScreenLocked) {
                            areControlsVisible = !areControlsVisible
                            return@detectTapGestures
                        }
                        val activeSide = currentDoubleTapSide
                        if (activeSide != null) {
                            val screenWidth = size.width
                            val isRight = offset.x >= screenWidth / 2
                            val tapSide = if (isRight) "RIGHT" else "LEFT"
                            if (tapSide == activeSide) {
                                val newSeconds = currentDoubleTapSeconds + 10
                                doubleTapSeconds = newSeconds
                                displaySeconds = newSeconds
                                if (isRight) {
                                    exoPlayer.seekTo((exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration))
                                } else {
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0L))
                                }
                                currentPositionMs = exoPlayer.currentPosition
                                doubleTapTrigger++
                            } else {
                                doubleTapSide = null
                                doubleTapSeconds = 0
                                areControlsVisible = !areControlsVisible
                            }
                        } else {
                            areControlsVisible = !areControlsVisible
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isScreenLocked) return@detectTapGestures
                        val screenWidth = size.width
                        val isRight = offset.x >= screenWidth / 2
                        val side = if (isRight) "RIGHT" else "LEFT"

                        val newSeconds = if (currentDoubleTapSide == side) currentDoubleTapSeconds + 10 else 10
                        doubleTapSide = side
                        doubleTapSeconds = newSeconds
                        displaySide = side
                        displaySeconds = newSeconds

                        if (isRight) {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration))
                        } else {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0L))
                        }
                        currentPositionMs = exoPlayer.currentPosition
                        doubleTapTrigger++
                    }
                )
            }
            .pointerInput(isScreenLocked) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (isScreenLocked) {
                            activeDragType = null
                            return@detectDragGestures
                        }
                        val screenWidth = size.width
                        val leftZoneThreshold = screenWidth * 0.25f
                        val rightZoneThreshold = screenWidth * 0.75f

                        if (offset.x <= leftZoneThreshold) {
                            activeDragType = "BRIGHTNESS"
                            isAdjustingBrightness = true
                            controlsInteractionCounter++
                        } else if (offset.x >= rightZoneThreshold) {
                            activeDragType = "VOLUME"
                            isAdjustingVolume = true
                            controlsInteractionCounter++
                            if (audioManager != null) {
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                accumulatedVolumeDelta = curVol.toFloat()
                                currentVolume = (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
                            }
                        } else {
                            activeDragType = null
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (isScreenLocked) return@detectDragGestures
                        val deltaY = -dragAmount.y // Drag up is positive
                        val screenHeight = size.height.toFloat().coerceAtLeast(1f)

                        when (activeDragType) {
                            "BRIGHTNESS" -> {
                                change.consume()
                                val step = deltaY / screenHeight
                                updateBrightness((currentBrightness + step).coerceIn(0.01f, 1f))
                                controlsInteractionCounter++
                            }
                            "VOLUME" -> {
                                change.consume()
                                if (audioManager != null) {
                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    if (maxVol > 0) {
                                        val step = (deltaY / screenHeight) * maxVol
                                        accumulatedVolumeDelta = (accumulatedVolumeDelta + step).coerceIn(0f, maxVol.toFloat())
                                        val frac = accumulatedVolumeDelta / maxVol.toFloat()
                                        updateVolume(frac.coerceIn(0f, 1f))
                                        controlsInteractionCounter++
                                    }
                                }
                            }
                            else -> {
                                // Ignore drags starting outside the vertical side zones
                            }
                        }
                    },
                    onDragEnd = {
                        activeDragType = null
                        scope.launch {
                            delay(1200)
                            isAdjustingBrightness = false
                            isAdjustingVolume = false
                        }
                    },
                    onDragCancel = {
                        activeDragType = null
                        scope.launch {
                            delay(1200)
                            isAdjustingBrightness = false
                            isAdjustingVolume = false
                        }
                    }
                )
            }
    ) {
        // Player Surface with custom aspect ratio
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    this.resizeMode = resizeMode
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Modern Double-tap visual indicator overlay (YouTube/Netflix style)
        AnimatedVisibility(
            visible = doubleTapSide != null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150), initialScale = 0.88f),
            exit = fadeOut(animationSpec = tween(220)) + scaleOut(animationSpec = tween(220), targetScale = 0.92f),
            modifier = Modifier.align(if (displaySide == "RIGHT") Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            DoubleTapSeekOverlay(
                side = displaySide,
                seconds = displaySeconds,
                trigger = doubleTapTrigger
            )
        }

        // Loading state
        if (isLoadingStream) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = WarmIvory)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Conectando al servidor multimedia...",
                        color = WarmIvory,
                        fontFamily = Inter,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Mid-playback Buffering Spinner (Floating indicator when video is buffering and controls are hidden)
        AnimatedVisibility(
            visible = isBuffering && !isLoadingStream && errorMessage == null && !areControlsVisible,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200), initialScale = 0.85f),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200), targetScale = 0.85f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC06080C))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = WarmIvory,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Error message
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE06080C)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = SoftSilver,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontFamily = Inter,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                errorMessage = null
                                failedStreamUrls.clear()
                                resolveRetryCount = 0
                                resolveRetryTrigger++
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmIvory,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        if (streamList.size > 1) {
                            OutlinedButton(
                                onClick = { showServerModal = true },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, SubtleBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Servidores (${streamList.size})", color = Color.White)
                            }
                        }

                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, SubtleBorder)
                        ) {
                            Text("Regresar", color = Color.White)
                        }
                    }
                }
            }
        }

        // Persistent & Interactive Vertical Bars (Brightness Left, Volume Right)
        val isBrightnessBarVisible = ((areControlsVisible && !isScreenLocked) || isAdjustingBrightness) && !showEpisodesModal
        val isVolumeBarVisible = ((areControlsVisible && !isScreenLocked) || isAdjustingVolume) && !showEpisodesModal

        // Left Side Vertical Brightness Bar
        AnimatedVisibility(
            visible = isBrightnessBarVisible && errorMessage == null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150), initialScale = 0.90f),
            exit = fadeOut(animationSpec = tween(250)) + scaleOut(animationSpec = tween(250), targetScale = 0.90f),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            InteractiveVerticalBar(
                level = currentBrightness,
                icon = when {
                    currentBrightness < 0.35f -> Icons.Default.BrightnessLow
                    currentBrightness > 0.70f -> Icons.Default.BrightnessHigh
                    else -> Icons.Default.BrightnessMedium
                },
                accentColor = TvProYellow,
                isLeft = true,
                onLevelChange = { newLevel ->
                    updateBrightness(newLevel)
                    controlsInteractionCounter++
                },
                onInteractionStart = {
                    isAdjustingBrightness = true
                    controlsInteractionCounter++
                },
                onInteractionEnd = {
                    scope.launch {
                        delay(1200)
                        isAdjustingBrightness = false
                    }
                },
                onIconClick = {
                    val toggled = if (currentBrightness > 0.7f) 0.3f else 1.0f
                    updateBrightness(toggled)
                    controlsInteractionCounter++
                }
            )
        }

        // Right Side Vertical Volume Bar
        AnimatedVisibility(
            visible = isVolumeBarVisible && errorMessage == null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150), initialScale = 0.90f),
            exit = fadeOut(animationSpec = tween(250)) + scaleOut(animationSpec = tween(250), targetScale = 0.90f),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            InteractiveVerticalBar(
                level = currentVolume,
                icon = when {
                    currentVolume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                    currentVolume < 0.50f -> Icons.AutoMirrored.Filled.VolumeDown
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                },
                accentColor = WarmIvory,
                isLeft = false,
                onLevelChange = { newLevel ->
                    updateVolume(newLevel)
                    controlsInteractionCounter++
                },
                onInteractionStart = {
                    isAdjustingVolume = true
                    controlsInteractionCounter++
                },
                onInteractionEnd = {
                    scope.launch {
                        delay(1200)
                        isAdjustingVolume = false
                    }
                },
                onIconClick = {
                    val toggled = if (currentVolume > 0.01f) 0f else 0.5f
                    updateVolume(toggled)
                    controlsInteractionCounter++
                }
            )
        }

        // Controls HUD Overlay
        AnimatedVisibility(
            visible = areControlsVisible && !isLoadingStream && errorMessage == null && !showEpisodesModal,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (isScreenLocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                            .padding(top = 18.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .clickable {
                                isScreenLocked = false
                                areControlsVisible = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Pantalla Bloqueada",
                                tint = TvProYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Pantalla Bloqueada • Toca para desbloquear",
                                color = WarmIvory,
                                fontSize = 12.5.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x77000000))
                ) {
                    // Top Bar: Back, Title, Server Pill, Speed, Audio/Subtitles, Lock, AspectRatio, PiP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val ep = currentEpisode
                        val cleanMovieTitle = movie.title.cleanTitle()
                        val displayTitle = if (ep != null) {
                            "$cleanMovieTitle • T${ep.seasonNumber}:E${ep.episodeNumber}"
                        } else {
                            cleanMovieTitle
                        }

                        Text(
                            text = displayTitle,
                            color = WarmIvory,
                            fontFamily = Inter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Episodes selection pill (Series only)
                        if (isSeriesContent && allEpisodes.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(PillSurface)
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(50))
                                    .clickable { showEpisodesModal = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = "Episodios",
                                        tint = WarmIvory,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Episodios",
                                        color = WarmIvory,
                                        fontSize = 11.5.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Server selection pill (if multiple streams available)
                        if (streamList.size > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PillSurface)
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                                    .clickable { showServerModal = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Servidor ${selectedStreamIndex + 1} ▾",
                                    color = WarmIvory,
                                    fontSize = 11.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Playback Speed Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PillSurface)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                                .clickable { showSpeedModal = true }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${currentSpeed}x",
                                color = WarmIvory,
                                fontSize = 11.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Audio & Subtitles button
                        IconButton(
                            onClick = { showTrackModal = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Audio y Subtítulos",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Lock Screen button
                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                areControlsVisible = false
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Bloquear Pantalla",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Resize mode toggle button
                        IconButton(
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // PiP Button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = { enterPictureInPicture() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0x8806080C))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "PiP",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                // Center Controls: Prev Ep, Rewind 10, Play/Pause / Buffering, Forward 10, Next Ep
                val showEpisodeSkipButtons = isSeriesContent && allEpisodes.isNotEmpty()
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(if (showEpisodeSkipButtons) 24.dp else 42.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showEpisodeSkipButtons) {
                        IconButton(
                            onClick = {
                                val prev = previousEpisode
                                if (prev != null) {
                                    currentEpisode = prev
                                    resolvedStreamUrl = null
                                    resumeTargetPositionMs = 0L
                                    isLoadingStream = true
                                    errorMessage = null
                                }
                            },
                            enabled = previousEpisode != null,
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (previousEpisode != null) Color(0x66000000) else Color(0x22000000),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (previousEpisode != null) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Episodio Anterior",
                                tint = if (previousEpisode != null) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0L))
                            isBuffering = true
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x66000000), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Retroceder 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isBuffering) {
                                exoPlayer.pause()
                            } else if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(WarmIvory, CircleShape)
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration))
                            isBuffering = true
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x66000000), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Adelantar 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    if (showEpisodeSkipButtons) {
                        IconButton(
                            onClick = {
                                val nxt = nextEpisode
                                if (nxt != null) {
                                    currentEpisode = nxt
                                    resolvedStreamUrl = null
                                    resumeTargetPositionMs = 0L
                                    isLoadingStream = true
                                    errorMessage = null
                                }
                            },
                            enabled = nextEpisode != null,
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (nextEpisode != null) Color(0x66000000) else Color(0x22000000),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (nextEpisode != null) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Siguiente Episodio",
                                tint = if (nextEpisode != null) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Bottom Bar: Progress Bar & Timers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    val displayFraction = if (totalDurationMs > 0) {
                        if (isDraggingSlider) dragProgressFraction else (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val displayTimeMs = if (isDraggingSlider) (dragProgressFraction * totalDurationMs).toLong() else currentPositionMs

                    Slider(
                        value = displayFraction,
                        onValueChange = { frac ->
                            isDraggingSlider = true
                            dragProgressFraction = frac
                        },
                        onValueChangeFinished = {
                            val target = (dragProgressFraction * totalDurationMs).toLong()
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                            isDraggingSlider = false
                            isBuffering = true
                        },
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .background(WarmIvory, CircleShape)
                                    .border(1.5.dp, Color(0xFF16191E), CircleShape)
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(3.5.dp),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = WarmIvory,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                                ),
                                thumbTrackGapSize = 0.dp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(displayTimeMs),
                            color = WarmIvory,
                            fontSize = 12.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "-" + formatDuration((totalDurationMs - displayTimeMs).coerceAtLeast(0L)),
                            color = MutedSlate,
                            fontSize = 12.sp,
                            fontFamily = Inter
                        )
                    }
                }

                // Floating Next Episode button (shows in last 45s of episode)
                if (nextEpisode != null && totalDurationMs > 40_000L && currentPositionMs >= totalDurationMs - 45_000L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                            .padding(end = 24.dp, bottom = 64.dp)
                    ) {
                        Button(
                            onClick = {
                                currentEpisode = nextEpisode
                                resolvedStreamUrl = null
                                isLoadingStream = true
                                errorMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TvProYellow,
                                contentColor = Color(0xFF0D0F14)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Siguiente Episodio",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Movie Ended Dialog Overlay
        if (showMovieEndedPrompt) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.60f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF14171E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WarmIvory.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TvProYellow,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (currentEpisode != null) "Episodio finalizado" else "Película finalizada",
                            color = WarmIvory,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.2).sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Esperamos que hayas disfrutado la función",
                            color = SoftSilver,
                            fontFamily = Inter,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    showMovieEndedPrompt = false
                                    resumeTargetPositionMs = 0L
                                    exoPlayer.seekTo(0L)
                                    exoPlayer.play()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarmIvory,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 11.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Volver a ver",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(
                                onClick = onBack,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, SubtleBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 11.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Detalles",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cinematic Fullscreen Episodes Modal (TV Pro signature design)
        AnimatedVisibility(
            visible = showEpisodesModal && isSeriesContent && allEpisodes.isNotEmpty(),
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.96f, animationSpec = tween(250)),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            val episodesLazyListState = rememberLazyListState()

            // Auto-scroll to active episode when opened or season changes
            LaunchedEffect(showEpisodesModal, selectedModalSeason) {
                if (showEpisodesModal) {
                    val activeIndex = episodesInModalSeason.indexOfFirst {
                        it.id == currentEpisode?.id || (it.seasonNumber == currentEpisode?.seasonNumber && it.episodeNumber == currentEpisode?.episodeNumber)
                    }
                    if (activeIndex >= 0) {
                        try {
                            episodesLazyListState.scrollToItem((activeIndex - 1).coerceAtLeast(0))
                        } catch (_: Exception) {}
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFA06080C))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Intercept all touches on backdrop so they never reach the video player underneath */ }
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Header row: EPISODIOS · X TOTAL | Temporadas T1, T2... | ACTIVO badge | Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = null,
                                tint = WarmIvory,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Episodios",
                                color = WarmIvory,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "·   ${episodesInModalSeason.size} episodios en total",
                                color = MutedSlate,
                                fontFamily = Inter,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )

                            // Seasons capsules (T1, T2, etc.)
                            if (availableSeasons.size > 1) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    availableSeasons.forEach { sNum ->
                                        val isSelected = (selectedModalSeason == sNum)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(if (isSelected) WarmIvory else Color(0xFF0E121A))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) WarmIvory else SubtleBorder,
                                                    shape = RoundedCornerShape(50)
                                                )
                                                .clickable { selectedModalSeason = sNum }
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "T$sNum",
                                                color = if (isSelected) Color.Black else WarmIvory,
                                                fontFamily = Inter,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right side: Active episode pill + Close button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val activeBadgeNum = currentEpisode?.episodeNumber ?: 1
                            Row(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF10141D))
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(50))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SoftSilver)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Activo: Episodio ${String.format("%02d", activeBadgeNum)}",
                                    color = WarmIvory,
                                    fontFamily = Inter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            IconButton(
                                onClick = { showEpisodesModal = false },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .border(1.dp, SubtleBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = WarmIvory,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Selecciona un episodio para reproducirlo inmediatamente.",
                        color = MutedSlate,
                        fontFamily = Inter,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Horizontal Carousel of Episode Cards
                    LazyRow(
                        state = episodesLazyListState,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(episodesInModalSeason) { index, epItem ->
                            val isCurrent = currentEpisode?.id == epItem.id ||
                                (currentEpisode?.seasonNumber == epItem.seasonNumber && currentEpisode?.episodeNumber == epItem.episodeNumber)

                            val isWatched = (epItem.seasonNumber < (currentEpisode?.seasonNumber ?: 1)) ||
                                (epItem.seasonNumber == (currentEpisode?.seasonNumber ?: 1) && epItem.episodeNumber < (currentEpisode?.episodeNumber ?: 1))

                            val cleanEpTitle = remember(epItem.title, movie.title, epItem.episodeNumber) {
                                cleanEpisodeTitle(epItem.title, movie.title, epItem.episodeNumber)
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCurrent) Color(0xFF151922) else Color(0xFF0C0F15),
                                border = if (isCurrent) BorderStroke(1.8.dp, WarmIvory)
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .width(230.dp)
                                    .fillMaxHeight()
                                    .clickable {
                                        if (!isCurrent) {
                                            currentEpisode = epItem
                                            resolvedStreamUrl = null
                                            resumeTargetPositionMs = 0L
                                            isLoadingStream = true
                                            errorMessage = null
                                        }
                                        showEpisodesModal = false
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                ) {
                                    // 16:9 Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF080A0F))
                                    ) {
                                        AsyncImage(
                                            model = epItem.image.ifBlank { movie.backdrop ?: movie.image },
                                            contentDescription = cleanEpTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Dark gradient overlay
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        0.0f to Color.Black.copy(alpha = 0.35f),
                                                        0.5f to Color.Transparent,
                                                        1.0f to Color.Black.copy(alpha = 0.85f)
                                                    )
                                                )
                                        )

                                        // Status badge (Top-Left)
                                        if (isCurrent) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(WarmIvory)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF6B665E))
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "REPRODUCIENDO",
                                                    color = Color(0xFF0A0D13),
                                                    fontFamily = Inter,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.8.sp
                                                )
                                            }
                                        } else if (isWatched) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color(0xD90A0E14))
                                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.45f), RoundedCornerShape(50))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "VISTO",
                                                    color = Color(0xFFD1FAE5),
                                                    fontFamily = Inter,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.0.sp
                                                )
                                            }
                                        }

                                        // Duration badge (Bottom-End or Top-End)
                                        val displayDuration = if (isCurrent && exoPlayer.currentPosition > 0L) {
                                            formatDuration(exoPlayer.currentPosition)
                                        } else {
                                            epItem.runtime.ifBlank { "45 min" }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(if (isCurrent) Alignment.TopEnd else Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(0xD9080A0F))
                                                .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                        ) {
                                            Text(
                                                text = displayDuration,
                                                color = WarmIvory,
                                                fontFamily = Inter,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.6.sp
                                            )
                                        }

                                        // Bottom progress indicator on thumbnail
                                        if (isWatched) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .fillMaxWidth()
                                                    .height(2.5.dp)
                                                    .background(Color(0xFF10B981))
                                            )
                                        } else if (isCurrent) {
                                            val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                                            val dur = exoPlayer.duration.coerceAtLeast(1L)
                                            val frac = (currentPos.toFloat() / dur.toFloat()).coerceIn(0.05f, 1f)
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .background(Color.White.copy(alpha = 0.2f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(frac)
                                                        .fillMaxHeight()
                                                        .background(WarmIvory)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Episode Number and Duration/Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "EPISODIO ${String.format("%02d", epItem.episodeNumber)}",
                                            color = if (isCurrent) WarmIvory else SoftSilver,
                                            fontFamily = Inter,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.3.sp
                                        )
                                        val durMeta = if (isCurrent) {
                                            val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                                            val dur = exoPlayer.duration.coerceAtLeast(1L)
                                            val percent = ((currentPos.toFloat() / dur.toFloat()) * 100).toInt().coerceIn(1, 99)
                                            "$percent% VISTO"
                                        } else {
                                            epItem.runtime.uppercase()
                                        }
                                        Text(
                                            text = durMeta,
                                            color = MutedSlate,
                                            fontFamily = Inter,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 1.0.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Episode Title in PlusJakartaSans Bold
                                    Text(
                                        text = cleanEpTitle,
                                        color = if (isCurrent) WarmIvory else Color(0xFFD1D5DB),
                                        fontFamily = PlusJakartaSans,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (epItem.overview.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = epItem.overview.cleanHtml(),
                                            color = MutedSlate,
                                            fontFamily = Inter,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            maxLines = 8,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Server switcher Modal Bottom Sheet
        if (showServerModal) {
            ModalBottomSheet(
                onDismissRequest = { showServerModal = false },
                containerColor = TvProSurface,
                contentColor = Color.White
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Seleccionar Servidor",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = WarmIvory
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    streamList.forEachIndexed { idx, s ->
                        val isSelected = idx == selectedStreamIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentPos = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs)
                                    if (currentPos > 0L) {
                                        resumeTargetPositionMs = currentPos
                                    }
                                    errorMessage = null
                                    selectedStreamIndex = idx
                                    resolvedStreamUrl = s.url
                                    showServerModal = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Servidor ${idx + 1} (${s.source ?: "HD Fast Stream"})",
                                color = if (isSelected) WarmIvory else SoftSilver,
                                fontFamily = Inter,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = WarmIvory,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Playback Speed Modal Bottom Sheet
        if (showSpeedModal) {
            ModalBottomSheet(
                onDismissRequest = { showSpeedModal = false },
                containerColor = TvProSurface,
                contentColor = Color.White
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Velocidad de Reproducción",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = WarmIvory
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { spd ->
                        val isSelected = currentSpeed == spd
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentSpeed = spd
                                    exoPlayer.setPlaybackSpeed(spd)
                                    showSpeedModal = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (spd == 1.0f) "Normal (1.0x)" else "${spd}x",
                                color = if (isSelected) WarmIvory else SoftSilver,
                                fontFamily = Inter,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = WarmIvory,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Audio & Subtitles Modal Bottom Sheet
        if (showTrackModal) {
            ModalBottomSheet(
                onDismissRequest = { showTrackModal = false },
                containerColor = TvProSurface,
                contentColor = Color.White
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Audio y Subtítulos",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = WarmIvory
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio tracks
                    val audioGroups = currentTracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO } ?: emptyList()
                    Text(
                        text = "PISTAS DE AUDIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate,
                        fontFamily = Inter,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (audioGroups.isEmpty()) {
                        Text(
                            text = "Audio predeterminado",
                            fontSize = 13.sp,
                            color = SoftSilver,
                            fontFamily = Inter,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        audioGroups.forEach { group ->
                            val trackGroup = group.mediaTrackGroup
                            for (i in 0 until trackGroup.length) {
                                val format = trackGroup.getFormat(i)
                                val isSelected = group.isTrackSelected(i)
                                val trackLabel = format.label ?: format.language ?: "Audio ${i + 1}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setOverrideForType(
                                                    TrackSelectionOverride(trackGroup, listOf(i))
                                                )
                                                .build()
                                            showTrackModal = false
                                        }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trackLabel.replaceFirstChar { it.uppercase() },
                                        color = if (isSelected) WarmIvory else SoftSilver,
                                        fontFamily = Inter,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = WarmIvory,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subtitle tracks
                    val subtitleGroups = currentTracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT } ?: emptyList()
                    Text(
                        text = "SUBTÍTULOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate,
                        fontFamily = Inter,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Option: Subtitles Off
                    val isSubtitlesDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                    .build()
                                showTrackModal = false
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Desactivados",
                            color = if (isSubtitlesDisabled) WarmIvory else SoftSilver,
                            fontFamily = Inter,
                            fontWeight = if (isSubtitlesDisabled) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        if (isSubtitlesDisabled) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = WarmIvory,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    subtitleGroups.forEach { group ->
                        val trackGroup = group.mediaTrackGroup
                        for (i in 0 until trackGroup.length) {
                            val format = trackGroup.getFormat(i)
                            val isSelected = !isSubtitlesDisabled && group.isTrackSelected(i)
                            val trackLabel = format.label ?: format.language ?: "Subtítulo ${i + 1}"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                            .setOverrideForType(
                                                TrackSelectionOverride(trackGroup, listOf(i))
                                            )
                                            .build()
                                        showTrackModal = false
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trackLabel.replaceFirstChar { it.uppercase() },
                                    color = if (isSelected) WarmIvory else SoftSilver,
                                    fontFamily = Inter,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = WarmIvory,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DoubleTapSeekOverlay(
    side: String,
    seconds: Int,
    trigger: Int,
    modifier: Modifier = Modifier
) {
    val isForward = side == "RIGHT"

    // Infinite wave animation for 3 chevrons
    val transition = rememberInfiniteTransition(label = "double_tap_wave")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Tactile spring bounce on each tap
    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        scaleAnim.snapTo(1.15f)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
        )
    }

    // Outer half-screen curved arc
    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.46f)
            .clip(
                if (isForward) RoundedCornerShape(topStartPercent = 100, bottomStartPercent = 100)
                else RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100)
            )
            .background(
                Brush.horizontalGradient(
                    colors = if (isForward) {
                        listOf(
                            Color.Transparent,
                            Color(0x14FFFFFF),
                            Color(0x30FFFFFF)
                        )
                    } else {
                        listOf(
                            Color(0x30FFFFFF),
                            Color(0x14FFFFFF),
                            Color.Transparent
                        )
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating glass capsule badge
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0x8805080E))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 3 Cascading Chevrons Wave
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-6).dp)
                ) {
                    val chevronIndices = if (isForward) listOf(0, 1, 2) else listOf(2, 1, 0)
                    for (i in chevronIndices) {
                        val distance = (wavePhase - i).mod(3f)
                        val alpha = if (distance in 0f..1.1f) 1.0f else 0.28f
                        val chevronScale = if (distance in 0f..1.1f) 1.12f else 0.88f

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = alpha),
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = if (isForward) chevronScale else -chevronScale
                                    scaleY = chevronScale
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "${seconds} segundos",
                    color = Color.White,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
fun InteractiveVerticalBar(
    level: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isLeft: Boolean,
    onLevelChange: (Float) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
    onIconClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cleanLevel = level.coerceIn(0f, 1f)
    var trackHeightPx by remember { mutableFloatStateOf(120f) }
    var trackTopInBoxPx by remember { mutableFloatStateOf(40f) }

    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(if (isLeft) WindowInsetsSides.Start else WindowInsetsSides.End))
            .padding(start = if (isLeft) 20.dp else 0.dp, end = if (!isLeft) 20.dp else 0.dp)
            .width(46.dp)
            .height(175.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(Color(0xD906080C))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(23.dp))
            .pointerInput(trackHeightPx, trackTopInBoxPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onInteractionStart()

                    fun calculateFraction(posY: Float): Float {
                        val safeHeight = trackHeightPx.coerceAtLeast(1f)
                        val relY = (posY - trackTopInBoxPx).coerceIn(0f, safeHeight)
                        return (1f - (relY / safeHeight)).coerceIn(0f, 1f)
                    }

                    onLevelChange(calculateFraction(down.position.y))

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        change.consume()
                        onLevelChange(calculateFraction(change.position.y))
                    }
                    onInteractionEnd()
                }
            }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon button at the top
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(enabled = onIconClick != null) {
                        onIconClick?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Vertical Track and Level Fill
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        trackHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                        trackTopInBoxPx = coordinates.positionInParent().y
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(cleanLevel)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                )
            }

            // Percentage text at the bottom
            Text(
                text = "${(cleanLevel * 100).roundToInt()}%",
                color = Color.White,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
