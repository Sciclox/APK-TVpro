package com.tvpro.app.ui.screens

import android.app.Activity
import android.net.Uri
import com.tvpro.app.PlayerKeyDispatcher
import androidx.activity.compose.BackHandler
import android.view.ViewGroup
import android.view.WindowManager
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.tvpro.app.ui.components.cleanHtml
import com.tvpro.app.ui.components.cleanTitle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.api.ApiClient
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.StreamItem
import com.tvpro.core.data.model.SubtitleTrackData
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ExoAudioTrackItem(
    val group: Tracks.Group,
    val trackIndex: Int,
    val name: String,
    val language: String,
    val isSelected: Boolean,
    val isSpanish: Boolean,
    val isEnglish: Boolean = false
)

data class ExoSubtitleTrackItem(
    val group: Tracks.Group,
    val trackIndex: Int,
    val name: String,
    val language: String,
    val isSelected: Boolean,
    val isSpanish: Boolean,
    val isEnglish: Boolean = false
)

data class DisplayLanguageOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val isSelected: Boolean,
    val onSelect: () -> Unit
)


@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    movie: MovieItem,
    streamUrl: String? = null,
    streams: List<StreamItem> = emptyList(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var currentMovieTitle by remember(movie.title) { mutableStateOf(movie.title) }
    var dynamicStreams by remember(movie.url) { mutableStateOf<List<StreamItem>>(emptyList()) }
    val failedStreamUrls = remember(movie.url) { mutableStateListOf<String>() }
    var detailResponse by remember(movie.url) { mutableStateOf<MovieDetailResponse?>(null) }
    var seasonEpisodes by remember(movie.url) { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    var isSeasonLoading by remember { mutableStateOf(false) }

    val (cleanMainTitle, activeSequenceLabel, currentSeasonNum, currentEpNum) = remember(currentMovieTitle) {
        val raw = currentMovieTitle.trim()
        val epRegex = Regex("""^(.*?)\s*-\s*T(\d+):E(\d+).*$""", RegexOption.IGNORE_CASE)
        val match = epRegex.find(raw)
        if (match != null) {
            val seriesName = match.groupValues[1].cleanTitle().ifBlank { raw.cleanTitle() }
            val sNum = match.groupValues[2].toIntOrNull() ?: 1
            val eNum = match.groupValues[3].toIntOrNull() ?: 1
            listOf(seriesName.uppercase(), "TEMPORADA $sNum : EPISODIO $eNum", sNum.toString(), eNum.toString())
        } else {
            val tempEpRegex = Regex("""^(.*?)(?:\s*[:\-·]\s*)?Temporada\s*(\d+)\s*[:\-·]?\s*Episodio\s*(\d+).*$""", RegexOption.IGNORE_CASE)
            val match2 = tempEpRegex.find(raw)
            if (match2 != null) {
                val seriesName = match2.groupValues[1].cleanTitle().ifBlank { raw.cleanTitle() }
                val sNum = match2.groupValues[2].toIntOrNull() ?: 1
                val eNum = match2.groupValues[3].toIntOrNull() ?: 1
                listOf(seriesName.uppercase(), "TEMPORADA $sNum : EPISODIO $eNum", sNum.toString(), eNum.toString())
            } else {
                val cleanedRaw = raw.cleanTitle()
                listOf(cleanedRaw.uppercase(), cleanedRaw.uppercase(), "1", "1")
            }
        }
    }
    val activeSeasonInt = currentSeasonNum.toIntOrNull() ?: 1
    val activeEpisodeInt = currentEpNum.toIntOrNull() ?: 1
    var selectedModalSeason by remember(activeSeasonInt) { mutableIntStateOf(activeSeasonInt) }

    val isSeriesContent = remember(detailResponse, movie, currentMovieTitle) {
        val initIsSeries = movie.type == "series" || movie.type == "anime" || movie.type == "drama" ||
            movie.url.contains("/tvshows/") || movie.url.contains("/series/") || movie.url.contains("/animes/") || movie.url.contains("/novelas/") ||
            currentMovieTitle.contains(Regex("""- T\d+:E\d+""", RegexOption.IGNORE_CASE))
        detailResponse?.isSeries ?: initIsSeries
    }

    val hasEpisodesButton = remember(isSeriesContent, seasonEpisodes, detailResponse) {
        if (!isSeriesContent) {
            false
        } else {
            val totalSeasons = maxOf(
                detailResponse?.seasonsCount ?: 1,
                detailResponse?.postId?.toIntOrNull()?.let { MovieRepository.getSeasonsCount(it) } ?: 1,
                seasonEpisodes.maxOfOrNull { it.seasonNumber } ?: 1
            )
            !(detailResponse != null && totalSeasons <= 1 && seasonEpisodes.isNotEmpty() && seasonEpisodes.size <= 1)
        }
    }

    val nextEpisode: EpisodeItem? = remember(isSeriesContent, seasonEpisodes, activeSeasonInt, activeEpisodeInt, detailResponse, cleanMainTitle) {
        if (!isSeriesContent) null
        else {
            val totalSeasons = maxOf(
                (detailResponse?.seasonsCount ?: 1),
                detailResponse?.postId?.toIntOrNull()?.let { MovieRepository.getSeasonsCount(it) } ?: 1,
                seasonEpisodes.maxOfOrNull { it.seasonNumber } ?: 1
            ).coerceAtLeast(1)
            // 1. Buscar siguiente episodio en la misma temporada
            val nextInSeason = seasonEpisodes.firstOrNull { it.seasonNumber == activeSeasonInt && it.episodeNumber == activeEpisodeInt + 1 }
            if (nextInSeason != null) {
                nextInSeason
            } else {
                // 2. Si la lista actual de episodios está secuenciada
                val currentIdx = seasonEpisodes.indexOfFirst { it.seasonNumber == activeSeasonInt && it.episodeNumber == activeEpisodeInt }
                if (currentIdx != -1 && currentIdx + 1 < seasonEpisodes.size) {
                    seasonEpisodes[currentIdx + 1]
                } else if (seasonEpisodes.isEmpty()) {
                    val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image
                    EpisodeItem(
                        id = -(activeSeasonInt * 100 + activeEpisodeInt + 1),
                        episodeNumber = activeEpisodeInt + 1,
                        seasonNumber = activeSeasonInt,
                        title = "Episodio ${activeEpisodeInt + 1}",
                        overview = "Episodio ${activeEpisodeInt + 1} de la Temporada $activeSeasonInt de $cleanMainTitle.",
                        image = defaultBackdrop,
                        runtime = "45 min"
                    )
                } else if (activeSeasonInt < totalSeasons) {
                    val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image
                    EpisodeItem(
                        id = -((activeSeasonInt + 1) * 100 + 1),
                        episodeNumber = 1,
                        seasonNumber = activeSeasonInt + 1,
                        title = "Episodio 1",
                        overview = "Episodio 1 de la Temporada ${activeSeasonInt + 1} de $cleanMainTitle.",
                        image = defaultBackdrop,
                        runtime = "45 min"
                    )
                } else null
            }
        }
    }

    val previousEpisode: EpisodeItem? = remember(isSeriesContent, seasonEpisodes, activeSeasonInt, activeEpisodeInt, detailResponse, cleanMainTitle) {
        if (!isSeriesContent) null
        else {
            // 1. Buscar episodio anterior en la misma temporada
            val prevInSeason = seasonEpisodes.firstOrNull { it.seasonNumber == activeSeasonInt && it.episodeNumber == activeEpisodeInt - 1 }
            if (prevInSeason != null) {
                prevInSeason
            } else {
                // 2. Si la lista actual de episodios está secuenciada
                val currentIdx = seasonEpisodes.indexOfFirst { it.seasonNumber == activeSeasonInt && it.episodeNumber == activeEpisodeInt }
                if (currentIdx > 0 && currentIdx - 1 < seasonEpisodes.size) {
                    seasonEpisodes[currentIdx - 1]
                } else if (activeEpisodeInt > 1) {
                    val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image
                    EpisodeItem(
                        id = -(activeSeasonInt * 100 + activeEpisodeInt - 1),
                        episodeNumber = activeEpisodeInt - 1,
                        seasonNumber = activeSeasonInt,
                        title = "Episodio ${activeEpisodeInt - 1}",
                        overview = "Episodio ${activeEpisodeInt - 1} de la Temporada $activeSeasonInt de $cleanMainTitle.",
                        image = defaultBackdrop,
                        runtime = "45 min"
                    )
                } else if (activeSeasonInt > 1) {
                    val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image
                    EpisodeItem(
                        id = -((activeSeasonInt - 1) * 100 + 1),
                        episodeNumber = 1,
                        seasonNumber = activeSeasonInt - 1,
                        title = "Episodio 1",
                        overview = "Episodio 1 de la Temporada ${activeSeasonInt - 1} de $cleanMainTitle.",
                        image = defaultBackdrop,
                        runtime = "45 min"
                    )
                } else null
            }
        }
    }

    var showNextEpisodePrompt by remember { mutableStateOf(false) }
    var isNextEpisodeDismissed by remember(currentMovieTitle) { mutableStateOf(false) }
    var nextEpisodeCountdownSeconds by remember(currentMovieTitle) { mutableIntStateOf(10) }
    var consecutiveAutoPlayCount by remember { mutableIntStateOf(0) }
    var nextEpisodeSelectedButton by remember(currentMovieTitle, showNextEpisodePrompt) { mutableIntStateOf(0) }
    val nextEpisodeFocusRequester = remember { FocusRequester() }
    val nextEpisodeCancelFocusRequester = remember { FocusRequester() }

    var showMovieEndedPrompt by remember { mutableStateOf(false) }
    var movieEndedSelectedButton by remember(showMovieEndedPrompt) { mutableIntStateOf(1) }

    LaunchedEffect(movie.url, streams, streamUrl) {
        try {
            if (streams.isNotEmpty()) {
                val verified = withContext(Dispatchers.IO) {
                    MovieRepository.filterWorkingStreams(streams)
                }
                if (verified.isNotEmpty()) {
                    dynamicStreams = verified
                }
            }
            val extracted = withContext(Dispatchers.IO) {
                MovieRepository.extractMovie(movie)
            }
            detailResponse = extracted
            if (extracted.episodes.isNotEmpty()) {
                seasonEpisodes = extracted.episodes
            }
            if (streams.isEmpty() && extracted.streams.isNotEmpty()) {
                dynamicStreams = extracted.streams
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(selectedModalSeason, detailResponse?.postId) {
        val res = detailResponse ?: return@LaunchedEffect
        if (!res.isSeries) return@LaunchedEffect
        if (selectedModalSeason == 1 && res.episodes.isNotEmpty() && seasonEpisodes.isNotEmpty() && seasonEpisodes.firstOrNull()?.seasonNumber == 1) return@LaunchedEffect

        isSeasonLoading = true
        try {
            val postIdInt = res.postId?.toIntOrNull() ?: -1
            val eps = withContext(Dispatchers.IO) {
                MovieRepository.fetchEpisodesForSeason(
                    postId = postIdInt,
                    season = selectedModalSeason,
                    fallbackTitle = cleanMainTitle,
                    fallbackBackdrop = res.backdrop ?: movie.backdrop ?: movie.image,
                    fallbackGallery = res.gallery ?: emptyList()
                )
            }
            if (eps.isNotEmpty()) {
                seasonEpisodes = eps
            }
        } catch (_: Exception) {
        } finally {
            isSeasonLoading = false
        }
    }

    // Prepare streams list with verified working servers and automatic hiding of failed servers
    val availableStreams = remember(streams, streamUrl, movie.url, dynamicStreams, failedStreamUrls.size) {
        val baseList = when {
            dynamicStreams.isNotEmpty() -> dynamicStreams
            streams.isNotEmpty() -> streams
            !streamUrl.isNullOrBlank() -> listOf(StreamItem(url = streamUrl, type = "EMBED", source = "Servidor 1: Principal HD"))
            else -> MovieRepository.getStreamsForMovie(movie.url)
        }
        val aliveList = baseList
            .filter { it.url.isNotBlank() && it.url !in failedStreamUrls }
            .distinctBy { it.url }
            .sortedWith(
                compareBy<StreamItem> { stream ->
                    val u = stream.url.lowercase()
                    when {
                        stream.type == "HLS" || stream.type == "MP4" || u.contains(".m3u8") || u.contains(".mp4") -> 0
                        u.contains("voe") || u.contains("megankid") -> 1
                        u.contains("vimeos") -> 2
                        u.contains("goodstream") -> 3
                        else -> 4
                    }
                }
            )
            .mapIndexed { idx, item ->
                val rawLabel = item.source ?: "Servidor HD"
                val cleanLabel = rawLabel.replace(Regex("^Servidor\\s*\\d+\\s*:\\s*", RegexOption.IGNORE_CASE), "")
                item.copy(source = "Servidor ${idx + 1}: $cleanLabel")
            }
            .toMutableList()

        if (aliveList.isEmpty()) {
            val fallbackCandidates = baseList.filter { it.url.isNotBlank() }
            if (fallbackCandidates.isNotEmpty()) {
                aliveList.addAll(fallbackCandidates)
            }
        }
        aliveList
    }

    val initialStreamUrl = remember(movie.url) {
        if (!streamUrl.isNullOrBlank()) streamUrl
        else streams.firstOrNull { it.url.isNotBlank() }?.url ?: ""
    }
    var activeStreamUrl by remember(movie.url) { mutableStateOf(initialStreamUrl) }

    // If activeStreamUrl is initially blank, bind to the first alive stream as soon as availableStreams is populated
    LaunchedEffect(availableStreams) {
        if (activeStreamUrl.isBlank() && availableStreams.isNotEmpty()) {
            activeStreamUrl = availableStreams.first().url
        }
    }

    val currentStreamIndex = remember(availableStreams, activeStreamUrl) {
        val found = availableStreams.indexOfFirst { it.url == activeStreamUrl }
        if (found >= 0) found else 0
    }
    val currentStream: StreamItem = remember(availableStreams, activeStreamUrl) {
        availableStreams.find { it.url == activeStreamUrl }
            ?: availableStreams.firstOrNull()
            ?: StreamItem(url = activeStreamUrl, type = "EMBED", source = "Servidor HD")
    }

    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var loadedStreamUrl by remember { mutableStateOf<String?>(null) }

    var isPlaying by remember { mutableStateOf(true) }

    // Keep TV Screen On only during active playback to prevent OLED panel burn-in and allow TV screensaver
    DisposableEffect(isPlaying) {
        if (isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Clean up WebView and avoid memory leaks when leaving Player
    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    onPause()
                    removeAllViews()
                    destroy()
                }
            } catch (_: Exception) {}
            webViewRef = null
        }
    }
    var isBuffering by remember { mutableStateOf(true) }
    var isVideoReady by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showEpisodesDialog by remember { mutableStateOf(false) }
    var subtitlesEnabled by remember { mutableStateOf(false) }
    var selectedSubtitleOption by remember { mutableIntStateOf(0) } // 0 = Desactivados, 1 = Español, 2 = Inglés (CC)
    var nativeWebAudioTracks by remember(activeStreamUrl) { mutableStateOf<List<String>>(emptyList()) }
    var selectedWebAudioTrackIdx by remember(activeStreamUrl) { mutableIntStateOf(0) }
    var exoAudioTracks by remember(activeStreamUrl) { mutableStateOf<List<ExoAudioTrackItem>>(emptyList()) }
    var exoSubtitleTracks by remember(activeStreamUrl) { mutableStateOf<List<ExoSubtitleTrackItem>>(emptyList()) }
    var nativeWebSubtitleTracks by remember(activeStreamUrl) { mutableStateOf<List<String>>(emptyList()) }
    var selectedWebSubtitleTrackIdx by remember(activeStreamUrl) { mutableIntStateOf(0) }
    var userSelectedAudioTrack by remember(activeStreamUrl) { mutableStateOf(false) }
    var seekToastText by remember { mutableStateOf<String?>(null) }
    var loadingSubtitle by remember { mutableStateOf("Preparando reproducción en alta definición") }
    var isEpisodeChanging by remember { mutableStateOf(false) }
    var isFailoverInFlight by remember { mutableStateOf(false) }
    var isResolvingFailover by remember { mutableStateOf(false) }

    // Watch History & Auto-Resume State (Netflix-style auto-resume)
    val savedHistory = remember(movie.url) { WatchHistoryManager.getMovieHistory(context, movie.url) }
    val initialResumePositionMs = remember(savedHistory) {
        if (savedHistory != null &&
            savedHistory.playbackPositionMs > 15_000L &&
            (savedHistory.durationMs <= 0L || savedHistory.playbackPositionMs < savedHistory.durationMs - 30_000L)
        ) {
            savedHistory.playbackPositionMs
        } else {
            0L
        }
    }

    var currentPositionMs by remember(movie.url) { mutableLongStateOf(initialResumePositionMs) }
    var durationMs by remember { mutableLongStateOf(savedHistory?.durationMs ?: 0L) }
    var pendingResumePositionMs by remember(movie.url) {
        mutableLongStateOf(if (initialResumePositionMs > 0L) initialResumePositionMs else -1L)
    }

    val playButtonFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val ffFocusRequester = remember { FocusRequester() }
    val prevEpisodeDockFocusRequester = remember { FocusRequester() }
    val nextEpisodeDockFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }
    val serverPillFocusRequester = remember { FocusRequester() }
    val restartButtonFocusRequester = remember { FocusRequester() }
    val episodesButtonFocusRequester = remember { FocusRequester() }
    val backButtonFocusRequester = remember { FocusRequester() }
    val activeServerFocusRequester = remember { FocusRequester() }
    val activeEpisodeFocusRequester = remember { FocusRequester() }
    val activeAudioTrackFocusRequester = remember { FocusRequester() }
    val movieEndedReplayFocusRequester = remember { FocusRequester() }
    val movieEndedBackFocusRequester = remember { FocusRequester() }
    var isMovieEndedReplayFocused by remember { mutableStateOf(false) }
    var isMovieEndedBackFocused by remember { mutableStateOf(false) }

    BackHandler {
        if (showMovieEndedPrompt) {
            showMovieEndedPrompt = false
            onBackClick()
        } else if (showEpisodesDialog) {
            showEpisodesDialog = false
        } else if (showServerDialog) {
            showServerDialog = false
        } else {
            onBackClick()
        }
    }

    val trackSelector = remember(context) {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setPreferredAudioLanguages("es", "es-419", "spa", "es-MX", "es-US", "es-ES", "es-CO", "es-AR")
                .setPreferredTextLanguages("es", "es-419", "spa")
                .setSelectUndeterminedTextLanguage(false)
                .build()
        }
    }

    // Build ExoPlayer with robust networking and TV-optimized buffer load control
    val exoPlayer = remember(context) {
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(ApiClient.streamingOkHttpClient)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(okHttpDataSourceFactory)

        // TV-optimized buffer: 15s min / 50s max buffer prevents stutter on typical 2.4GHz Wi-Fi
        val tvLoadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(tvLoadControl)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build().apply {
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setPreferredAudioLanguages("es", "es-419", "spa", "es-MX", "es-US", "es-ES", "es-CO", "es-AR")
                    .setPreferredTextLanguages("es", "es-419", "spa")
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .setSelectUndeterminedTextLanguage(false)
                    .build()
            }
    }

    val isEmbedStream = remember(activeStreamUrl, currentStream) {
        if (currentStream.url.isBlank()) {
            false
        } else if (currentStream.type == "HLS" || currentStream.type == "MP4" || activeStreamUrl.contains(".m3u8") || activeStreamUrl.contains(".mp4")) {
            false
        } else {
            currentStream.type == "EMBED" ||
            activeStreamUrl.contains("vidsrc") ||
            activeStreamUrl.contains("multiembed") ||
            activeStreamUrl.contains("smashystream") ||
            activeStreamUrl.contains("vimeos") ||
            activeStreamUrl.contains("goodstream") ||
            activeStreamUrl.contains("voe") ||
            activeStreamUrl.contains("dood") ||
            activeStreamUrl.contains("filemoon") ||
            activeStreamUrl.contains("hlswish") ||
            activeStreamUrl.contains("videoapp") ||
            activeStreamUrl.contains("lamovie") ||
            activeStreamUrl.contains("embed")
        }
    }

    // Function to safely switch server, purging prior errors and ExoPlayer instances cleanly
    fun switchServer(newIndex: Int, resetPosition: Boolean = false, streamList: List<StreamItem>? = null, isUserAction: Boolean = false) {
        val streamsToUse = streamList ?: availableStreams
        if (streamsToUse.isEmpty()) return
        val targetIndex = newIndex.coerceIn(0, streamsToUse.size - 1)
        val targetStream = streamsToUse[targetIndex]
        activeStreamUrl = targetStream.url
        isBuffering = true
        if (!isVideoReady) {
            isVideoReady = false
        }
        userSelectedAudioTrack = false
        exoAudioTracks = emptyList()
        exoSubtitleTracks = emptyList()
        nativeWebSubtitleTracks = emptyList()
        loadingSubtitle = "Cargando contenido..."
        if (isUserAction) {
            seekToastText = "Cargando ${targetStream.source}..."
        } else {
            seekToastText = null
        }

        val isTargetEmbed = if (targetStream.type == "HLS" || targetStream.type == "MP4" || targetStream.url.contains(".m3u8") || targetStream.url.contains(".mp4")) {
            false
        } else {
            targetStream.type == "EMBED" ||
            targetStream.url.contains("vidsrc") ||
            targetStream.url.contains("multiembed") ||
            targetStream.url.contains("smashystream") ||
            targetStream.url.contains("vimeos") ||
            targetStream.url.contains("goodstream") ||
            targetStream.url.contains("voe") ||
            targetStream.url.contains("dood") ||
            targetStream.url.contains("filemoon") ||
            targetStream.url.contains("hlswish") ||
            targetStream.url.contains("videoapp") ||
            targetStream.url.contains("lamovie") ||
            targetStream.url.contains("embed")
        }

        try {
            if (!isTargetEmbed) {
                // Target is ExoPlayer stream (HLS / MP4)
                try {
                    webViewRef?.apply {
                        stopLoading()
                        loadUrl("about:blank")
                        onPause()
                    }
                } catch (_: Exception) {}
                loadedStreamUrl = null

                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                if (!userSelectedAudioTrack) {
                    try {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setPreferredAudioLanguages("es", "es-419", "spa", "es-MX", "es-US", "es-ES", "es-CO", "es-AR")
                            .setPreferredTextLanguages("es", "es-419", "spa")
                            .setSelectUndeterminedTextLanguage(false)
                            .build()
                    } catch (_: Exception) {}
                }
                if (!subtitlesEnabled) {
                    try {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    } catch (_: Exception) {}
                }
                val startPos = if (resetPosition) 0L else currentPositionMs.coerceAtLeast(0L)
                val uri = Uri.parse(targetStream.url)
                val subConfigs = if (subtitlesEnabled) {
                    targetStream.safeSubtitles
                        .filter { !it.url.contains("vimeos", ignoreCase = true) && it.url.startsWith("http") }
                        .mapNotNull { sub ->
                            try {
                                val mime = if (sub.url.contains(".srt", ignoreCase = true)) {
                                    MimeTypes.APPLICATION_SUBRIP
                                } else {
                                    MimeTypes.TEXT_VTT
                                }
                                val flags = if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0
                                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                                    .setMimeType(mime)
                                    .setLanguage(sub.language)
                                    .setLabel(sub.label)
                                    .setSelectionFlags(flags)
                                    .build()
                            } catch (_: Exception) {
                                null
                            }
                        }
                } else {
                    emptyList()
                }
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .apply {
                        if (targetStream.type == "HLS" || targetStream.url.contains(".m3u8")) {
                            setMimeType(MimeTypes.APPLICATION_M3U8)
                        } else if (targetStream.type == "MP4" || targetStream.url.contains(".mp4")) {
                            setMimeType(MimeTypes.VIDEO_MP4)
                        }
                        if (subConfigs.isNotEmpty()) {
                            setSubtitleConfigurations(subConfigs)
                        }
                    }
                    .build()
                exoPlayer.setMediaItem(mediaItem, startPos)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                isPlaying = true
            } else {
                // Target is Web Embed
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.playWhenReady = false

                if (!resetPosition && currentPositionMs > 1_000L) {
                    pendingResumePositionMs = currentPositionMs
                } else if (resetPosition) {
                    currentPositionMs = 0L
                    pendingResumePositionMs = -1L
                }

                loadedStreamUrl = targetStream.url
                try {
                    webViewRef?.apply {
                        stopLoading()
                        loadUrl("about:blank")
                        onResume()
                        if (targetStream.url.contains("vimeos", ignoreCase = true) || targetStream.url.contains("lamovie", ignoreCase = true)) {
                            val iframeHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <style>
                                        body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #000; }
                                        iframe { width: 100%; height: 100%; border: none; }
                                    </style>
                                </head>
                                <body>
                                    <iframe src="${targetStream.url}" allowfullscreen="true" allow="autoplay; fullscreen; encrypted-media"></iframe>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://lamovie.org/", iframeHtml, "text/html", "UTF-8", null)
                        } else {
                            val headers = mapOf(
                                "Referer" to "https://lamovie.org/",
                                "Origin" to "https://lamovie.org"
                            )
                            loadUrl(targetStream.url, headers)
                        }
                    }
                } catch (_: Exception) {}
                isPlaying = true

            }
        } catch (e: Exception) {
            e.printStackTrace()
            seekToastText = "Error al conectar al servidor"
        }
    }

    fun triggerStreamFailover(failedUrl: String, reason: String) {
        if (isFailoverInFlight) return
        isFailoverInFlight = true
        if (failedUrl.isNotBlank() && failedUrl !in failedStreamUrls) {
            failedStreamUrls.add(failedUrl)
        }

        val remaining = availableStreams.filter { it.url !in failedStreamUrls && it.url.isNotBlank() && it.url != failedUrl }
        if (remaining.isNotEmpty()) {
            isBuffering = true
            seekToastText = null
            switchServer(0, resetPosition = false, streamList = remaining, isUserAction = false)
            scope.launch {
                delay(1000)
                isFailoverInFlight = false
            }
        } else {
            val postIdInt = detailResponse?.postId?.toIntOrNull() ?: movie.postId ?: -1
            if (!isResolvingFailover && isSeriesContent && postIdInt > 0) {
                isResolvingFailover = true
                isBuffering = true
                scope.launch {
                    try {
                        val fresh = withContext(Dispatchers.IO) {
                            MovieRepository.resolveEpisodeStreams(
                                episodeId = 0,
                                seriesTitle = cleanMainTitle,
                                fallbackStreams = emptyList(),
                                seasonNumber = activeSeasonInt,
                                episodeNumber = activeEpisodeInt,
                                seriesPostId = postIdInt
                            )
                        }
                        val newCandidates = fresh.filter { it.url !in failedStreamUrls && it.url.isNotBlank() }
                        if (newCandidates.isNotEmpty()) {
                            dynamicStreams = (dynamicStreams + newCandidates).distinctBy { it.url }
                            switchServer(0, resetPosition = false, streamList = newCandidates, isUserAction = false)
                        } else {
                            seekToastText = "Este contenido no se encuentra disponible temporalmente."
                            showServerDialog = true
                            isBuffering = false
                            isVideoReady = true
                        }
                    } catch (_: Exception) {
                        seekToastText = "Error de conexión. Selecciona otro servidor."
                        showServerDialog = true
                        isBuffering = false
                        isVideoReady = true
                    } finally {
                        isResolvingFailover = false
                        delay(2000)
                        isFailoverInFlight = false
                    }
                }
            } else if (!isResolvingFailover && !isSeriesContent && (postIdInt > 0 || movie.url.isNotBlank())) {
                isResolvingFailover = true
                isBuffering = true
                scope.launch {
                    try {
                        val fresh = withContext(Dispatchers.IO) {
                            MovieRepository.resolveLaMovieStreams(movie.url)
                        }
                        val newCandidates = fresh.filter { it.url !in failedStreamUrls && it.url.isNotBlank() }
                        if (newCandidates.isNotEmpty()) {
                            dynamicStreams = (dynamicStreams + newCandidates).distinctBy { it.url }
                            switchServer(0, resetPosition = false, streamList = newCandidates, isUserAction = false)
                        } else {
                            seekToastText = "Este video no se encuentra disponible temporalmente."
                            showServerDialog = true
                            isBuffering = false
                            isVideoReady = true
                        }
                    } catch (_: Exception) {
                        seekToastText = "Error de conexión. Intenta más tarde."
                        showServerDialog = true
                        isBuffering = false
                        isVideoReady = true
                    } finally {
                        isResolvingFailover = false
                        delay(2000)
                        isFailoverInFlight = false
                    }
                }
            } else {
                seekToastText = "Servidor no disponible. Selecciona otro servidor."
                showServerDialog = true
                isBuffering = false
                isVideoReady = true
                scope.launch {
                    delay(2000)
                    isFailoverInFlight = false
                }
            }
        }
    }

    // Unified Play/Pause & Seek Functions for both ExoPlayer and Embedded Native Web Player
    fun setPlaybackPlaying(play: Boolean) {
        isPlaying = play
        if (isEmbedStream) {
            val js = """
                (function() {
                    var v = document.querySelector('video');
                    if (!v) {
                        var iframes = document.querySelectorAll('iframe');
                        for (var i = 0; i < iframes.length; i++) {
                            try {
                                var iv = iframes[i].contentDocument && iframes[i].contentDocument.querySelector('video');
                                if (iv) { v = iv; break; }
                            } catch(e) {}
                        }
                    }
                    if (v) {
                        v.controls = false;
                        v.removeAttribute('controls');
                        if ($play) v.play(); else v.pause();
                    }
                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                        try {
                            var p = window.jwplayer();
                            if (p) {
                                if ($play && p.play) p.play(true);
                                else if (!$play && p.pause) p.pause(true);
                                if (typeof p.setControls === 'function') p.setControls(false);
                            }
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
        } else {
            if (play) exoPlayer.play() else exoPlayer.pause()
        }
    }

    fun togglePlayPause() {
        val nextPlay = !isPlaying
        setPlaybackPlaying(nextPlay)
    }

    fun seekToMs(targetMs: Long) {
        val clampedMs = if (durationMs > 0L) targetMs.coerceIn(0L, durationMs) else targetMs.coerceAtLeast(0L)
        currentPositionMs = clampedMs
        if (isEmbedStream) {
            val targetSec = clampedMs / 1000.0
            val js = """
                (function() {
                    var v = document.querySelector('video');
                    if (!v) {
                        var iframes = document.querySelectorAll('iframe');
                        for (var i = 0; i < iframes.length; i++) {
                            try {
                                var iv = iframes[i].contentDocument && iframes[i].contentDocument.querySelector('video');
                                if (iv) { v = iv; break; }
                            } catch(e) {}
                        }
                    }
                    if (v) {
                        v.controls = false;
                        v.removeAttribute('controls');
                        try { v.currentTime = $targetSec; } catch(e) {}
                    }
                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                        try {
                            var p = window.jwplayer();
                            if (p && typeof p.seek === 'function') p.seek($targetSec);
                            if (p && typeof p.setControls === 'function') p.setControls(false);
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
        } else {
            exoPlayer.seekTo(clampedMs)
        }
    }

    val playEpisode: (EpisodeItem) -> Unit = { ep ->
        if (isSeriesContent) {
            showEpisodesDialog = false
            showNextEpisodePrompt = false
            showControls = true
            isNextEpisodeDismissed = false
            if (selectedModalSeason != ep.seasonNumber) {
                selectedModalSeason = ep.seasonNumber
            }
            currentMovieTitle = "$cleanMainTitle - T${ep.seasonNumber}:E${ep.episodeNumber}"
            loadingSubtitle = "Cargando Temporada ${ep.seasonNumber} · Episodio ${ep.episodeNumber}..."
            isEpisodeChanging = true
            WatchHistoryManager.saveToHistory(
                context = context,
                movie = movie.copy(title = cleanMainTitle),
                positionMs = 0L,
                durationMs = 0L,
                seasonNumber = ep.seasonNumber,
                episodeNumber = ep.episodeNumber,
                isSeries = true,
                postId = movie.postId,
                slug = movie.slug
            )
            isBuffering = true
            isVideoReady = false
            currentPositionMs = 0L
            scope.launch {
                loadingSubtitle = "Obteniendo enlaces del Episodio ${ep.episodeNumber}..."
                val newStreams = withContext(Dispatchers.IO) {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(9000L) {
                            MovieRepository.resolveEpisodeStreams(
                                episodeId = ep.id,
                                seriesTitle = cleanMainTitle,
                                fallbackStreams = availableStreams,
                                seasonNumber = ep.seasonNumber,
                                episodeNumber = ep.episodeNumber,
                                seriesPostId = detailResponse?.postId?.toIntOrNull()
                            )
                        } ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                if (newStreams.isNotEmpty()) {
                    loadingSubtitle = "Conectando al servidor en alta definición..."
                    failedStreamUrls.clear()
                    dynamicStreams = newStreams
                    switchServer(0, resetPosition = true, streamList = newStreams)
                } else {
                    seekToastText = "No se encontraron enlaces para el Episodio ${ep.episodeNumber}"
                    seekToMs(0L)
                    setPlaybackPlaying(true)
                    isBuffering = false
                    isVideoReady = true
                    isEpisodeChanging = false
                    loadingSubtitle = "Preparando reproducción en alta definición"
                }
            }
        }
    }

    fun selectExoSubtitle(subTrack: ExoSubtitleTrackItem?) {
        if (subTrack == null) {
            subtitlesEnabled = false
            selectedSubtitleOption = 0
            try {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                    .build()
            } catch (_: Exception) {}
        } else {
            subtitlesEnabled = true
            selectedSubtitleOption = if (subTrack.isEnglish) 2 else 1
            try {
                val override = TrackSelectionOverride(
                    subTrack.group.mediaTrackGroup,
                    listOf(subTrack.trackIndex)
                )
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                    .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                    .addOverride(override)
                    .setPreferredTextLanguage(subTrack.language)
                    .build()
            } catch (_: Exception) {}
        }
    }

    fun selectSubtitleTrackData(subData: SubtitleTrackData?) {
        if (subData == null) {
            selectExoSubtitle(null)
            return
        }
        subtitlesEnabled = true
        selectedSubtitleOption = if (subData.language.startsWith("en", ignoreCase = true)) 2 else 1
        val matched = exoSubtitleTracks.firstOrNull {
            it.language.equals(subData.language, ignoreCase = true) ||
            it.name.contains(subData.label, ignoreCase = true) ||
            (subData.language.startsWith("es", ignoreCase = true) && it.isSpanish) ||
            (subData.language.startsWith("en", ignoreCase = true) && it.isEnglish)
        }
        if (matched != null) {
            selectExoSubtitle(matched)
        } else {
            try {
                val targetLang = subData.language
                val altLang = if (targetLang == "es") "spa" else "eng"
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                    .setPreferredTextLanguages(targetLang, altLang)
                    .build()
            } catch (_: Exception) {}
        }
    }

    fun applyWebSubtitle(enabled: Boolean, trackIdx: Int = -1) {
        subtitlesEnabled = enabled
        selectedWebSubtitleTrackIdx = trackIdx
        if (isEmbedStream) {
            val subJs = """
                (function() {
                    var enableSubs = $enabled;
                    var trackIdx = $trackIdx;
                    function updateDocSubs(doc) {
                        if (!doc) return;
                        var st = doc.getElementById('tvpro-subtitles-toggle');
                        if (!st) {
                            st = doc.createElement('style');
                            st.id = 'tvpro-subtitles-toggle';
                            (doc.head || doc.documentElement).appendChild(st);
                        }
                        if (!enableSubs) {
                            st.innerHTML = '.jw-captions, .jw-text-track-display, .jw-text-track-container, .jw-text-track-cue, .vjs-text-track-display, .plyr__captions, video::cue { display: none !important; opacity: 0 !important; visibility: hidden !important; }';
                        } else {
                            st.innerHTML = '';
                        }
                        var v = doc.querySelector('video');
                        if (v && v.textTracks) {
                            for (var i = 0; i < v.textTracks.length; i++) {
                                var tr = v.textTracks[i];
                                if (!enableSubs) {
                                    tr.mode = 'disabled';
                                } else {
                                    tr.mode = (trackIdx < 0 || i === trackIdx) ? 'showing' : 'disabled';
                                }
                            }
                        }
                    }
                    updateDocSubs(document);
                    var iframes = document.querySelectorAll('iframe');
                    for (var j = 0; j < iframes.length; j++) {
                        try { updateDocSubs(iframes[j].contentDocument); } catch(e) {}
                    }
                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                        try {
                            var p = window.jwplayer();
                            if (p && typeof p.setCurrentCaptions === 'function') {
                                if (!enableSubs) {
                                    p.setCurrentCaptions(0);
                                } else {
                                    p.setCurrentCaptions(trackIdx >= 0 ? (trackIdx + 1) : 1);
                                }
                            }
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(subJs, null)
        }
    }

    fun applySubtitlePreference(enabled: Boolean, optionIdx: Int) {
        subtitlesEnabled = enabled
        selectedSubtitleOption = optionIdx
        val langCode = if (optionIdx == 2) "en" else "es"
        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !enabled)
                .setPreferredTextLanguage(langCode)
                .build()
        } catch (_: Exception) {}

        if (isEmbedStream) {
            val subJs = """
                (function() {
                    var enableSubs = $enabled;
                    var prefLang = '$langCode';
                    function updateDocSubs(doc) {
                        if (!doc) return;
                        var st = doc.getElementById('tvpro-subtitles-toggle');
                        if (!st) {
                            st = doc.createElement('style');
                            st.id = 'tvpro-subtitles-toggle';
                            (doc.head || doc.documentElement).appendChild(st);
                        }
                        if (!enableSubs) {
                            st.innerHTML = '.jw-captions, .jw-text-track-display, .jw-text-track-container, .jw-text-track-cue, .vjs-text-track-display, .plyr__captions, video::cue { display: none !important; opacity: 0 !important; visibility: hidden !important; }';
                        } else {
                            st.innerHTML = '';
                        }
                        var v = doc.querySelector('video');
                        if (v && v.textTracks) {
                            for (var i = 0; i < v.textTracks.length; i++) {
                                var tr = v.textTracks[i];
                                if (!enableSubs) {
                                    tr.mode = 'disabled';
                                } else {
                                    var lbl = ((tr.label || '') + ' ' + (tr.language || '')).toLowerCase();
                                    if (prefLang === 'en' && (lbl.indexOf('en') !== -1 || lbl.indexOf('ing') !== -1)) {
                                        tr.mode = 'showing';
                                    } else if (prefLang === 'es' && (lbl.indexOf('es') !== -1 || lbl.indexOf('spa') !== -1 || lbl.indexOf('lat') !== -1 || i === 0)) {
                                        tr.mode = 'showing';
                                    } else {
                                        tr.mode = (v.textTracks.length === 1) ? 'showing' : 'disabled';
                                    }
                                }
                            }
                        }
                    }
                    updateDocSubs(document);
                    var iframes = document.querySelectorAll('iframe');
                    for (var j = 0; j < iframes.length; j++) {
                        try { updateDocSubs(iframes[j].contentDocument); } catch(e) {}
                    }
                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                        try {
                            var p = window.jwplayer();
                            if (p && typeof p.setCurrentCaptions === 'function') {
                                if (!enableSubs) {
                                    p.setCurrentCaptions(0);
                                } else {
                                    var list = (typeof p.getCaptionsList === 'function') ? p.getCaptionsList() : [];
                                    var chosen = 1;
                                    for (var k = 1; k < list.length; k++) {
                                        var cLbl = (list[k].label || '').toLowerCase();
                                        if (prefLang === 'en' && (cLbl.indexOf('en') !== -1 || cLbl.indexOf('ing') !== -1)) { chosen = k; break; }
                                        if (prefLang === 'es' && (cLbl.indexOf('es') !== -1 || cLbl.indexOf('spa') !== -1 || cLbl.indexOf('lat') !== -1)) { chosen = k; break; }
                                    }
                                    p.setCurrentCaptions(chosen);
                                }
                            }
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(subJs, null)
        }
    }

    fun applyWebAudioTrack(trackIdx: Int) {
        userSelectedAudioTrack = true
        selectedWebAudioTrackIdx = trackIdx
        if (isEmbedStream) {
            val audioJs = """
                (function() {
                    function applyTrack(win, doc) {
                        if (!win) return;
                        if (win.jwplayer && typeof win.jwplayer === 'function') {
                            try {
                                var p = win.jwplayer();
                                if (p && typeof p.setCurrentAudioTrack === 'function') {
                                    p.setCurrentAudioTrack($trackIdx);
                                }
                            } catch(e) {}
                        }
                        if (doc) {
                            var v = doc.querySelector('video');
                            if (v && v.audioTracks) {
                                for (var i = 0; i < v.audioTracks.length; i++) {
                                    v.audioTracks[i].enabled = (i === $trackIdx);
                                }
                            }
                        }
                    }
                    applyTrack(window, document);
                    var iframes = document.querySelectorAll('iframe');
                    for (var i = 0; i < iframes.length; i++) {
                        try {
                            applyTrack(iframes[i].contentWindow, iframes[i].contentDocument);
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(audioJs, null)
        }
    }

    // Initialize or re-switch source cleanly and save movie to recent history
    LaunchedEffect(movie.url) {
        val sNum = if (isSeriesContent) currentSeasonNum.toIntOrNull() else null
        val eNum = if (isSeriesContent) currentEpNum.toIntOrNull() else null
        WatchHistoryManager.saveToHistory(
            context,
            movie.copy(title = cleanMainTitle),
            positionMs = initialResumePositionMs,
            durationMs = savedHistory?.durationMs ?: 0L,
            seasonNumber = sNum,
            episodeNumber = eNum,
            isSeries = isSeriesContent,
            postId = movie.postId,
            slug = movie.slug
        )
        if (availableStreams.isNotEmpty()) {
            switchServer(currentStreamIndex, resetPosition = (initialResumePositionMs <= 0L))
            if (initialResumePositionMs > 0L) {
                val resumeTimeStr = formatTimeSec(initialResumePositionMs / 1000)
                seekToastText = "Reanudando en $resumeTimeStr"
            }
        }
    }

    // Real-time synchronization with Embedded Native Web Player (<video> / JWPlayer) + Hide Web Chrome & Grey Poster
    LaunchedEffect(isEmbedStream, webViewRef, activeStreamUrl, subtitlesEnabled) {
        while (isActive && isEmbedStream) {
            delay(350)
            val wv = webViewRef
            if (wv != null) {
                val syncJs = """
                    (function() {
                        var subsOn = $subtitlesEnabled;
                        function hideNativeChrome(doc) {
                            if (!doc) return;
                            if (!doc.getElementById('tvpro-hide-native-ui')) {
                                var st = doc.createElement('style');
                                st.id = 'tvpro-hide-native-ui';
                                st.innerHTML = `
                                    html, body, video, .jwplayer, .jw-media {
                                        background: #000 !important;
                                        background-color: #000 !important;
                                    }
                                    .jw-preview, .vjs-poster, .plyr__poster,
                                    .jw-controls, .jw-controlbar, .jw-display, .jw-display-icon-container,
                                    .jw-display-controls, .jw-title, .jw-logo, .jw-overlays, .jw-settings-menu,
                                    .jw-controls-backdrop, .jw-nextup-container, .jw-float-icon, .jw-rightclick,
                                    .jw-button-container, .jw-slider-time,
                                    .vjs-control-bar, .vjs-big-play-button, .vjs-title-bar, .vjs-modal-dialog,
                                    .plyr__controls, .plyr__control--overlaid, .fluid_controls_container, .media-control,
                                    div.video_ad, div.video_ad_fadein, #over_player_msg, #adbd, #logo-vimeos {
                                        opacity: 0 !important;
                                        visibility: hidden !important;
                                        pointer-events: none !important;
                                        display: none !important;
                                        background-image: none !important;
                                    }
                                    video::-webkit-media-controls,
                                    video::-webkit-media-controls-enclosure,
                                    video::-webkit-media-controls-panel,
                                    video::-webkit-media-controls-overlay-play-button {
                                        display: none !important;
                                        opacity: 0 !important;
                                        -webkit-appearance: none !important;
                                    }
                                `;
                                (doc.head || doc.documentElement).appendChild(st);
                            }
                            var subSt = doc.getElementById('tvpro-subtitles-toggle');
                            if (!subSt) {
                                subSt = doc.createElement('style');
                                subSt.id = 'tvpro-subtitles-toggle';
                                (doc.head || doc.documentElement).appendChild(subSt);
                            }
                            if (!subsOn) {
                                subSt.innerHTML = '.jw-captions, .jw-text-track-display, .jw-text-track-container, .jw-text-track-cue, .vjs-text-track-display, .plyr__captions, video::cue { display: none !important; opacity: 0 !important; visibility: hidden !important; }';
                            } else {
                                subSt.innerHTML = '';
                            }
                        }
                        hideNativeChrome(document);
                        document.querySelectorAll('div.video_ad, div.video_ad_fadein, #over_player_msg, #adbd, a[target="_blank"]').forEach(function(el) {
                            try { el.remove(); } catch(e) {}
                        });
                        var v = document.querySelector('video');
                        if (!v) {
                            var iframes = document.querySelectorAll('iframe');
                            for (var i = 0; i < iframes.length; i++) {
                                try {
                                    var idoc = iframes[i].contentDocument;
                                    if (idoc) {
                                        hideNativeChrome(idoc);
                                        var iv = idoc.querySelector('video');
                                        if (iv) { v = iv; break; }
                                    }
                                } catch(e) {}
                            }
                        }
                        if (v) {
                            v.controls = false;
                            v.removeAttribute('controls');
                            v.removeAttribute('poster');
                            v.style.backgroundColor = '#000';
                            if (!subsOn && v.textTracks) {
                                for (var t = 0; t < v.textTracks.length; t++) {
                                    v.textTracks[t].mode = 'disabled';
                                }
                            }
                        }
                        var audioLabels = [];
                        var p = (window.jwplayer && typeof window.jwplayer === 'function') ? window.jwplayer() : null;
                        if (!p || typeof p.getAudioTracks !== 'function') {
                            var iframes = document.querySelectorAll('iframe');
                            for (var i = 0; i < iframes.length; i++) {
                                try {
                                    var win = iframes[i].contentWindow;
                                    if (win && win.jwplayer && typeof win.jwplayer === 'function') {
                                        var ip = win.jwplayer();
                                        if (ip && typeof ip.getAudioTracks === 'function') { p = ip; break; }
                                    }
                                } catch(e) {}
                            }
                        }
                        if (p) {
                            try {
                                var st = (typeof p.getState === 'function') ? p.getState() : '';
                                if (st === 'idle' && typeof p.play === 'function') p.play(true);
                                if (typeof p.setControls === 'function') p.setControls(false);
                                if (!subsOn && typeof p.setCurrentCaptions === 'function') p.setCurrentCaptions(0);
                                if (typeof p.getAudioTracks === 'function') {
                                    var ats = p.getAudioTracks() || [];
                                    for (var a = 0; a < ats.length; a++) {
                                        audioLabels.push(ats[a].name || ats[a].label || ats[a].language || ('Pista ' + (a + 1)));
                                    }
                                    if (!window.__tvpro_audio_inited && ats.length > 1) {
                                        for (var a = 0; a < ats.length; a++) {
                                            var n = (ats[a].name || ats[a].label || ats[a].language || '').toLowerCase();
                                            if (n.indexOf('espan') !== -1 || n.indexOf('españ') !== -1 || n.indexOf('lat') !== -1 || n.indexOf('spa') !== -1 || n.indexOf('castell') !== -1) {
                                                p.setCurrentAudioTrack(a);
                                                window.__tvpro_audio_inited = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch(e) {}
                        }
                        if (audioLabels.length === 0 && v && v.audioTracks && v.audioTracks.length > 0) {
                            for (var a = 0; a < v.audioTracks.length; a++) {
                                var tr = v.audioTracks[a];
                                audioLabels.push(tr.label || tr.language || ('Pista ' + (a + 1)));
                            }
                            if (!window.__tvpro_audio_inited && v.audioTracks.length > 1) {
                                for (var a = 0; a < v.audioTracks.length; a++) {
                                    var tr = v.audioTracks[a];
                                    var n = ((tr.label || '') + ' ' + (tr.language || '')).toLowerCase();
                                    if (n.indexOf('espan') !== -1 || n.indexOf('españ') !== -1 || n.indexOf('lat') !== -1 || n.indexOf('spa') !== -1 || n.indexOf('castell') !== -1) {
                                        for (var j = 0; j < v.audioTracks.length; j++) {
                                            v.audioTracks[j].enabled = (j === a);
                                        }
                                        window.__tvpro_audio_inited = true;
                                        break;
                                    }
                                }
                            }
                        }
                        var subLabels = [];
                        if (p && typeof p.getCaptionsList === 'function') {
                            var caps = p.getCaptionsList() || [];
                            for (var c = 0; c < caps.length; c++) {
                                var cl = caps[c].label || caps[c].name || caps[c].language || '';
                                if (cl && cl.toLowerCase() !== 'off' && cl.toLowerCase() !== 'desactivado' && cl.toLowerCase() !== 'none') {
                                    subLabels.push(cl);
                                }
                            }
                        }
                        if (subLabels.length === 0 && v && v.textTracks && v.textTracks.length > 0) {
                            for (var c = 0; c < v.textTracks.length; c++) {
                                var tr = v.textTracks[c];
                                var cl = tr.label || tr.language || '';
                                if (cl && cl.toLowerCase() !== 'off' && cl.toLowerCase() !== 'disabled') {
                                    subLabels.push(cl);
                                }
                            }
                        }
                        var bodyText = (document.body ? (document.body.innerText || document.body.textContent || '') : '').toLowerCase();
                        var isDead = bodyText.includes('232001') ||
                            bodyText.includes('no se puede reproducir') ||
                            bodyText.includes('archivo de video no se puede reproducir') ||
                            bodyText.includes('video file cannot be played') ||
                            bodyText.includes('error loading media') ||
                            bodyText.includes('the media playback was aborted') ||
                            bodyText.includes('expired or has been deleted') ||
                            bodyText.includes('no longer available') ||
                            bodyText.includes('file was deleted') ||
                            bodyText.includes('video deleted') ||
                            bodyText.includes('contenido no disponible') ||
                            bodyText.includes('byse frontend');

                        if (!isDead) {
                            var iframes = document.querySelectorAll('iframe');
                            for (var i = 0; i < iframes.length; i++) {
                                try {
                                    var idoc = iframes[i].contentDocument;
                                    if (idoc && idoc.body) {
                                        var itxt = (idoc.body.innerText || idoc.body.textContent || '').toLowerCase();
                                        if (itxt.includes('232001') || itxt.includes('no se puede reproducir') || itxt.includes('video file cannot be played')) {
                                            isDead = true;
                                            break;
                                        }
                                    }
                                } catch(e) {}
                            }
                        }

                        if (!isDead && p) {
                            try {
                                if (typeof p.getState === 'function' && p.getState() === 'error') {
                                    isDead = true;
                                }
                            } catch(e) {}
                        }
                        if (!isDead && v && v.error) {
                            isDead = true;
                        }

                        if (isDead) {
                            return JSON.stringify({ dead: true, reason: 'Error 232001' });
                        }

                        if (v && v.duration && !isNaN(v.duration) && v.duration > 0) {
                            var ready = (v.readyState >= 2 && (v.currentTime > 0.05 || v.videoWidth > 0));
                            var isEnded = v.ended || (v.duration > 30 && v.currentTime >= v.duration - 2.5);
                            return JSON.stringify({
                                pos: Math.floor(v.currentTime * 1000),
                                dur: Math.floor(v.duration * 1000),
                                playing: !v.paused && !v.ended,
                                ready: ready,
                                ended: isEnded,
                                audioTracks: audioLabels,
                                subtitleTracks: subLabels
                            });
                        }
                        if (p && typeof p.getPosition === 'function' && typeof p.getDuration === 'function') {
                            try {
                                var d = p.getDuration();
                                if (d > 0) {
                                    var jwState = (typeof p.getState === 'function') ? p.getState() : '';
                                    var jwReady = (jwState === 'playing' || jwState === 'paused' || p.getPosition() > 0.05);
                                    var jwEnded = (jwState === 'complete' || (d > 30 && p.getPosition() >= d - 2.5));
                                    return JSON.stringify({
                                        pos: Math.floor(p.getPosition() * 1000),
                                        dur: Math.floor(d * 1000),
                                        playing: jwState === 'playing',
                                        ready: jwReady,
                                        ended: jwEnded,
                                        audioTracks: audioLabels,
                                        subtitleTracks: subLabels
                                    });
                                }
                            } catch(e) {}
                        }
                        return '';
                    })();
                """.trimIndent()
                wv.evaluateJavascript(syncJs) { rawResult ->
                    try {
                        val cleaned = rawResult
                            ?.trim()
                            ?.removeSurrounding("\"")
                            ?.replace("\\\"", "\"")
                            ?.replace("\\\\", "\\")
                        if (!cleaned.isNullOrBlank() && cleaned.startsWith("{")) {
                            val obj = JSONObject(cleaned)
                            if (obj.optBoolean("dead", false)) {
                                val deadReason = obj.optString("reason", "Error 232001")
                                triggerStreamFailover(activeStreamUrl, deadReason)
                                return@evaluateJavascript
                            }
                            val pos = obj.optLong("pos", 0L)
                            val dur = obj.optLong("dur", 0L)
                            val playingState = obj.optBoolean("playing", isPlaying)
                            val readyState = obj.optBoolean("ready", false)
                            val tracksArr = obj.optJSONArray("audioTracks")
                            if (tracksArr != null && tracksArr.length() > 1) {
                                val parsed = mutableListOf<String>()
                                for (idx in 0 until tracksArr.length()) {
                                    parsed.add(tracksArr.optString(idx, "Audio ${idx + 1}"))
                                }
                                if (parsed != nativeWebAudioTracks) {
                                    nativeWebAudioTracks = parsed
                                    if (!userSelectedAudioTrack) {
                                        val spanishIdx = parsed.indexOfFirst { trackStr ->
                                            val s = trackStr.lowercase()
                                            s.contains("espan") || s.contains("españ") || s.contains("lat") || s.contains("spanish") || s.contains("spa") || s.contains("castell")
                                        }
                                        if (spanishIdx >= 0 && spanishIdx != selectedWebAudioTrackIdx) {
                                            applyWebAudioTrack(spanishIdx)
                                        }
                                    }
                                }
                            }
                            val subTracksArr = obj.optJSONArray("subtitleTracks")
                            if (subTracksArr != null) {
                                val parsedSubs = mutableListOf<String>()
                                for (idx in 0 until subTracksArr.length()) {
                                    val s = subTracksArr.optString(idx, "")
                                    if (s.isNotBlank() && !parsedSubs.contains(s)) {
                                        parsedSubs.add(s)
                                    }
                                }
                                if (parsedSubs != nativeWebSubtitleTracks) {
                                    nativeWebSubtitleTracks = parsedSubs
                                }
                            }
                            if (dur > 0L) {
                                durationMs = dur
                                currentPositionMs = pos.coerceIn(0L, dur)
                                isPlaying = playingState
                                 if (readyState || pos > 50L) {
                                    if (!isVideoReady) {
                                        isVideoReady = true
                                        isBuffering = false
                                        isEpisodeChanging = false
                                        loadingSubtitle = "Preparando reproducción en alta definición"
                                    }
                                }
                                if (pendingResumePositionMs > 0L) {
                                    val target = pendingResumePositionMs
                                    pendingResumePositionMs = -1L
                                    seekToMs(target)
                                }
                                val isStreamEnded = obj.optBoolean("ended", false)
                                if (isStreamEnded) {
                                    if (isSeriesContent && nextEpisode != null && !isNextEpisodeDismissed && !isEpisodeChanging) {
                                        showControls = false
                                        showNextEpisodePrompt = true
                                    } else if (!isSeriesContent && !showMovieEndedPrompt) {
                                        showControls = false
                                        showMovieEndedPrompt = true
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Player Listener for state, buffering, and automatic failover
    DisposableEffect(exoPlayer, isEmbedStream) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!isEmbedStream) {
                    isBuffering = (playbackState == Player.STATE_BUFFERING)
                    if (playbackState == Player.STATE_READY) {
                        isVideoReady = true
                        isEpisodeChanging = false
                        loadingSubtitle = "Preparando reproducción en alta definición"
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    } else if (playbackState == Player.STATE_ENDED) {
                        if (isSeriesContent && nextEpisode != null && !isNextEpisodeDismissed && !isEpisodeChanging) {
                            showControls = false
                            showNextEpisodePrompt = true
                        } else if (!isSeriesContent && !showMovieEndedPrompt) {
                            showControls = false
                            showMovieEndedPrompt = true
                        }
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                if (isEmbedStream) return
                val audioList = mutableListOf<ExoAudioTrackItem>()
                val subtitleList = mutableListOf<ExoSubtitleTrackItem>()
                var spanishCandidate: ExoAudioTrackItem? = null
                var isAnySpanishSelected = false

                val isCurrentStreamEnglish = ((currentStream.source ?: "") + " " + currentStream.url).lowercase().let { raw ->
                    val isExplicitSpanishOnly = (raw.contains("latino") || raw.contains("castellano")) &&
                        !raw.contains("dual") && !raw.contains("multi") && !raw.contains("ingl") && !raw.contains("sub")
                    if (isExplicitSpanishOnly) false
                    else raw.contains("sub") || raw.contains("inglés") || raw.contains("ingles") || raw.contains("english") || raw.contains("orig")
                }

                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val isSelected = group.isTrackSelected(i)
                            val lang = (format.language ?: "").lowercase()
                            val label = (format.label ?: "").lowercase()
                            val rawFormatLabel = format.label?.trim() ?: ""

                            var isSpanish = lang.startsWith("es") || lang.startsWith("spa") ||
                                lang.contains("419") ||
                                label.contains("espan") || label.contains("españ") ||
                                label.contains("lat") || label.contains("spanish") ||
                                label.contains("castellano")
                            var isEnglish = lang.startsWith("en") || lang.startsWith("eng") ||
                                label.contains("ingles") || label.contains("inglés") ||
                                label.contains("english") ||
                                (label.contains("orig") && !label.contains("port") && !label.contains("esp"))
                            val isPortuguese = lang.startsWith("pt") || lang.startsWith("por") ||
                                label.contains("portug") || label.contains("brasil") || label.contains("brazil")
                            val isJapanese = lang.startsWith("ja") || lang.startsWith("jpn") ||
                                label.contains("jap") || label.contains("anime")
                            val isFrench = lang.startsWith("fr") || lang.startsWith("fra") || lang.startsWith("fre") ||
                                label.contains("franc") || label.contains("french")
                            val isItalian = lang.startsWith("it") || lang.startsWith("ita") ||
                                label.contains("ital")
                            val isGerman = lang.startsWith("de") || lang.startsWith("deu") || lang.startsWith("ger") ||
                                label.contains("alem") || label.contains("german") || label.contains("deutsch")
                            val isRussian = lang.startsWith("ru") || lang.startsWith("rus") ||
                                label.contains("rus")
                            val isKorean = lang.startsWith("ko") || lang.startsWith("kor") ||
                                label.contains("corean") || label.contains("korean")
                            val isChinese = lang.startsWith("zh") || lang.startsWith("chi") || lang.startsWith("zho") ||
                                label.contains("chin")

                            // When format language and label are undetermined (e.g. "und", "", raw streams):
                            val hasRecognizedLanguage = isSpanish || isEnglish || isPortuguese || isJapanese ||
                                isFrench || isItalian || isGerman || isRussian || isKorean || isChinese
                            if (!hasRecognizedLanguage) {
                                val isUndeterminedLang = lang.isBlank() || lang == "und" || lang == "mis" || lang == "mul" || lang == "zxx"
                                val isGenericAudioLabel = rawFormatLabel.isBlank() || rawFormatLabel.equals("und", true) || rawFormatLabel.startsWith("audio", true)
                                if (isUndeterminedLang && isGenericAudioLabel) {
                                    if (isCurrentStreamEnglish) {
                                        isEnglish = true
                                    } else {
                                        isSpanish = true
                                    }
                                }
                            }

                            val isGenericSpanishLabel = rawFormatLabel.equals("es", true) ||
                                rawFormatLabel.equals("spa", true) ||
                                rawFormatLabel.equals("spanish", true) ||
                                rawFormatLabel.equals("español", true) ||
                                rawFormatLabel.equals("espanol", true) ||
                                rawFormatLabel.startsWith("audio", true) ||
                                rawFormatLabel.equals("und", true)
                            val isGenericEnglishLabel = rawFormatLabel.equals("en", true) ||
                                rawFormatLabel.equals("eng", true) ||
                                rawFormatLabel.equals("english", true) ||
                                rawFormatLabel.equals("ingles", true) ||
                                rawFormatLabel.equals("inglés", true) ||
                                rawFormatLabel.startsWith("audio", true) ||
                                rawFormatLabel.equals("und", true)

                            val displayName = when {
                                isPortuguese -> {
                                    if (label.contains("brasil") || label.contains("brazil")) "Portugués (Brasil)"
                                    else "Portugués"
                                }
                                isSpanish -> {
                                    if (label.contains("castellano") || label.contains("españa") || label.contains("spain")) "Español (Castellano)"
                                    else if (rawFormatLabel.isNotBlank() && !isGenericSpanishLabel) "Español ($rawFormatLabel)"
                                    else "Español Latino"
                                }
                                isEnglish -> {
                                    if (rawFormatLabel.isNotBlank() && !isGenericEnglishLabel && !label.contains("orig")) "Inglés ($rawFormatLabel)"
                                    else "Inglés"
                                }
                                isJapanese -> "Japonés"
                                isFrench -> "Francés"
                                isItalian -> "Italiano"
                                isGerman -> "Alemán"
                                isRussian -> "Ruso"
                                isKorean -> "Coreano"
                                isChinese -> "Chino"
                                !format.label.isNullOrBlank() && !format.label!!.startsWith("audio", true) -> format.label!!
                                !format.language.isNullOrBlank() && !format.language!!.equals("und", true) -> format.language!!.uppercase()
                                else -> if (isCurrentStreamEnglish) "Inglés" else "Español Latino"
                            }

                            val item = ExoAudioTrackItem(
                                group = group,
                                trackIndex = i,
                                name = displayName,
                                language = format.language ?: "",
                                isSelected = isSelected,
                                isSpanish = isSpanish,
                                isEnglish = isEnglish
                            )
                            if (isSelected && isSpanish) {
                                isAnySpanishSelected = true
                            }
                            if (isSpanish && spanishCandidate == null) {
                                spanishCandidate = item
                            }
                            audioList.add(item)
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSupported(i)) {
                                val format = group.getTrackFormat(i)
                                val isSelected = group.isTrackSelected(i)
                                val lang = (format.language ?: "").lowercase()
                                val label = (format.label ?: "").lowercase()
                                val isSpanish = lang.startsWith("es") || lang.startsWith("spa") ||
                                    lang.contains("419") ||
                                    label.contains("espan") || label.contains("españ") ||
                                    label.contains("lat") || label.contains("spanish") ||
                                    label.contains("castellano")
                                val isEnglish = lang.startsWith("en") || lang.startsWith("eng") ||
                                    label.contains("ingles") || label.contains("inglés") ||
                                    label.contains("english") ||
                                    (label.contains("orig") && !label.contains("port") && !label.contains("esp"))
                                val isPortuguese = lang.startsWith("pt") || lang.startsWith("por") ||
                                    label.contains("portug") || label.contains("brasil") || label.contains("brazil")

                                val rawFormatLabel = format.label?.trim() ?: ""
                                val isGenericSpanishLabel = rawFormatLabel.equals("es", true) ||
                                    rawFormatLabel.equals("spa", true) ||
                                    rawFormatLabel.equals("spanish", true) ||
                                    rawFormatLabel.equals("español", true) ||
                                    rawFormatLabel.equals("espanol", true)
                                val isGenericEnglishLabel = rawFormatLabel.equals("en", true) ||
                                    rawFormatLabel.equals("eng", true) ||
                                    rawFormatLabel.equals("english", true) ||
                                    rawFormatLabel.equals("ingles", true) ||
                                    rawFormatLabel.equals("inglés", true)

                                val displayName = when {
                                    isPortuguese -> {
                                        if (label.contains("brasil") || label.contains("brazil")) "Portugués (Brasil)"
                                        else "Portugués"
                                    }
                                    isSpanish -> if (rawFormatLabel.isNotBlank() && !isGenericSpanishLabel) "Español ($rawFormatLabel)" else "Español"
                                    isEnglish -> if (rawFormatLabel.isNotBlank() && !isGenericEnglishLabel) "Inglés ($rawFormatLabel)" else "Inglés"
                                    rawFormatLabel.contains("portug", true) -> "Portugués"
                                    !format.label.isNullOrBlank() -> format.label!!
                                    !format.language.isNullOrBlank() -> format.language!!.uppercase()
                                    else -> "Subtítulo ${subtitleList.size + 1}"
                                }

                                subtitleList.add(
                                    ExoSubtitleTrackItem(
                                        group = group,
                                        trackIndex = i,
                                        name = displayName,
                                        language = format.language ?: "",
                                        isSelected = isSelected,
                                        isSpanish = isSpanish,
                                        isEnglish = isEnglish
                                    )
                                )
                            }
                        }
                    }
                }

                exoAudioTracks = audioList
                exoSubtitleTracks = subtitleList

                // Automatically switch to Spanish if multiple audio tracks exist, no Spanish track is currently selected,
                // and the user has not explicitly chosen an audio track manually.
                if (!userSelectedAudioTrack && audioList.size > 1 && !isAnySpanishSelected && spanishCandidate != null) {
                    try {
                        val override = TrackSelectionOverride(
                            spanishCandidate.group.mediaTrackGroup,
                            listOf(spanishCandidate.trackIndex)
                        )
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .addOverride(override)
                            .setPreferredAudioLanguages("es", "es-419", "spa", "es-MX", "es-US", "es-ES", "es-CO", "es-AR")
                            .build()
                        android.util.Log.i("TVProAudio", "Auto-switched to Spanish track: ${spanishCandidate.name}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (!isEmbedStream) {
                    isPlaying = playing
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (isEmbedStream) return
                error.printStackTrace()
                // Automatic silent failover to next stream preserving playback position
                triggerStreamFailover(activeStreamUrl, "ExoPlayer Error: ${error.errorCodeName}")
            }
            override fun onRenderedFirstFrame() {
                if (!isEmbedStream) {
                    isVideoReady = true
                    isBuffering = false
                    isEpisodeChanging = false
                    loadingSubtitle = "Preparando reproducción en alta definición"
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        if (!isEmbedStream && exoPlayer.playbackState == Player.STATE_READY) {
            isVideoReady = true
            isBuffering = false
            isEpisodeChanging = false
            loadingSubtitle = "Preparando reproducción en alta definición"
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
        }

        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Proactive watchdog for ExoPlayer: ensures loading splash clears ONLY when player is actually ready
    LaunchedEffect(activeStreamUrl, isEmbedStream) {
        while (isActive && !isEmbedStream && !isVideoReady) {
            delay(300)
            if (exoPlayer.playbackState == Player.STATE_READY) {
                isVideoReady = true
                isBuffering = false
                isEpisodeChanging = false
                loadingSubtitle = "Preparando reproducción en alta definición"
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
        }
    }

    // Universal Playback Watchdog (ExoPlayer & Web Embed):
    // If the stream fails to reach STATE_READY or start video within 10 seconds, failover!
    LaunchedEffect(activeStreamUrl) {
        if (activeStreamUrl.isNotBlank()) {
            val targetUrl = activeStreamUrl
            val startTime = System.currentTimeMillis()
            while (isActive && activeStreamUrl == targetUrl) {
                delay(300)
                if (isVideoReady) {
                    // Video playback or readiness confirmed by player
                    break
                }
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= 10_000L) {
                    triggerStreamFailover(targetUrl, "Tiempo de espera agotado")
                    break
                }
            }
        }
    }

    // Save playback position periodically and on exit (for both ExoPlayer and WebView)
    LaunchedEffect(movie.url, isEmbedStream, currentSeasonNum, currentEpNum) {
        while (isActive) {
            delay(5000)
            val sNum = if (isSeriesContent) currentSeasonNum.toIntOrNull() else null
            val eNum = if (isSeriesContent) currentEpNum.toIntOrNull() else null
            if (isEmbedStream) {
                if (currentPositionMs > 0L && durationMs > 0L) {
                    WatchHistoryManager.updatePosition(
                        context,
                        movie.url,
                        currentPositionMs,
                        durationMs,
                        seasonNumber = sNum,
                        episodeNumber = eNum,
                        isSeries = isSeriesContent
                    )
                }
            } else if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                currentPositionMs = pos
                if (dur > 0) durationMs = dur
                WatchHistoryManager.updatePosition(
                    context,
                    movie.url,
                    pos,
                    dur,
                    seasonNumber = sNum,
                    episodeNumber = eNum,
                    isSeries = isSeriesContent
                )
            }
        }
    }

    // Trigger para mostrar tarjeta de siguiente episodio cuando restan 30 segundos, o fin de película
    LaunchedEffect(isSeriesContent, nextEpisode, isNextEpisodeDismissed, isEpisodeChanging, showNextEpisodePrompt, showMovieEndedPrompt) {
        while (isActive && !isEpisodeChanging) {
            delay(1000L)
            val pos = if (isEmbedStream) currentPositionMs else exoPlayer.currentPosition
            val dur = if (isEmbedStream) durationMs else exoPlayer.duration
            if (dur > 60_000L && pos > 0L) {
                val remaining = dur - pos
                if (isSeriesContent && nextEpisode != null && !isNextEpisodeDismissed && !showNextEpisodePrompt) {
                    if (remaining in 1L..30_000L) {
                        showControls = false
                        showNextEpisodePrompt = true
                        break
                    }
                } else if (!isSeriesContent && !showMovieEndedPrompt) {
                    if (pos >= dur - 3_000L) {
                        showControls = false
                        showMovieEndedPrompt = true
                        break
                    }
                }
            }
        }
    }

    LaunchedEffect(showMovieEndedPrompt) {
        if (showMovieEndedPrompt) {
            WatchHistoryManager.resetMoviePosition(context, movie.url)
            delay(120)
            try {
                movieEndedBackFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Cuenta regresiva automática de 10 segundos para el siguiente episodio (estilo Netflix)
    LaunchedEffect(showNextEpisodePrompt, nextEpisode) {
        if (showNextEpisodePrompt && nextEpisode != null) {
            try {
                delay(120)
                nextEpisodeFocusRequester.requestFocus()
            } catch (_: Exception) {}

            if (consecutiveAutoPlayCount < 3) {
                nextEpisodeCountdownSeconds = 10
                while (nextEpisodeCountdownSeconds > 0 && showNextEpisodePrompt) {
                    delay(1000L)
                    if (showNextEpisodePrompt) {
                        nextEpisodeCountdownSeconds--
                    }
                }
                if (showNextEpisodePrompt) {
                    showNextEpisodePrompt = false
                    consecutiveAutoPlayCount++
                    playEpisode(nextEpisode)
                }
            }
        }
    }

    val currentSeasonNumState by rememberUpdatedState(currentSeasonNum)
    val currentEpNumState by rememberUpdatedState(currentEpNum)
    val isSeriesContentState by rememberUpdatedState(isSeriesContent)
    val isEmbedStreamState by rememberUpdatedState(isEmbedStream)
    val currentPositionMsState by rememberUpdatedState(currentPositionMs)
    val durationMsState by rememberUpdatedState(durationMs)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                setPlaybackPlaying(false)
                val pos = if (isEmbedStreamState) currentPositionMsState else exoPlayer.currentPosition
                val dur = if (isEmbedStreamState) durationMsState else exoPlayer.duration
                if (pos > 0L) {
                    val sNum = currentSeasonNumState.toIntOrNull()
                    val eNum = currentEpNumState.toIntOrNull()
                    WatchHistoryManager.updatePosition(
                        context,
                        movie.url,
                        pos,
                        dur,
                        seasonNumber = sNum,
                        episodeNumber = eNum,
                        isSeries = isSeriesContentState
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val pos = if (isEmbedStreamState) currentPositionMsState else exoPlayer.currentPosition
            val dur = if (isEmbedStreamState) durationMsState else exoPlayer.duration
            if (pos > 0L) {
                val sNum = currentSeasonNumState.toIntOrNull()
                val eNum = currentEpNumState.toIntOrNull()
                WatchHistoryManager.updatePosition(
                    context,
                    movie.url,
                    pos,
                    dur,
                    seasonNumber = sNum,
                    episodeNumber = eNum,
                    isSeries = isSeriesContentState
                )
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer.release()
            } catch (_: Exception) {}
        }
    }

    var prevShowControls by remember { mutableStateOf(false) }
    var prevShowServerDialog by remember { mutableStateOf(false) }
    var prevShowEpisodesDialog by remember { mutableStateOf(false) }

    // Auto Focus appropriate button when controls, video readiness, or dialogs open/close
    LaunchedEffect(showServerDialog, showEpisodesDialog, showControls, isVideoReady) {
        delay(80)
        try {
            when {
                showEpisodesDialog -> activeEpisodeFocusRequester.requestFocus()
                showServerDialog -> activeAudioTrackFocusRequester.requestFocus()
                prevShowServerDialog && !showServerDialog && showControls -> {
                    serverPillFocusRequester.requestFocus()
                }
                prevShowEpisodesDialog && !showEpisodesDialog && showControls -> {
                    episodesButtonFocusRequester.requestFocus()
                }
                !prevShowControls && showControls && isVideoReady -> playButtonFocusRequester.requestFocus()
                !prevShowControls && showControls -> backButtonFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
        prevShowControls = showControls
        prevShowServerDialog = showServerDialog
        prevShowEpisodesDialog = showEpisodesDialog
    }

    var controlsWakeTick by remember { mutableIntStateOf(0) }
    var controlsHideFastTick by remember { mutableIntStateOf(0) }

    // Controls Auto-Hide Timer (Rápido y dinámico: 3.5s normal, 1.5s tras pulsar Play)
    LaunchedEffect(showControls, isPlaying, showServerDialog, showEpisodesDialog, controlsWakeTick, controlsHideFastTick) {
        if (showControls && isPlaying && !showServerDialog && !showEpisodesDialog) {
            val timeout = if (controlsHideFastTick > 0) 1500L else 3500L
            delay(timeout)
            showControls = false
            controlsHideFastTick = 0
        }
    }

    // Seek Toast Auto-Dismiss
    LaunchedEffect(seekToastText) {
        if (seekToastText != null) {
            delay(1500)
            seekToastText = null
        }
    }

    fun wakeUpControls() {
        if (showNextEpisodePrompt) return
        showControls = true
        controlsHideFastTick = 0
        controlsWakeTick++
    }

    fun performSeek(forward: Boolean, stepMs: Long = 10_000L) {
        if (showControls) {
            controlsHideFastTick = 0
            controlsWakeTick++
        }
        val basePos = if (isEmbedStream) currentPositionMs else exoPlayer.currentPosition.coerceAtLeast(0L)
        val maxDur = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        val newPos = if (forward) {
            minOf(maxDur, basePos + stepMs)
        } else {
            maxOf(0L, basePos - stepMs)
        }
        seekToMs(newPos)
        val timeLabel = formatTimeSec(newPos / 1000)
        seekToastText = if (forward) "+${stepMs / 1000}s  |  $timeLabel" else "-${stepMs / 1000}s  |  $timeLabel"
    }

    DisposableEffect(showControls, showServerDialog, showEpisodesDialog, showNextEpisodePrompt, showMovieEndedPrompt, isMovieEndedReplayFocused, isMovieEndedBackFocused, nextEpisodeSelectedButton, isPlaying, durationMs, isEmbedStream, currentPositionMs, nextEpisode, previousEpisode) {
        PlayerKeyDispatcher.onPlayerKeyEvent = { event ->
            if (showMovieEndedPrompt) {
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            try { movieEndedReplayFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            try { movieEndedBackFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_SPACE -> {
                            showMovieEndedPrompt = false
                            if (isMovieEndedReplayFocused) {
                                seekToMs(0L)
                                setPlaybackPlaying(true)
                            } else {
                                onBackClick()
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_BACK,
                        android.view.KeyEvent.KEYCODE_ESCAPE -> {
                            showMovieEndedPrompt = false
                            onBackClick()
                            true
                        }
                        else -> true
                    }
                } else {
                    true
                }
            } else if (showNextEpisodePrompt && nextEpisode != null) {
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            nextEpisodeSelectedButton = 0
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            nextEpisodeSelectedButton = 1
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_SPACE -> {
                            if (nextEpisodeSelectedButton == 0) {
                                consecutiveAutoPlayCount = 0
                                showNextEpisodePrompt = false
                                playEpisode(nextEpisode)
                            } else {
                                showNextEpisodePrompt = false
                                isNextEpisodeDismissed = true
                                consecutiveAutoPlayCount = 0
                                wakeUpControls()
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_BACK,
                        android.view.KeyEvent.KEYCODE_ESCAPE -> {
                            showNextEpisodePrompt = false
                            isNextEpisodeDismissed = true
                            consecutiveAutoPlayCount = 0
                            wakeUpControls()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            true
                        }
                        else -> true
                    }
                } else {
                    true
                }
            } else if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                val keyCode = event.keyCode
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK || keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
                    if (showEpisodesDialog) {
                        showEpisodesDialog = false
                        wakeUpControls()
                        true
                    } else if (showServerDialog) {
                        showServerDialog = false
                        wakeUpControls()
                        true
                    } else if (showControls) {
                        showControls = false
                        true
                    } else {
                        onBackClick()
                        true
                    }
                } else if (!showControls && !showServerDialog && !showEpisodesDialog) {
                    when (keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            performSeek(false)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT, android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            performSeek(true)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT, android.view.KeyEvent.KEYCODE_CHANNEL_UP -> {
                            if (isSeriesContent && nextEpisode != null) {
                                seekToastText = "Cargando ${nextEpisode.title}..."
                                playEpisode(nextEpisode)
                                true
                            } else {
                                wakeUpControls()
                                true
                            }
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS, android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            if (isSeriesContent && previousEpisode != null) {
                                seekToastText = "Cargando ${previousEpisode.title}..."
                                playEpisode(previousEpisode)
                                true
                            } else {
                                wakeUpControls()
                                true
                            }
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_SPACE,
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                        android.view.KeyEvent.KEYCODE_MENU,
                        android.view.KeyEvent.KEYCODE_INFO,
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            wakeUpControls()
                            true
                        }
                        else -> {
                            wakeUpControls()
                            true
                        }
                    }
                } else if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_NEXT) {
                    if (isSeriesContent && nextEpisode != null) {
                        seekToastText = "Cargando ${nextEpisode.title}..."
                        playEpisode(nextEpisode)
                        true
                    } else false
                } else if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                    if (isSeriesContent && previousEpisode != null) {
                        seekToastText = "Cargando ${previousEpisode.title}..."
                        playEpisode(previousEpisode)
                        true
                    } else false
                } else {
                    wakeUpControls()
                    false
                }
            } else {
                false
            }
        }
        onDispose {
            PlayerKeyDispatcher.onPlayerKeyEvent = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if (showMovieEndedPrompt) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                try { movieEndedReplayFocusRequester.requestFocus() } catch (_: Exception) {}
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionRight -> {
                                try { movieEndedBackFocusRequester.requestFocus() } catch (_: Exception) {}
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                                showMovieEndedPrompt = false
                                if (isMovieEndedReplayFocused) {
                                    seekToMs(0L)
                                    setPlaybackPlaying(true)
                                } else {
                                    onBackClick()
                                }
                                return@onPreviewKeyEvent true
                            }
                            Key.Back, Key.Escape -> {
                                showMovieEndedPrompt = false
                                onBackClick()
                                return@onPreviewKeyEvent true
                            }
                            else -> return@onPreviewKeyEvent true
                        }
                    }

                    if (showNextEpisodePrompt) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                nextEpisodeSelectedButton = 0
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionRight -> {
                                nextEpisodeSelectedButton = 1
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                                showNextEpisodePrompt = false
                                if (nextEpisodeSelectedButton == 0) {
                                    nextEpisode?.let { playEpisode(it) }
                                } else {
                                    isNextEpisodeDismissed = true
                                }
                                return@onPreviewKeyEvent true
                            }
                            Key.Back, Key.Escape -> {
                                showNextEpisodePrompt = false
                                isNextEpisodeDismissed = true
                                return@onPreviewKeyEvent true
                            }
                            else -> return@onPreviewKeyEvent true
                        }
                    }

                    if (event.key == Key.Back || event.key == Key.Escape) {
                        if (showEpisodesDialog) {
                            showEpisodesDialog = false
                            showControls = true
                            return@onPreviewKeyEvent true
                        } else if (showServerDialog) {
                            showServerDialog = false
                            showControls = true
                            return@onPreviewKeyEvent true
                        } else if (showControls) {
                            showControls = false
                            return@onPreviewKeyEvent true
                        } else {
                            onBackClick()
                            return@onPreviewKeyEvent true
                        }
                    }

                    // When controls are hidden, allow immediate pause/play or seek with a single remote press!
                    if (!showControls && !showServerDialog && !showEpisodesDialog && !showNextEpisodePrompt && !showMovieEndedPrompt) {
                        when (event.key) {
                            Key.DirectionLeft, Key.MediaRewind -> {
                                performSeek(false)
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionRight, Key.MediaFastForward -> {
                                performSeek(true)
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.MediaPlayPause -> {
                                togglePlayPause()
                                wakeUpControls()
                                return@onPreviewKeyEvent true
                            }
                            else -> {
                                wakeUpControls()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    wakeUpControls()
                    when (event.key) {
                        Key.MediaRewind -> {
                            performSeek(false)
                            true
                        }
                        Key.MediaFastForward -> {
                            performSeek(true)
                            true
                        }
                        Key.MediaPlayPause, Key.Spacebar -> {
                            togglePlayPause()
                            true
                        }
                        Key.MediaPlay -> {
                            setPlaybackPlaying(true)
                            true
                        }
                        Key.MediaPause -> {
                            setPlaybackPlaying(false)
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        if (availableStreams.isEmpty()) {
            val noContentFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(120)
                try { noContentFocusRequester.requestFocus() } catch (_: Exception) {}
            }
            BackHandler { onBackClick() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C0D10))
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                if (!movie.backdrop.isNullOrBlank() || movie.image.isNotBlank()) {
                    AsyncImage(
                        model = movie.backdrop ?: movie.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = 0.2f)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(max = 560.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161922).copy(alpha = 0.96f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Contenido aún no disponible",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aún no tenemos este contenido disponible para reproducir en nuestros servidores.",
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    var isBtnFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBtnFocused) Color(0xFFF5F2EB) else Color(0xFF1E232A),
                            contentColor = if (isBtnFocused) Color.Black else Color(0xFFF5F2EB)
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                        modifier = Modifier
                            .focusRequester(noContentFocusRequester)
                            .onFocusChanged { isBtnFocused = it.isFocused }
                    ) {
                        Text(
                            text = "VOLVER AL DETALLE",
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.4.sp
                        )
                    }
                }
            }
        }

        // Video View: Hybrid Native Media3 ExoPlayer & TV Fullscreen Web Player
        if (isEmbedStream) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        setBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        isFocusable = false
                        isFocusableInTouchMode = false
                        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.allowContentAccess = true
                        settings.allowFileAccess = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        try {
                            val cookieMgr = android.webkit.CookieManager.getInstance()
                            cookieMgr.setAcceptCookie(true)
                            cookieMgr.setAcceptThirdPartyCookies(this, true)
                        } catch (_: Exception) {}
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 TVPro/1.0"
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)

                        addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun onPlayerDead(reason: String?) {
                                scope.launch(Dispatchers.Main) {
                                    triggerStreamFailover(activeStreamUrl, reason ?: "Error de Reproducción")
                                }
                            }
                        }, "TVProBridge")

                        webChromeClient = object : WebChromeClient() {
                            override fun getDefaultVideoPoster(): android.graphics.Bitmap? {
                                return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                            }
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                if (newProgress in 40..85) {
                                    val earlyBootstrap = """
                                        (function() {
                                            var btn = document.querySelector('.jw-display-icon-container, .vjs-big-play-button, button.play, .play-button');
                                            if (btn) { try { btn.click(); } catch(e){} }
                                            var v = document.querySelector('video');
                                            if (v && v.paused) { try { v.play(); } catch(e){} }
                                            if (window.jwplayer && typeof window.jwplayer === 'function') {
                                                try {
                                                    var p = window.jwplayer();
                                                    if (p && typeof p.play === 'function') p.play(true);
                                                } catch(e) {}
                                            }
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(earlyBootstrap, null)
                                }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                handler?.proceed()
                            }
                            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    val failedUrl = request.url?.toString() ?: activeStreamUrl
                                    triggerStreamFailover(failedUrl, "Error de Red")
                                }
                            }
                            override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                val status = errorResponse?.statusCode ?: 200
                                val isMainFrame = request?.isForMainFrame == true
                                if (isMainFrame && status >= 400 && !isVideoReady) {
                                    val failedUrl = request?.url?.toString() ?: activeStreamUrl
                                    triggerStreamFailover(failedUrl, "Error HTTP $status")
                                }
                            }
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): android.webkit.WebResourceResponse? {
                                val u = request?.url?.toString()?.lowercase() ?: return null
                                val adKeywords = listOf(
                                    "adangle", "imasdk", "ima3.js", "ima-ad-player", "adsanalytics",
                                    "ggpick", "doubleclick", "googlesyndication", "popads", "propeller",
                                    "adsterra", "monetag", "exoclick", "juicyads", "trafficjunky",
                                    "adnxs", "bidswitch", "rubiconproject", "criteo", "taboola",
                                    "outbrain", "yandex.ru/ads", "adsystem", "adserver", "/vast",
                                    "vpaid", "delivery/ajs.php", "a.magsrv.com", "tsyndicate",
                                    "google-analytics", "googletagmanager", "histats", "statcounter",
                                    "scorecardresearch", "hotjar", "clarity.ms", "disqus", "crypto"
                                )
                                if (adKeywords.any { u.contains(it) }) {
                                    val corsHeaders = mapOf(
                                        "Access-Control-Allow-Origin" to "*",
                                        "Access-Control-Allow-Methods" to "GET, POST, OPTIONS, HEAD",
                                        "Access-Control-Allow-Headers" to "*"
                                    )
                                    return android.webkit.WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        200,
                                        "OK",
                                        corsHeaders,
                                        java.io.ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                val target = request?.url?.toString() ?: return false
                                val scheme = request.url?.scheme?.lowercase() ?: ""
                                if (scheme != "http" && scheme != "https") {
                                    return true // block non-web schemes (intent:, market:, etc.)
                                }
                                val adDomains = listOf(
                                    "adangle", "imasdk", "popads", "propeller", "adsterra", "monetag", "exoclick",
                                    "juicyads", "trafficjunky", "adnxs", "bidswitch", "criteo", "taboola", "outbrain",
                                    "bet365", "1xbet", "casino", "yandex.ru/ads", "delivery/ajs", "tsyndicate"
                                )
                                if (adDomains.any { target.lowercase().contains(it) }) {
                                    return true
                                }
                                return false
                            }
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isBuffering = true
                                if (!isVideoReady) {
                                    isVideoReady = false
                                }
                                view?.evaluateJavascript(
                                    "window.open = function() { return null; }; window.alert = function() {};",
                                    null
                                )
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val adBlockAndDeadCheckJs = """
                                (function() {
                                    // 1. Check dead stream
                                    var t = document.body ? (document.body.innerText || document.body.textContent || '').toLowerCase() : '';
                                    if (t.includes('232001') || t.includes('no se puede reproducir') || t.includes('archivo de video no se puede reproducir') || t.includes('video file cannot be played') || t.includes('error loading media') || t.includes('the media playback was aborted') || t.includes('error code:') || t.includes('no longer available') || t.includes('expired or has been deleted') || t.includes('file was deleted') || t.includes('video deleted') || t.includes('byse frontend') || t.includes('contenido no disponible')) {
                                        return 'DEAD';
                                    }
                                    
                                    // 2. Suppress popups, alerts, and navigation hijacking
                                    window.open = function() { return null; };
                                    window.alert = function() {};
                                    window.confirm = function() { return true; };
                                    
                                    // 3. Inject CSS removing ad overlays, banners, clickjackers, grey posters, AND native web player chrome
                                    var style = document.createElement('style');
                                    style.id = 'tvpro-adblock-style';
                                    style.innerHTML = `
                                        html, body, video, .jwplayer, .jw-media {
                                            background: #000 !important;
                                            background-color: #000 !important;
                                        }
                                        div.video_ad, div.video_ad_fadein, #over_player_msg, #adbd, #logo-vimeos,
                                        .jw-preview, .vjs-poster, .plyr__poster,
                                        .jw-ad-container, .jw-ad-controls, .ima-ad-container,
                                        .jw-controls, .jw-controlbar, .jw-display, .jw-display-icon-container,
                                        .jw-display-controls, .jw-title, .jw-logo, .jw-overlays, .jw-settings-menu,
                                        .jw-controls-backdrop, .jw-nextup-container, .jw-float-icon, .jw-rightclick,
                                        .jw-button-container, .jw-slider-time,
                                        .vjs-control-bar, .vjs-big-play-button, .vjs-title-bar, .vjs-modal-dialog,
                                        .plyr__controls, .plyr__control--overlaid, .fluid_controls_container, .media-control,
                                        [id*="ad-"], [class*="ad-"], [id*="banner"], [class*="banner"],
                                        a[href*="ggpick"], a[href*="adsanalytics"], iframe[src*="ad"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                            opacity: 0 !important;
                                            pointer-events: none !important;
                                            width: 0 !important;
                                            height: 0 !important;
                                            z-index: -9999 !important;
                                            background-image: none !important;
                                        }
                                        video::-webkit-media-controls,
                                        video::-webkit-media-controls-enclosure,
                                        video::-webkit-media-controls-panel,
                                        video::-webkit-media-controls-overlay-play-button {
                                            display: none !important;
                                            opacity: 0 !important;
                                            -webkit-appearance: none !important;
                                        }
                                    `;
                                    if (!document.getElementById('tvpro-adblock-style')) {
                                        (document.head || document.documentElement).appendChild(style);
                                    }
                                    
                                    // 4. Remove physical ad overlay nodes
                                    document.querySelectorAll('div.video_ad, div.video_ad_fadein, #over_player_msg, #adbd').forEach(function(el) {
                                        try { el.remove(); } catch(e){}
                                    });
                                    
                                    // 5. JWPlayer: Auto skip ads, auto play, and disable native controls
                                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                                        try {
                                            var p = window.jwplayer();
                                            if (p && typeof p.on === 'function') {
                                                p.on('adPlay', function() {
                                                    if (typeof p.skipAd === 'function') p.skipAd();
                                                    else if (typeof p.stopAd === 'function') p.stopAd();
                                                });
                                                p.on('adError', function() {
                                                    if (typeof p.play === 'function') p.play();
                                                });
                                                p.on('adBlock', function() {
                                                    if (typeof p.play === 'function') p.play();
                                                });
                                                p.on('error', function(err) {
                                                    if (window.TVProBridge && typeof window.TVProBridge.onPlayerDead === 'function') {
                                                        window.TVProBridge.onPlayerDead('JW Error 232001');
                                                    }
                                                });
                                                p.on('setupError', function(err) {
                                                    if (window.TVProBridge && typeof window.TVProBridge.onPlayerDead === 'function') {
                                                        window.TVProBridge.onPlayerDead('JW Setup Error');
                                                    }
                                                });
                                            }
                                            if (p && typeof p.play === 'function') p.play(true);
                                            if (p && typeof p.setControls === 'function') p.setControls(false);
                                        } catch(e) {}
                                    }
                                    
                                    // 6. Auto-start video & remove native HTML5 controls and poster
                                    var btn = document.querySelector('.jw-display-icon-container, .vjs-big-play-button, button.play, .play-button');
                                    if (btn) { btn.click(); }
                                    var v = document.querySelector('video');
                                    if (v) {
                                        v.controls = false;
                                        v.removeAttribute('controls');
                                        v.removeAttribute('poster');
                                        v.style.backgroundColor = '#000';
                                        v.addEventListener('error', function() {
                                            if (window.TVProBridge && typeof window.TVProBridge.onPlayerDead === 'function') {
                                                window.TVProBridge.onPlayerDead('HTML5 Video Error');
                                            }
                                        });
                                        if (v.paused) { v.play(); }
                                    }
                                    
                                    return 'OK';
                                })();
                                """.trimIndent()

                                view?.evaluateJavascript(adBlockAndDeadCheckJs) { result ->
                                    if (result != null && result.contains("DEAD")) {
                                        val failedUrl = url ?: activeStreamUrl
                                        triggerStreamFailover(failedUrl, "Servidor caído")
                                    }
                                }
                            }
                        }
                        fun loadTargetUrl(targetUrl: String) {
                            if (targetUrl.contains("vimeos", ignoreCase = true) || targetUrl.contains("lamovie", ignoreCase = true)) {
                                val iframeHtml = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <style>
                                            body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #000; }
                                            iframe { width: 100%; height: 100%; border: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <iframe src="$targetUrl" allowfullscreen="true" allow="autoplay; fullscreen; encrypted-media"></iframe>
                                    </body>
                                    </html>
                                """.trimIndent()
                                loadDataWithBaseURL("https://lamovie.org/", iframeHtml, "text/html", "UTF-8", null)
                            } else {
                                val headers = mapOf(
                                    "Referer" to "https://lamovie.org/",
                                    "Origin" to "https://lamovie.org"
                                )
                                loadUrl(targetUrl, headers)
                            }
                        }
                        loadTargetUrl(activeStreamUrl)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    if (loadedStreamUrl != activeStreamUrl) {
                        loadedStreamUrl = activeStreamUrl
                        if (activeStreamUrl.contains("vimeos", ignoreCase = true) || activeStreamUrl.contains("lamovie", ignoreCase = true)) {
                            val iframeHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <style>
                                        body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #000; }
                                        iframe { width: 100%; height: 100%; border: none; }
                                    </style>
                                </head>
                                <body>
                                    <iframe src="$activeStreamUrl" allowfullscreen="true" allow="autoplay; fullscreen; encrypted-media"></iframe>
                                </body>
                                </html>
                            """.trimIndent()
                            webView.loadDataWithBaseURL("https://lamovie.org/", iframeHtml, "text/html", "UTF-8", null)
                        } else {
                            val headers = mapOf(
                                "Referer" to "https://lamovie.org/",
                                "Origin" to "https://lamovie.org"
                            )
                            webView.loadUrl(activeStreamUrl, headers)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (isVideoReady) 1f else 0f }
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        keepScreenOn = true
                        isFocusable = false
                        isFocusableInTouchMode = false
                        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    }
                },
                update = { view ->
                    if (view.player != exoPlayer) {
                        view.player = exoPlayer
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (isVideoReady) 1f else 0f }
            )
        }

        // Transparent tap shield when controls are hidden: wakes up TVPro controls instead of touching WebView
        if (!showControls && !showServerDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { wakeUpControls() }
                        )
                    }
            )
        }

        // Full-Screen Initial Loading Splash until the video has actually loaded its first frame
        if (!isVideoReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07090E)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = NetflixRed,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(58.dp)
                )
            }
        }

        // Mid-playback Buffering Indicator (Clean floating spinner over the video without blocking the screen or controls)
        if (isVideoReady && isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NetflixRed,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Animated Seek Toast Bubble OSD
        seekToastText?.let { toastMsg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE610141D))
                    .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = toastMsg,
                    color = Color.White,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }



        // Audio & Subtitles Selection Modal (Estilo Cinema de Alta Gama)
        if (showServerDialog) {
            val warmIvory = Color(0xFFF5F2EB)
            val audioScrollState = rememberScrollState()
            val subScrollState = rememberScrollState()
            val targetFocusIdx = currentStreamIndex.coerceIn(0, (availableStreams.size - 1).coerceAtLeast(0))

            val hasSubtitles = if (isEmbedStream) {
                nativeWebSubtitleTracks.isNotEmpty() || (currentStream.subtitle == 1) || currentStream.safeSubtitles.isNotEmpty()
            } else {
                exoSubtitleTracks.isNotEmpty() || (currentStream.subtitle == 1) || currentStream.safeSubtitles.isNotEmpty()
            }

            LaunchedEffect(Unit) {
                audioScrollState.scrollTo(0)
                subScrollState.scrollTo(0)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF206080C))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showServerDialog = false })
                    }
                    .padding(horizontal = 56.dp, vertical = 42.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 940.dp)
                ) {
                    // Header superior del modal: AUDIO Y SUBTÍTULOS  |  Estado actual
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = warmIvory,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "AUDIO Y SUBTÍTULOS",
                                    color = warmIvory,
                                    fontFamily = BebasNeue,
                                    fontSize = 32.sp,
                                    letterSpacing = 2.2.sp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "·   CONFIGURACIÓN DE PISTAS",
                                    color = Color(0xFF9CA3AF),
                                    fontFamily = Inter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.0.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Selecciona el idioma de audio preferido o activa y desactiva los subtítulos disponibles en este video.",
                                color = Color(0xFF9CA3AF),
                                fontFamily = Inter,
                                fontSize = 13.5.sp
                            )
                        }

                        // Píldora derecha: Estado de Subtítulos dinámico
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF10141D))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !hasSubtitles -> Color(0xFF6B7280)
                                        subtitlesEnabled -> Color(0xFF2DD4BF)
                                        else -> Color(0xFF6B7280)
                                    }
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when {
                                    !hasSubtitles -> "SUBTÍTULOS: NO DISPONIBLES"
                                    subtitlesEnabled -> "SUBTÍTULOS: ACTIVADOS"
                                    else -> "SUBTÍTULOS: DESACTIVADOS"
                                },
                                color = warmIvory,
                                fontFamily = Inter,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Diseño en 2 Columnas: Izquierda (IDIOMA Y AUDIO) | Derecha (SUBTÍTULOS)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // COLUMNA 1: IDIOMA Y PISTAS DE AUDIO
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(355.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0F15))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(22.dp)
                            ) {
                                Text(
                                    text = "IDIOMA Y AUDIO",
                                    color = Color(0xFF9CA3AF),
                                    fontFamily = Inter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.0.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(audioScrollState),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val displayAudioOptions = remember(
                                        exoAudioTracks,
                                        nativeWebAudioTracks,
                                        selectedWebAudioTrackIdx,
                                        isEmbedStream,
                                        availableStreams,
                                        currentStreamIndex,
                                        currentStream
                                    ) {
                                        val options = mutableListOf<DisplayLanguageOption>()

                                        fun isStreamSpanish(s: StreamItem): Boolean {
                                            val src = (s.source ?: "").lowercase()
                                            val isExplicitEnglish = src.contains("subtitulado") || src.contains("subtitulos") ||
                                                src.contains("inglés") || src.contains("ingles") || src.contains("english") || src.contains("vo ")
                                            if (isExplicitEnglish && !src.contains("dual") && !src.contains("multi")) return false

                                            if (src.contains("latino") || src.contains("castellano") || src.contains("español") || src.contains("espanol") || src.contains("spanish")) {
                                                return true
                                            }
                                            val urlLower = s.url.lowercase()
                                            if ((urlLower.contains("latino") || urlLower.contains("castellano")) && !urlLower.contains("sub_en")) return true
                                            return false
                                        }

                                        fun isStreamEnglish(s: StreamItem): Boolean {
                                            val src = (s.source ?: "").lowercase()
                                            val isExplicitSpanish = (src.contains("latino") || src.contains("castellano") || src.contains("español") || src.contains("espanol")) &&
                                                !src.contains("dual") && !src.contains("multi")
                                            if (isExplicitSpanish) return false

                                            if (src.contains("subtitulado") || src.contains("subtitulos") || src.contains("inglés") || src.contains("ingles") || src.contains("english") || src.contains("original")) {
                                                return true
                                            }
                                            val urlLower = s.url.lowercase()
                                            if (urlLower.contains("english") || urlLower.contains("subtitulado") || urlLower.contains("sub_en")) return true
                                            return false
                                        }

                                        fun isWebSpanish(label: String): Boolean {
                                            val raw = label.lowercase()
                                            return raw.contains("es") || raw.contains("spa") || raw.contains("lat") || raw.contains("castell")
                                        }

                                        fun isWebEnglish(label: String): Boolean {
                                            val raw = label.lowercase()
                                            return raw.contains("en") || raw.contains("eng") || raw.contains("orig") || raw.contains("ing")
                                        }

                                        // 1. Pistas nativas de ExoPlayer
                                        if (!isEmbedStream && exoAudioTracks.isNotEmpty()) {
                                            val exoSpanish = exoAudioTracks.filter { it.isSpanish }
                                            val exoEnglish = exoAudioTracks.filter { it.isEnglish }
                                            val exoOther = exoAudioTracks.filter { !it.isSpanish && !it.isEnglish }

                                            if (exoSpanish.isNotEmpty()) {
                                                exoSpanish.forEach { track ->
                                                    options.add(
                                                        DisplayLanguageOption(
                                                            id = "exo_es_${track.trackIndex}",
                                                            title = track.name,
                                                            subtitle = if (exoAudioTracks.size == 1) "Doblaje oficial latino  ·  Audio integrado" else "Doblaje Español Latino  ·  Audio Nativo",
                                                            isSelected = track.isSelected,
                                                            onSelect = {
                                                                userSelectedAudioTrack = true
                                                                try {
                                                                    val override = TrackSelectionOverride(
                                                                        track.group.mediaTrackGroup,
                                                                        listOf(track.trackIndex)
                                                                    )
                                                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                                        .buildUpon()
                                                                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                                        .addOverride(override)
                                                                        .build()
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            }
                                                        )
                                                    )
                                                }
                                            } else {
                                                val spanishStreamIdx = availableStreams.indexOfFirst { isStreamSpanish(it) }
                                                if (spanishStreamIdx >= 0) {
                                                    options.add(
                                                        DisplayLanguageOption(
                                                            id = "stream_es_$spanishStreamIdx",
                                                            title = "Español Latino",
                                                            subtitle = "Doblaje oficial latino  ·  Transmisión HD",
                                                            isSelected = (currentStreamIndex == spanishStreamIdx),
                                                            onSelect = {
                                                                if (currentStreamIndex != spanishStreamIdx) {
                                                                    switchServer(spanishStreamIdx, resetPosition = false)
                                                                }
                                                            }
                                                        )
                                                    )
                                                }
                                            }

                                            if (exoEnglish.isNotEmpty()) {
                                                exoEnglish.forEach { track ->
                                                    options.add(
                                                        DisplayLanguageOption(
                                                            id = "exo_en_${track.trackIndex}",
                                                            title = track.name,
                                                            subtitle = if (exoAudioTracks.size == 1) "Audio original en inglés  ·  Audio integrado" else "Audio Original / Inglés  ·  Audio Nativo",
                                                            isSelected = track.isSelected,
                                                            onSelect = {
                                                                userSelectedAudioTrack = true
                                                                try {
                                                                    val override = TrackSelectionOverride(
                                                                        track.group.mediaTrackGroup,
                                                                        listOf(track.trackIndex)
                                                                    )
                                                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                                        .buildUpon()
                                                                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                                        .addOverride(override)
                                                                        .build()
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            }
                                                        )
                                                    )
                                                }
                                            } else {
                                                val englishStreamIdx = availableStreams.indexOfFirst { isStreamEnglish(it) }
                                                if (englishStreamIdx >= 0) {
                                                    options.add(
                                                        DisplayLanguageOption(
                                                            id = "stream_en_$englishStreamIdx",
                                                            title = "Inglés",
                                                            subtitle = "Audio original en inglés  ·  Transmisión HD",
                                                            isSelected = (currentStreamIndex == englishStreamIdx),
                                                            onSelect = {
                                                                if (currentStreamIndex != englishStreamIdx) {
                                                                    switchServer(englishStreamIdx, resetPosition = false)
                                                                }
                                                            }
                                                        )
                                                    )
                                                }
                                            }

                                            exoOther.forEach { track ->
                                                val isGeneric = track.name.startsWith("Audio ", ignoreCase = true) ||
                                                    track.name.equals("und", ignoreCase = true) ||
                                                    track.name.isBlank()
                                                if (!isGeneric) {
                                                    options.add(
                                                        DisplayLanguageOption(
                                                            id = "exo_other_${track.trackIndex}",
                                                            title = track.name,
                                                            subtitle = if (track.name.isNotBlank()) "Audio en ${track.name.lowercase()}  ·  Audio Nativo" else "Pista de audio adicional  ·  Audio Nativo",
                                                            isSelected = track.isSelected,
                                                            onSelect = {
                                                                userSelectedAudioTrack = true
                                                                try {
                                                                    val override = TrackSelectionOverride(
                                                                        track.group.mediaTrackGroup,
                                                                        listOf(track.trackIndex)
                                                                    )
                                                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                                        .buildUpon()
                                                                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                                        .addOverride(override)
                                                                        .build()
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            }
                                                        )
                                                    )
                                                }
                                            }
                                        } else if (isEmbedStream && nativeWebAudioTracks.isNotEmpty()) {
                                            var webSpanishIdx = nativeWebAudioTracks.indexOfFirst { isWebSpanish(it) }
                                            var webEnglishIdx = nativeWebAudioTracks.indexOfFirst { isWebEnglish(it) }

                                            if (webSpanishIdx < 0 && webEnglishIdx < 0 && nativeWebAudioTracks.size == 1) {
                                                if (isStreamEnglish(currentStream)) webEnglishIdx = 0 else webSpanishIdx = 0
                                            }

                                            if (webSpanishIdx >= 0) {
                                                options.add(
                                                    DisplayLanguageOption(
                                                        id = "web_es_$webSpanishIdx",
                                                        title = "Español Latino",
                                                        subtitle = "Doblaje Español Latino  ·  Audio Web",
                                                        isSelected = (selectedWebAudioTrackIdx == webSpanishIdx),
                                                        onSelect = {
                                                            userSelectedAudioTrack = true
                                                            applyWebAudioTrack(webSpanishIdx)
                                                        }
                                                    )
                                                )
                                            } else {
                                                val spanishStreamIdx = availableStreams.indexOfFirst { isStreamSpanish(it) }
                                                if (spanishStreamIdx >= 0) {
                                                    options.add(
                                                        DisplayLanguageOption(
                                                            id = "stream_es_$spanishStreamIdx",
                                                            title = "Español Latino",
                                                            subtitle = "Doblaje oficial latino  ·  Transmisión HD",
                                                            isSelected = (currentStreamIndex == spanishStreamIdx),
                                                            onSelect = {
                                                                if (currentStreamIndex != spanishStreamIdx) {
                                                                    switchServer(spanishStreamIdx, resetPosition = false)
                                                                }
                                                            }
                                                        )
                                                    )
                                                }
                                            }

                                            if (webEnglishIdx >= 0) {
                                                options.add(
                                                    DisplayLanguageOption(
                                                        id = "web_en_$webEnglishIdx",
                                                        title = "Inglés",
                                                        subtitle = "Audio Original en Inglés  ·  Audio Web",
                                                        isSelected = (selectedWebAudioTrackIdx == webEnglishIdx),
                                                        onSelect = {
                                                            userSelectedAudioTrack = true
                                                            applyWebAudioTrack(webEnglishIdx)
                                                        }
                                                    )
                                                )
                                            } else {
                                                val englishStreamIdx = availableStreams.indexOfFirst { isStreamEnglish(it) }
                                                if (englishStreamIdx >= 0) {
                                                    options.add(
                                                        DisplayLanguageOption(
                                                            id = "stream_en_$englishStreamIdx",
                                                            title = "Inglés",
                                                            subtitle = "Audio original en inglés  ·  Transmisión HD",
                                                            isSelected = (currentStreamIndex == englishStreamIdx),
                                                            onSelect = {
                                                                if (currentStreamIndex != englishStreamIdx) {
                                                                    switchServer(englishStreamIdx, resetPosition = false)
                                                                }
                                                            }
                                                        )
                                                    )
                                                }
                                            }

                                            nativeWebAudioTracks.forEachIndexed { idx, label ->
                                                if (idx != webSpanishIdx && idx != webEnglishIdx) {
                                                    val isGeneric = label.startsWith("Audio ", ignoreCase = true) ||
                                                        label.equals("und", ignoreCase = true) ||
                                                        label.isBlank()
                                                    if (!isGeneric) {
                                                        options.add(
                                                            DisplayLanguageOption(
                                                                id = "web_other_$idx",
                                                                title = label,
                                                                subtitle = "Pista de Audio Web",
                                                                isSelected = (selectedWebAudioTrackIdx == idx),
                                                                onSelect = {
                                                                    userSelectedAudioTrack = true
                                                                    applyWebAudioTrack(idx)
                                                                }
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            // Pista única identificada de la fuente activa o embed sin pistas expuestas
                                            val currentIsExplicitEnglish = isStreamEnglish(currentStream)
                                            val currentIsExplicitSpanish = isStreamSpanish(currentStream)

                                            // Strictly mutually exclusive: only ONE language can be active at any given moment
                                            val isEnglishActive = currentIsExplicitEnglish && !currentIsExplicitSpanish
                                            val isSpanishActive = !isEnglishActive

                                            val spanishStreamIdx = if (isSpanishActive) currentStreamIndex else availableStreams.indexOfFirst { isStreamSpanish(it) }
                                            val hasSpanish = isSpanishActive || spanishStreamIdx >= 0

                                            val englishStreamIdx = if (isEnglishActive) currentStreamIndex else availableStreams.indexOfFirst { isStreamEnglish(it) }
                                            val hasEnglish = isEnglishActive || englishStreamIdx >= 0

                                            if (hasSpanish) {
                                                options.add(
                                                    DisplayLanguageOption(
                                                        id = "fallback_es",
                                                        title = "Español Latino",
                                                        subtitle = if (isSpanishActive) "Doblaje oficial latino  ·  Audio integrado" else "Doblaje oficial latino",
                                                        isSelected = isSpanishActive,
                                                        onSelect = {
                                                            if (spanishStreamIdx >= 0 && spanishStreamIdx != currentStreamIndex) {
                                                                switchServer(spanishStreamIdx, resetPosition = false)
                                                            }
                                                        }
                                                    )
                                                )
                                            }

                                            if (hasEnglish) {
                                                options.add(
                                                    DisplayLanguageOption(
                                                        id = "fallback_en",
                                                        title = "Inglés",
                                                        subtitle = if (isEnglishActive) "Audio original en inglés  ·  Audio integrado" else "Audio original en inglés",
                                                        isSelected = isEnglishActive,
                                                        onSelect = {
                                                            if (englishStreamIdx >= 0 && englishStreamIdx != currentStreamIndex) {
                                                                switchServer(englishStreamIdx, resetPosition = false)
                                                            }
                                                        }
                                                    )
                                                )
                                            }

                                            if (options.isEmpty()) {
                                                options.add(
                                                    DisplayLanguageOption(
                                                        id = "fallback_single",
                                                        title = "Español Latino",
                                                        subtitle = "Audio estéreo integrado de la transmisión",
                                                        isSelected = true,
                                                        onSelect = {}
                                                    )
                                                )
                                            }
                                        }

                                        // Deduplicate by title: if two options have the same title (e.g. "Español Latino"),
                                        // keep only one, preferring the one that is currently active (isSelected = true).
                                        val deduplicated = mutableListOf<DisplayLanguageOption>()
                                        for (opt in options) {
                                            val existingIdx = deduplicated.indexOfFirst { it.title.trim().equals(opt.title.trim(), ignoreCase = true) }
                                            if (existingIdx >= 0) {
                                                if (!deduplicated[existingIdx].isSelected && opt.isSelected) {
                                                    deduplicated[existingIdx] = opt
                                                }
                                            } else {
                                                deduplicated.add(opt)
                                            }
                                        }

                                        // Ensure at least one option is selected if any exists
                                        if (deduplicated.isNotEmpty() && deduplicated.none { it.isSelected }) {
                                            deduplicated[0] = deduplicated[0].copy(isSelected = true)
                                        }

                                        // Strict mutual exclusivity: ensure NEVER more than one option is marked as ACTIVO (isSelected = true)
                                        var hasActiveTrack = false
                                        val finalOptions = deduplicated.map { opt ->
                                            if (opt.isSelected) {
                                                if (!hasActiveTrack) {
                                                    hasActiveTrack = true
                                                    opt
                                                } else {
                                                    opt.copy(isSelected = false)
                                                }
                                            } else {
                                                opt
                                            }
                                        }
                                        finalOptions
                                    }

                                    displayAudioOptions.forEachIndexed { optIdx, option ->
                                        var isOptFocused by remember { mutableStateOf(false) }
                                        val shouldFocusThisOpt = if (displayAudioOptions.any { it.isSelected }) option.isSelected else (optIdx == 0)

                                        Button(
                                            onClick = {
                                                option.onSelect()
                                                showServerDialog = false
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (shouldFocusThisOpt) Modifier.focusRequester(activeAudioTrackFocusRequester)
                                                    else Modifier
                                                )
                                                .border(
                                                    border = if (isOptFocused) BorderStroke(2.dp, warmIvory)
                                                    else if (option.isSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                    else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .onFocusChanged { isOptFocused = it.isFocused }
                                                .onKeyEvent { keyEvent ->
                                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                    ) {
                                                        option.onSelect()
                                                        showServerDialog = false
                                                        true
                                                    } else false
                                                },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isOptFocused) warmIvory
                                                else if (option.isSelected) Color(0xFF171C26)
                                                else Color(0xFF11151E),
                                                contentColor = if (isOptFocused) Color.Black else warmIvory
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = option.title,
                                                        fontFamily = Inter,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = if (isOptFocused) Color.Black else warmIvory
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = option.subtitle,
                                                        fontFamily = Inter,
                                                        fontSize = 11.sp,
                                                        color = if (isOptFocused) Color.Black.copy(alpha = 0.7f) else Color(0xFF9CA3AF)
                                                    )
                                                }
                                                if (option.isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .background(if (isOptFocused) Color(0xFF0A0D13) else warmIvory)
                                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = "ACTIVO",
                                                            fontFamily = Inter,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (isOptFocused) warmIvory else Color(0xFF0A0D13),
                                                            fontSize = 9.5.sp,
                                                            letterSpacing = 1.0.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // COLUMNA 2: SUBTÍTULOS (ACTIVAR / DESACTIVAR)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(355.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0F15))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(22.dp)
                            ) {
                                Text(
                                    text = "SUBTÍTULOS (CC)",
                                    color = Color(0xFF9CA3AF),
                                    fontFamily = Inter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.0.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                if (!hasSubtitles) {
                                    // Estado Elegante de Sin Subtítulos
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF10141D))
                                            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF181E2B)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Subtitles,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6B7280),
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "SIN SUBTÍTULOS EN ESTE VIDEO",
                                                color = warmIvory,
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                letterSpacing = 1.2.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Esta transmisión cuenta con audio integrado y no incluye pistas de subtítulos en esta fuente.",
                                                color = Color(0xFF9CA3AF),
                                                fontFamily = Inter,
                                                fontSize = 11.5.sp,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                } else {
                                    // Lista dinámica de subtítulos
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(subScrollState),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Opción 1: Desactivados
                                        var isOffFocused by remember { mutableStateOf(false) }
                                        val isOffSelected = !subtitlesEnabled

                                        Button(
                                            onClick = {
                                                if (isEmbedStream) {
                                                    applyWebSubtitle(enabled = false, trackIdx = -1)
                                                } else {
                                                    selectExoSubtitle(null)
                                                }
                                                showServerDialog = false
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    border = if (isOffFocused) BorderStroke(2.dp, warmIvory)
                                                    else if (isOffSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                    else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .onFocusChanged { isOffFocused = it.isFocused }
                                                .onKeyEvent { keyEvent ->
                                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                                        (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                    ) {
                                                        if (isEmbedStream) {
                                                            applyWebSubtitle(enabled = false, trackIdx = -1)
                                                        } else {
                                                            selectExoSubtitle(null)
                                                        }
                                                        showServerDialog = false
                                                        true
                                                    } else false
                                                },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isOffFocused) warmIvory
                                                else if (isOffSelected) Color(0xFF171C26)
                                                else Color(0xFF11151E),
                                                contentColor = if (isOffFocused) Color.Black else warmIvory
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Desactivados",
                                                        fontFamily = Inter,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = if (isOffFocused) Color.Black else warmIvory
                                                    )
                                                    Text(
                                                        text = "Ocultar los subtítulos en pantalla",
                                                        fontFamily = Inter,
                                                        fontSize = 11.sp,
                                                        color = if (isOffFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                                    )
                                                }
                                                if (isOffSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .background(if (isOffFocused) Color(0xFF0A0D13) else warmIvory)
                                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = "ACTIVO",
                                                            fontFamily = Inter,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (isOffFocused) warmIvory else Color(0xFF0A0D13),
                                                            fontSize = 9.5.sp,
                                                            letterSpacing = 1.0.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Pistas de ExoPlayer
                                        if (!isEmbedStream) {
                                            if (exoSubtitleTracks.isNotEmpty()) {
                                                exoSubtitleTracks.forEach { subItem ->
                                                    var isItemSubFocused by remember { mutableStateOf(false) }
                                                    val isItemSubSelected = subtitlesEnabled && subItem.isSelected

                                                    Button(
                                                        onClick = {
                                                            selectExoSubtitle(subItem)
                                                            showServerDialog = false
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .border(
                                                                border = if (isItemSubFocused) BorderStroke(2.dp, warmIvory)
                                                                else if (isItemSubSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .onFocusChanged { isItemSubFocused = it.isFocused }
                                                            .onKeyEvent { keyEvent ->
                                                                if (keyEvent.type == KeyEventType.KeyDown &&
                                                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                                ) {
                                                                    selectExoSubtitle(subItem)
                                                                    showServerDialog = false
                                                                    true
                                                                } else false
                                                            },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isItemSubFocused) warmIvory
                                                            else if (isItemSubSelected) Color(0xFF171C26)
                                                            else Color(0xFF11151E),
                                                            contentColor = if (isItemSubFocused) Color.Black else warmIvory
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = subItem.name,
                                                                    fontFamily = Inter,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 14.sp,
                                                                    color = if (isItemSubFocused) Color.Black else warmIvory
                                                                )
                                                                Text(
                                                                    text = when {
                                                                        subItem.isSpanish -> "Subtítulos en español sincronizados"
                                                                        subItem.name.contains("portug", ignoreCase = true) -> "Subtítulos en portugués sincronizados"
                                                                        subItem.isEnglish -> "Subtítulos en inglés sincronizados"
                                                                        else -> "Subtítulos en idioma original"
                                                                    },
                                                                    fontFamily = Inter,
                                                                    fontSize = 11.sp,
                                                                    color = if (isItemSubFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                                                )
                                                            }
                                                            if (isItemSubSelected) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(50))
                                                                        .background(if (isItemSubFocused) Color(0xFF0A0D13) else warmIvory)
                                                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "ACTIVO",
                                                                        fontFamily = Inter,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = if (isItemSubFocused) warmIvory else Color(0xFF0A0D13),
                                                                        fontSize = 9.5.sp,
                                                                        letterSpacing = 1.0.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (currentStream.safeSubtitles.isNotEmpty()) {
                                                currentStream.safeSubtitles.forEachIndexed { sIdx, subData ->
                                                    var isItemSubFocused by remember { mutableStateOf(false) }
                                                    val isItemSubSelected = subtitlesEnabled && (
                                                        if (subData.language.startsWith("en", ignoreCase = true)) selectedSubtitleOption == 2
                                                        else selectedSubtitleOption == 1 || selectedSubtitleOption == (sIdx + 1)
                                                    )

                                                    Button(
                                                        onClick = {
                                                            selectSubtitleTrackData(subData)
                                                            showServerDialog = false
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .border(
                                                                border = if (isItemSubFocused) BorderStroke(2.dp, warmIvory)
                                                                else if (isItemSubSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .onFocusChanged { isItemSubFocused = it.isFocused }
                                                            .onKeyEvent { keyEvent ->
                                                                if (keyEvent.type == KeyEventType.KeyDown &&
                                                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                                ) {
                                                                    selectSubtitleTrackData(subData)
                                                                    showServerDialog = false
                                                                    true
                                                                } else false
                                                            },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isItemSubFocused) warmIvory
                                                            else if (isItemSubSelected) Color(0xFF171C26)
                                                            else Color(0xFF11151E),
                                                            contentColor = if (isItemSubFocused) Color.Black else warmIvory
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = subData.label,
                                                                    fontFamily = Inter,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 14.sp,
                                                                    color = if (isItemSubFocused) Color.Black else warmIvory
                                                                )
                                                                Text(
                                                                    text = when {
                                                                        subData.language.startsWith("es", ignoreCase = true) -> "Subtítulos en español sincronizados"
                                                                        subData.language.startsWith("pt", ignoreCase = true) || subData.label.contains("portug", ignoreCase = true) -> "Subtítulos en portugués sincronizados"
                                                                        subData.language.startsWith("en", ignoreCase = true) -> "Subtítulos en inglés sincronizados"
                                                                        else -> "Subtítulos en idioma original"
                                                                    },
                                                                    fontFamily = Inter,
                                                                    fontSize = 11.sp,
                                                                    color = if (isItemSubFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                                                )
                                                            }
                                                            if (isItemSubSelected) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(50))
                                                                        .background(if (isItemSubFocused) Color(0xFF0A0D13) else warmIvory)
                                                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "ACTIVO",
                                                                        fontFamily = Inter,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = if (isItemSubFocused) warmIvory else Color(0xFF0A0D13),
                                                                        fontSize = 9.5.sp,
                                                                        letterSpacing = 1.0.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Pistas de Reproductor Web
                                            if (nativeWebSubtitleTracks.isNotEmpty()) {
                                                nativeWebSubtitleTracks.forEachIndexed { sIdx, subLabel ->
                                                    var isWebSubFocused by remember { mutableStateOf(false) }
                                                    val isWebSubSelected = subtitlesEnabled && (selectedWebSubtitleTrackIdx == sIdx)

                                                    Button(
                                                        onClick = {
                                                            applyWebSubtitle(enabled = true, trackIdx = sIdx)
                                                            showServerDialog = false
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .border(
                                                                border = if (isWebSubFocused) BorderStroke(2.dp, warmIvory)
                                                                else if (isWebSubSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .onFocusChanged { isWebSubFocused = it.isFocused }
                                                            .onKeyEvent { keyEvent ->
                                                                if (keyEvent.type == KeyEventType.KeyDown &&
                                                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                                ) {
                                                                    applyWebSubtitle(enabled = true, trackIdx = sIdx)
                                                                    showServerDialog = false
                                                                    true
                                                                } else false
                                                            },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isWebSubFocused) warmIvory
                                                            else if (isWebSubSelected) Color(0xFF171C26)
                                                            else Color(0xFF11151E),
                                                            contentColor = if (isWebSubFocused) Color.Black else warmIvory
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = subLabel,
                                                                    fontFamily = Inter,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 14.sp,
                                                                    color = if (isWebSubFocused) Color.Black else warmIvory
                                                                )
                                                                Text(
                                                                    text = "Pista de subtítulos del reproductor",
                                                                    fontFamily = Inter,
                                                                    fontSize = 11.sp,
                                                                    color = if (isWebSubFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                                                )
                                                            }
                                                            if (isWebSubSelected) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(50))
                                                                        .background(if (isWebSubFocused) Color(0xFF0A0D13) else warmIvory)
                                                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "ACTIVO",
                                                                        fontFamily = Inter,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = if (isWebSubFocused) warmIvory else Color(0xFF0A0D13),
                                                                        fontSize = 9.5.sp,
                                                                        letterSpacing = 1.0.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (currentStream.subtitle == 1 || currentStream.safeSubtitles.isNotEmpty()) {
                                                val webFallbackSubs = if (currentStream.safeSubtitles.isNotEmpty()) {
                                                    currentStream.safeSubtitles
                                                } else {
                                                    listOf(SubtitleTrackData(url = "", label = "Español", language = "es"))
                                                }
                                                webFallbackSubs.forEachIndexed { sIdx, subData ->
                                                    var isApiSubFocused by remember { mutableStateOf(false) }
                                                    val isApiSubSelected = subtitlesEnabled && (
                                                        if (subData.language.startsWith("en", ignoreCase = true)) selectedSubtitleOption == 2
                                                        else selectedSubtitleOption == 1 || selectedSubtitleOption == (sIdx + 1)
                                                    )

                                                    Button(
                                                        onClick = {
                                                            applySubtitlePreference(
                                                                enabled = true,
                                                                optionIdx = if (subData.language.startsWith("en", ignoreCase = true)) 2 else 1
                                                            )
                                                            showServerDialog = false
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .border(
                                                                border = if (isApiSubFocused) BorderStroke(2.dp, warmIvory)
                                                                else if (isApiSubSelected) BorderStroke(1.5.dp, warmIvory.copy(alpha = 0.55f))
                                                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .onFocusChanged { isApiSubFocused = it.isFocused }
                                                            .onKeyEvent { keyEvent ->
                                                                if (keyEvent.type == KeyEventType.KeyDown &&
                                                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                                ) {
                                                                    applySubtitlePreference(
                                                                        enabled = true,
                                                                        optionIdx = if (subData.language.startsWith("en", ignoreCase = true)) 2 else 1
                                                                    )
                                                                    showServerDialog = false
                                                                    true
                                                                } else false
                                                            },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isApiSubFocused) warmIvory
                                                            else if (isApiSubSelected) Color(0xFF171C26)
                                                            else Color(0xFF11151E),
                                                            contentColor = if (isApiSubFocused) Color.Black else warmIvory
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = subData.label,
                                                                    fontFamily = Inter,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 14.sp,
                                                                    color = if (isApiSubFocused) Color.Black else warmIvory
                                                                )
                                                                Text(
                                                                    text = if (subData.language.startsWith("es", ignoreCase = true)) "Subtítulos en español sincronizados" else "Subtítulos en idioma original",
                                                                    fontFamily = Inter,
                                                                    fontSize = 11.sp,
                                                                    color = if (isApiSubFocused) Color.Black.copy(alpha = 0.72f) else Color(0xFF9CA3AF)
                                                                )
                                                            }
                                                            if (isApiSubSelected) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(50))
                                                                        .background(if (isApiSubFocused) Color(0xFF0A0D13) else warmIvory)
                                                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "ACTIVO",
                                                                        fontFamily = Inter,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = if (isApiSubFocused) warmIvory else Color(0xFF0A0D13),
                                                                        fontSize = 9.5.sp,
                                                                        letterSpacing = 1.0.sp
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

                                Spacer(modifier = Modifier.height(10.dp))
                                var isCloseFocused by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { showServerDialog = false },
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .height(38.dp)
                                        .onFocusChanged { isCloseFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown &&
                                                (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                            ) {
                                                showServerDialog = false
                                                true
                                            } else false
                                        },
                                    border = if (isCloseFocused) BorderStroke(2.dp, warmIvory)
                                    else BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCloseFocused) warmIvory else Color(0xFF141822),
                                        contentColor = if (isCloseFocused) Color.Black else warmIvory
                                    ),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "CERRAR",
                                        fontFamily = Inter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chapters / Episodes Selection Modal (Estilo Cinema de Alta Gama como en la imagen de referencia)
        if (showEpisodesDialog) {
            val warmIvory = Color(0xFFF5F2EB)
            val isSeriesContent = remember(detailResponse, movie) {
                detailResponse?.isSeries == true ||
                    movie.type == "series" || movie.type == "anime" || movie.type == "drama" ||
                    movie.url.contains("/tvshows/") || movie.url.contains("/series/") || movie.url.contains("/animes/") ||
                    currentMovieTitle.contains(Regex("""- T\d+:E\d+""", RegexOption.IGNORE_CASE))
            }
            val totalSeasons = maxOf(
                (detailResponse?.seasonsCount ?: 1),
                detailResponse?.postId?.toIntOrNull()?.let { MovieRepository.getSeasonsCount(it) } ?: 1,
                seasonEpisodes.maxOfOrNull { it.seasonNumber } ?: 1
            ).coerceAtLeast(1)
            val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image

            val modalEpisodes = remember(seasonEpisodes, isSeriesContent, cleanMainTitle, defaultBackdrop, selectedModalSeason) {
                if (seasonEpisodes.isNotEmpty() && (!isSeriesContent || seasonEpisodes.firstOrNull()?.seasonNumber == selectedModalSeason)) {
                    seasonEpisodes
                } else if (isSeriesContent) {
                    val epNames = listOf(
                        "El Comienzo",
                        "Sombras del Pasado",
                        "Punto de Quiebre",
                        "En la Oscuridad",
                        "Territorio Hostil",
                        "Revelaciones",
                        "Sin Retorno",
                        "El Desenlace"
                    )
                    epNames.mapIndexed { idx, name ->
                        val num = idx + 1
                        EpisodeItem(
                            id = -(selectedModalSeason * 100 + num),
                            episodeNumber = num,
                            seasonNumber = selectedModalSeason,
                            title = name,
                            overview = "Capítulo $num de la Temporada $selectedModalSeason de $cleanMainTitle disponible en alta definición.",
                            image = defaultBackdrop,
                            runtime = "${44 + (idx % 4) * 3} min"
                        )
                    }
                } else {
                    val sceneTitles = listOf(
                        "Prólogo e Introducción",
                        "El Primer Giro",
                        "Punto de No Retorno",
                        "Clímax Principal",
                        "Desenlace Final"
                    )
                    val sceneDescs = listOf(
                        "Apertura cinematográfica y presentación de los protagonistas en $cleanMainTitle.",
                        "Los acontecimientos se aceleran y el conflicto central toma forma.",
                        "La tensión alcanza su punto crítico con decisiones irreversibles.",
                        "Secuencia cumbre de acción y confrontación definitiva.",
                        "Resolución de la historia y secuencia final de cierre."
                    )
                    val sceneRuntimes = listOf("24 min", "34 min", "43 min", "33 min", "28 min")
                    sceneTitles.mapIndexed { idx, name ->
                        EpisodeItem(
                            id = -(idx + 1),
                            episodeNumber = idx + 1,
                            seasonNumber = 1,
                            title = name,
                            overview = sceneDescs[idx],
                            image = defaultBackdrop,
                            runtime = sceneRuntimes[idx]
                        )
                    }
                }
            }

            val displayPosMs = if (isEmbedStream) currentPositionMs else exoPlayer.currentPosition.coerceAtLeast(0L)
            val progressFraction = if (durationMs > 0L) {
                (displayPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val completedPercent = (progressFraction * 100f).toInt().coerceIn(0, 100)

            val activeCardIndex = remember(modalEpisodes, isSeriesContent, activeSeasonInt, activeEpisodeInt, progressFraction) {
                if (isSeriesContent) {
                    if (selectedModalSeason == activeSeasonInt) {
                        modalEpisodes.indexOfFirst { it.episodeNumber == activeEpisodeInt }.coerceAtLeast(0)
                    } else 0
                } else {
                    ((progressFraction * modalEpisodes.size).toInt()).coerceIn(0, (modalEpisodes.size - 1).coerceAtLeast(0))
                }
            }

            val lazyListState = rememberLazyListState()
            LaunchedEffect(activeCardIndex, selectedModalSeason) {
                try {
                    lazyListState.scrollToItem((activeCardIndex - 1).coerceAtLeast(0))
                    delay(80)
                    activeEpisodeFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF206080C))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showEpisodesDialog = false })
                    }
                    .padding(horizontal = 46.dp, vertical = 38.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header superior del modal: CAPÍTULOS · X EN TOTAL  |  Selector de Temporada  |  ACTIVO: CAPÍTULO 0X
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = null,
                                    tint = warmIvory,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isSeriesContent) "EPISODIOS" else "ESCENAS",
                                    color = warmIvory,
                                    fontFamily = BebasNeue,
                                    fontSize = 32.sp,
                                    letterSpacing = 2.2.sp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "·   ${modalEpisodes.size} ${if (isSeriesContent) "EPISODIOS EN TOTAL" else "ESCENAS EN TOTAL"}",
                                    color = Color(0xFF9CA3AF),
                                    fontFamily = Inter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.0.sp
                                )

                                // Selector de Temporadas cuando la serie tiene más de 1 temporada
                                if (isSeriesContent && totalSeasons > 1) {
                                    Spacer(modifier = Modifier.width(24.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (s in 1..totalSeasons.coerceAtMost(60)) {
                                            var isSeasonFocused by remember { mutableStateOf(false) }
                                            val isSelected = (selectedModalSeason == s)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        when {
                                                            isSeasonFocused -> warmIvory
                                                            isSelected -> Color(0xFF1F2430)
                                                            else -> Color(0xFF0E121A)
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isSeasonFocused || isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSeasonFocused || isSelected) warmIvory else Color.White.copy(alpha = 0.14f),
                                                        shape = RoundedCornerShape(50)
                                                    )
                                                    .onFocusChanged { isSeasonFocused = it.isFocused }
                                                    .focusable()
                                                    .onKeyEvent { keyEvent ->
                                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                        ) {
                                                            selectedModalSeason = s
                                                            true
                                                        } else false
                                                    }
                                                    .clickable { selectedModalSeason = s }
                                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = "T$s",
                                                    color = if (isSeasonFocused) Color.Black else warmIvory,
                                                    fontFamily = Inter,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.2.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isSeriesContent) {
                                    "Selecciona un episodio con el mando a distancia para reproducirlo inmediatamente."
                                } else {
                                    "Selecciona una escena con el mando a distancia para saltar inmediatamente a ese momento."
                                },
                                color = Color(0xFF9CA3AF),
                                fontFamily = Inter,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Píldora derecha: ● ACTIVO: EPISODIO 0X / ESCENA 0X
                        val activeBadgeNum = if (isSeriesContent) activeEpisodeInt else (activeCardIndex + 1)
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF10141D))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD6D0C4))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isSeriesContent) "ACTIVO: EPISODIO ${String.format("%02d", activeBadgeNum)}" else "ACTIVO: ESCENA ${String.format("%02d", activeBadgeNum)}",
                                color = warmIvory,
                                fontFamily = Inter,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Carrusel horizontal de tarjetas de capítulos / escenas
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(modalEpisodes) { index, ep ->
                            var isCardFocused by remember { mutableStateOf(false) }
                            val isCurrentActive = if (isSeriesContent) {
                                selectedModalSeason == activeSeasonInt && ep.episodeNumber == activeEpisodeInt
                            } else {
                                index == activeCardIndex
                            }
                            val isWatched = if (isSeriesContent) {
                                selectedModalSeason < activeSeasonInt ||
                                    (selectedModalSeason == activeSeasonInt && ep.episodeNumber < activeEpisodeInt)
                            } else {
                                index < activeCardIndex
                            }

                            val cleanEpTitle = remember(ep.title, cleanMainTitle, ep.episodeNumber) {
                                val stripped = ep.title
                                    .replace(Regex("^\\Q$cleanMainTitle\\E\\s*[:\\-·]\\s*", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("^Temporada\\s*\\d+\\s*[:\\-·]?\\s*Episodio\\s*\\d+\\s*[:\\-·]?\\s*", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("^Episodio\\s*\\d+\\s*[:\\-·]\\s*", RegexOption.IGNORE_CASE), "")
                                    .trim()
                                stripped.ifBlank { if (isSeriesContent) "Episodio ${ep.episodeNumber}" else "Escena ${index + 1}" }
                            }

                            val activateChapter = {
                                if (isSeriesContent) {
                                    if (!isCurrentActive) {
                                        playEpisode(ep)
                                    } else {
                                        showEpisodesDialog = false
                                        setPlaybackPlaying(true)
                                    }
                                } else {
                                    // Escena de película: saltar al porcentaje correspondiente
                                    showEpisodesDialog = false
                                    val targetFrac = index.toFloat() / modalEpisodes.size.coerceAtLeast(1).toFloat()
                                    val targetMs = if (durationMs > 0L) (durationMs * targetFrac).toLong() else 0L
                                    seekToMs(targetMs)
                                    setPlaybackPlaying(true)
                                    wakeUpControls()
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCardFocused) Color(0xFF151922) else Color(0xFF0C0F15)
                                ),
                                border = if (isCardFocused) BorderStroke(2.2.dp, Color(0xFFE5DFD3))
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .width(292.dp)
                                    .height(352.dp)
                                    .then(
                                        if (index == activeCardIndex) Modifier.focusRequester(activeEpisodeFocusRequester)
                                        else Modifier
                                    )
                                    .onFocusChanged { isCardFocused = it.isFocused }
                                    .focusable()
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                        ) {
                                            activateChapter()
                                            true
                                        } else false
                                    }
                                    .clickable { activateChapter() }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp)
                                ) {
                                    // 1. Miniatura 16:9 con insignias VISTO / REPRODUCIENDO AHORA y barra inferior
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(146.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF080A0F))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(ep.image.ifBlank { defaultBackdrop })
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = cleanEpTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        0.0f to Color.Black.copy(alpha = 0.35f),
                                                        0.5f to Color.Transparent,
                                                        1.0f to Color.Black.copy(alpha = 0.82f)
                                                    )
                                                )
                                        )

                                        // Insignia superior izquierda: REPRODUCIENDO AHORA o VISTO
                                        if (isCurrentActive) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(10.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(warmIvory)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF6B665E))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "REPRODUCIENDO AHORA",
                                                    color = Color(0xFF0A0D13),
                                                    fontFamily = Inter,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.0.sp
                                                )
                                            }
                                        } else if (isWatched) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(10.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color(0xD90A0E14))
                                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.45f), RoundedCornerShape(50))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "VISTO",
                                                    color = Color(0xFFD1FAE5),
                                                    fontFamily = Inter,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.2.sp
                                                )
                                            }
                                        }

                                        // Insignia de tiempo / duración
                                        Box(
                                            modifier = Modifier
                                                .align(if (isCurrentActive) Alignment.TopEnd else Alignment.BottomEnd)
                                                .padding(10.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xD9080A0F))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (isCurrentActive && displayPosMs > 0L) {
                                                    formatTimeHms(displayPosMs / 1000)
                                                } else {
                                                    ep.runtime.ifBlank { "45 min" }
                                                },
                                                color = warmIvory,
                                                fontFamily = Inter,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            )
                                        }

                                        // Barra de progreso inferior de la miniatura
                                        if (isWatched) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .background(Color(0xFF10B981))
                                            )
                                        } else if (isCurrentActive) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .fillMaxWidth()
                                                    .height(3.5.dp)
                                                    .background(Color.White.copy(alpha = 0.22f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(progressFraction.coerceIn(0.18f, 1f))
                                                        .fillMaxHeight()
                                                        .background(warmIvory)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 2. Fila de metadatos: EPISODIO 0X · EN FOCO | % VISTO o Duración
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val chapLabel = if (isSeriesContent) {
                                            if (isCardFocused) "EPISODIO ${String.format("%02d", ep.episodeNumber)} · EN FOCO"
                                            else "EPISODIO ${String.format("%02d", ep.episodeNumber)}"
                                        } else {
                                            if (isCardFocused) "ESCENA ${String.format("%02d", index + 1)} · EN FOCO"
                                            else "ESCENA ${String.format("%02d", index + 1)}"
                                        }
                                        Text(
                                            text = chapLabel,
                                            color = if (isCardFocused) Color(0xFFE5DFD3) else Color(0xFF6B7280),
                                            fontFamily = Inter,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.5.sp
                                        )

                                        Text(
                                            text = if (isCurrentActive) "$completedPercent% VISTO" else ep.runtime.uppercase(),
                                            color = if (isCardFocused) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                            fontFamily = Inter,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 1.2.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 3. Título del capítulo
                                    Text(
                                        text = cleanEpTitle,
                                        color = if (isCardFocused) warmIvory else Color(0xFFD1D5DB),
                                        fontFamily = Inter,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 4. Sinopsis del capítulo
                                    Text(
                                        text = ep.overview,
                                        color = if (isCardFocused) Color(0xFFB0B7C3) else Color(0xFF6B7280),
                                        fontFamily = Inter,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    // 5. Pie de tarjeta con Duración a la izquierda y botón CONTINUAR / REPRODUCIR OK a la derecha
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Duración: ${ep.runtime.ifBlank { "45 min" }}",
                                            color = Color(0xFF9CA3AF),
                                            fontFamily = Inter,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )

                                        if (isCardFocused || isCurrentActive) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (isCardFocused) warmIvory else Color.White.copy(alpha = 0.12f)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = if (isCardFocused) Color(0xFF0A0D13) else warmIvory,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isCurrentActive) "CONTINUAR" else "REPRODUCIR",
                                                    color = if (isCardFocused) Color(0xFF0A0D13) else warmIvory,
                                                    fontFamily = Inter,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.2.sp
                                                )
                                                if (isCardFocused) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFD6D0C4))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "OK",
                                                            color = Color(0xFF0A0D13),
                                                            fontFamily = Inter,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Black
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
                }
            }
        }

        // TV Controls Overlay — HUD Cinematográfico de Alta Gama (Inspirado en el diseño de referencia del usuario)
        AnimatedVisibility(
            visible = showControls && !showServerDialog && !showEpisodesDialog && !showNextEpisodePrompt,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(280))
        ) {
            val warmIvory = Color(0xFFF5F2EB)
            val glassSurface = Color(0xD90A0D13)
            val pillSurface = Color(0xCC0B0F17)
            val subtleBorder = Color.White.copy(alpha = 0.14f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.80f),
                                0.22f to Color.Transparent,
                                0.65f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = false }
                        )
                    }
                    .padding(horizontal = 44.dp, vertical = 30.dp)
            ) {
                // 1. TOP BAR: Botón "← VOLVER" + Nombre limpio de la serie/película
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isBackFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBackFocused) warmIvory else pillSurface,
                                contentColor = if (isBackFocused) Color.Black else warmIvory
                            ),
                            border = if (isBackFocused) BorderStroke(2.dp, warmIvory)
                            else BorderStroke(1.dp, subtleBorder),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .focusRequester(backButtonFocusRequester)
                                .onFocusChanged { isBackFocused = it.isFocused }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                onBackClick()
                                                true
                                            }
                                            Key.DirectionDown -> {
                                                try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            Key.DirectionRight -> {
                                                try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VOLVER",
                                fontFamily = Inter,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        Text(
                            text = cleanMainTitle,
                            color = warmIvory,
                            fontFamily = BebasNeue,
                            fontSize = 24.sp,
                            letterSpacing = 1.8.sp,
                            maxLines = 1
                        )
                }

                // 2. BOTTOM FLOATING CINEMA HUD (Centro del video 100% despejado)
                if (isVideoReady) {
                    var isTimelineFocused by remember { mutableStateOf(false) }
                    val displayPosMs = if (isEmbedStream) currentPositionMs else exoPlayer.currentPosition.coerceAtLeast(0L)
                    val progressFraction = if (durationMs > 0L) {
                        (displayPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val remainingSec = if (durationMs > displayPosMs) (durationMs - displayPosMs) / 1000 else 0L
                    val completedPercent = (progressFraction * 100f).toInt().coerceIn(0, 100)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // A. TARJETA FLOTANTE DE LÍNEA DE TIEMPO (Glass Card)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(glassSurface)
                                .border(
                                    width = if (isTimelineFocused) 1.5.dp else 1.dp,
                                    color = if (isTimelineFocused) warmIvory.copy(alpha = 0.55f) else subtleBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            // Fila superior de la tarjeta: TEMPORADA X : EPISODIO Y  |  XX% COMPLETADO
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeSequenceLabel,
                                    color = Color(0xFFE5E7EB),
                                    fontFamily = Inter,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.8.sp,
                                    maxLines = 1
                                )

                                Text(
                                    text = "$completedPercent% COMPLETADO",
                                    color = Color(0xFF9CA3AF),
                                    fontFamily = Inter,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Barra de progreso interactiva con marcas de cuarto y anillo indicador (Hollow Ring Scrubber)
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .focusRequester(timelineFocusRequester)
                                    .onFocusChanged { isTimelineFocused = it.isFocused }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionLeft -> {
                                                    performSeek(false, 10_000L)
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    performSeek(true, 10_000L)
                                                    true
                                                }
                                                Key.DirectionUp -> {
                                                    try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionDown -> {
                                                    try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                    togglePlayPause()
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                                    .focusable()
                                    .pointerInput(durationMs) {
                                        detectTapGestures { offset ->
                                            if (durationMs > 0L && size.width > 0) {
                                                val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                val target = (frac * durationMs).toLong()
                                                seekToMs(target)
                                                seekToastText = formatTimeHms(target / 1000)
                                                wakeUpControls()
                                            }
                                        }
                                    }
                                    .pointerInput(durationMs) {
                                        detectHorizontalDragGestures { change, _ ->
                                            change.consume()
                                            if (durationMs > 0L && size.width > 0) {
                                                val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                val target = (frac * durationMs).toLong()
                                                seekToMs(target)
                                                seekToastText = formatTimeHms(target / 1000)
                                                wakeUpControls()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val trackHeight = if (isTimelineFocused) 4.dp else 2.5.dp
                                val haloSize = if (isTimelineFocused) 24.dp else 18.dp
                                val ringSize = if (isTimelineFocused) 15.dp else 12.dp
                                val thumbOffset = ((maxWidth - haloSize) * progressFraction).coerceAtLeast(0.dp)

                                // Riel base
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(trackHeight)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.18f))
                                )

                                // Marcas sutiles de capítulos/cuartos (25%, 50%, 75%)
                                listOf(0.25f, 0.50f, 0.75f).forEach { tickFrac ->
                                    val tickX = maxWidth * tickFrac
                                    Box(
                                        modifier = Modifier
                                            .offset(x = tickX)
                                            .width(1.dp)
                                            .height(7.dp)
                                            .background(Color.White.copy(alpha = 0.28f))
                                    )
                                }

                                // Riel de progreso completado en blanco marfil cálido
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = progressFraction)
                                        .height(trackHeight)
                                        .clip(RoundedCornerShape(50))
                                        .background(warmIvory)
                                )

                                // Scrubber Thumb: Anillo hueco luminoso con núcleo oscuro y punto central
                                Box(
                                    modifier = Modifier
                                        .offset(x = thumbOffset)
                                        .size(haloSize)
                                        .clip(CircleShape)
                                        .background(warmIvory.copy(alpha = if (isTimelineFocused) 0.28f else 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(ringSize)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0A0D13))
                                            .border(2.dp, warmIvory, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(3.5.dp)
                                                .clip(CircleShape)
                                                .background(warmIvory)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Fila inferior de la tarjeta: Tiempo actual | Tiempo restante
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatTimeHms(displayPosMs / 1000),
                                    color = warmIvory,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.2.sp
                                )

                                Text(
                                    text = if (durationMs > 0L) "-${formatTimeHms(remainingSec)}" else "--:--:--",
                                    color = Color(0xFF9CA3AF),
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }

                        // B. DOCK INFERIOR DE CONTROLES (Izquierda: Audio y Servidores | Centro: Píldora Oval [-10, Play/Pausa, +10] | Derecha: Desde el inicio)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Botón Izquierdo: AUDIO Y SERVIDORES (Icono rectangular Subtitles, SIN tuerca)
                            var isServerPillFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = { showServerDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isServerPillFocused) warmIvory else pillSurface,
                                    contentColor = if (isServerPillFocused) Color.Black else warmIvory
                                ),
                                border = if (isServerPillFocused) BorderStroke(2.dp, warmIvory)
                                else BorderStroke(1.dp, subtleBorder),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .height(42.dp)
                                    .focusRequester(serverPillFocusRequester)
                                    .onFocusChanged { isServerPillFocused = it.isFocused }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                    showServerDialog = true
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    try {
                                                        if (isSeriesContent) {
                                                            prevEpisodeDockFocusRequester.requestFocus()
                                                        } else {
                                                            rewindFocusRequester.requestFocus()
                                                        }
                                                    } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionUp -> {
                                                    try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionDown -> {
                                                    showControls = false
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "Audio y Subtítulos",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "AUDIO Y SUBTÍTULOS",
                                    fontFamily = Inter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.6.sp
                                )
                            }

                            // Dock Central Ovalado: [⏮ Ep. Anterior] [-10s]  [ Play / Pausa ]  [+10s] [⏭ Ep. Siguiente]
                            Row(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(glassSurface)
                                    .border(1.dp, subtleBorder, RoundedCornerShape(50))
                                    .padding(horizontal = if (isSeriesContent) 14.dp else 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(if (isSeriesContent) 14.dp else 22.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botón: EPISODIO ANTERIOR (Solo en series)
                                if (isSeriesContent) {
                                    var isPrevEpFocused by remember { mutableStateOf(false) }
                                    val hasPrev = previousEpisode != null
                                    IconButton(
                                        onClick = {
                                            if (previousEpisode != null) {
                                                seekToastText = "Cargando ${previousEpisode.title}..."
                                                playEpisode(previousEpisode)
                                            } else {
                                                seekToMs(0L)
                                                seekToastText = "Ya estás en el primer episodio"
                                                wakeUpControls()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .focusRequester(prevEpisodeDockFocusRequester)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPrevEpFocused) warmIvory else Color.Transparent
                                            )
                                            .onFocusChanged { isPrevEpFocused = it.isFocused }
                                            .onKeyEvent { keyEvent ->
                                                if (keyEvent.type == KeyEventType.KeyDown) {
                                                    when (keyEvent.key) {
                                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                            if (previousEpisode != null) {
                                                                seekToastText = "Cargando ${previousEpisode.title}..."
                                                                playEpisode(previousEpisode)
                                                            } else {
                                                                seekToMs(0L)
                                                                seekToastText = "Ya estás en el primer episodio"
                                                                wakeUpControls()
                                                            }
                                                            true
                                                        }
                                                        Key.DirectionLeft -> {
                                                            try { serverPillFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        Key.DirectionRight -> {
                                                            try { rewindFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        Key.DirectionUp -> {
                                                            try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        Key.DirectionDown -> {
                                                            showControls = false
                                                            true
                                                        }
                                                        else -> false
                                                    }
                                                } else false
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = if (hasPrev) "Episodio anterior: ${previousEpisode?.title ?: ""}" else "Primer episodio",
                                            tint = if (isPrevEpFocused) Color.Black else if (hasPrev) warmIvory else warmIvory.copy(alpha = 0.38f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                var isRewindFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { performSeek(false) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .focusRequester(rewindFocusRequester)
                                        .clip(CircleShape)
                                        .background(
                                            if (isRewindFocused) warmIvory else Color.Transparent
                                        )
                                        .onFocusChanged { isRewindFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                        performSeek(false)
                                                        true
                                                    }
                                                    Key.DirectionLeft -> {
                                                        try {
                                                            if (isSeriesContent) {
                                                                prevEpisodeDockFocusRequester.requestFocus()
                                                            } else {
                                                                serverPillFocusRequester.requestFocus()
                                                            }
                                                        } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionRight -> {
                                                        try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionUp -> {
                                                        try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionDown -> {
                                                        showControls = false
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Retroceder 10 segundos",
                                        tint = if (isRewindFocused) Color.Black else warmIvory,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                var isPlayFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        val willPlay = !isPlaying
                                        togglePlayPause()
                                        if (willPlay) {
                                            controlsHideFastTick++
                                        } else {
                                            wakeUpControls()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .focusRequester(playButtonFocusRequester)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPlayFocused) warmIvory else Color.Transparent
                                        )
                                        .onFocusChanged { isPlayFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                        val willPlay = !isPlaying
                                                        togglePlayPause()
                                                        if (willPlay) {
                                                            controlsHideFastTick++
                                                        } else {
                                                            wakeUpControls()
                                                        }
                                                        true
                                                    }
                                                    Key.DirectionLeft -> {
                                                        try { rewindFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionRight -> {
                                                        try { ffFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionUp -> {
                                                        try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionDown -> {
                                                        showControls = false
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir o Pausar",
                                        tint = if (isPlayFocused) Color.Black else warmIvory,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                var isFfFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { performSeek(true) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .focusRequester(ffFocusRequester)
                                        .clip(CircleShape)
                                        .background(
                                            if (isFfFocused) warmIvory else Color.Transparent
                                        )
                                        .onFocusChanged { isFfFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                        performSeek(true)
                                                        true
                                                    }
                                                    Key.DirectionLeft -> {
                                                        try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionRight -> {
                                                        try {
                                                            if (isSeriesContent) {
                                                                nextEpisodeDockFocusRequester.requestFocus()
                                                            } else {
                                                                restartButtonFocusRequester.requestFocus()
                                                            }
                                                        } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionUp -> {
                                                        try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionDown -> {
                                                        showControls = false
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Adelantar 10 segundos",
                                        tint = if (isFfFocused) Color.Black else warmIvory,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Botón: SIGUIENTE EPISODIO (Solo en series)
                                if (isSeriesContent) {
                                    var isNextEpFocused by remember { mutableStateOf(false) }
                                    val hasNext = nextEpisode != null
                                    IconButton(
                                        onClick = {
                                            if (nextEpisode != null) {
                                                seekToastText = "Cargando ${nextEpisode.title}..."
                                                playEpisode(nextEpisode)
                                            } else {
                                                seekToastText = "No hay más episodios disponibles"
                                                wakeUpControls()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .focusRequester(nextEpisodeDockFocusRequester)
                                            .clip(CircleShape)
                                            .background(
                                                if (isNextEpFocused) warmIvory else Color.Transparent
                                            )
                                            .onFocusChanged { isNextEpFocused = it.isFocused }
                                            .onKeyEvent { keyEvent ->
                                                if (keyEvent.type == KeyEventType.KeyDown) {
                                                    when (keyEvent.key) {
                                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                            if (nextEpisode != null) {
                                                                seekToastText = "Cargando ${nextEpisode.title}..."
                                                                playEpisode(nextEpisode)
                                                            } else {
                                                                seekToastText = "No hay más episodios disponibles"
                                                                wakeUpControls()
                                                            }
                                                            true
                                                        }
                                                        Key.DirectionLeft -> {
                                                            try { ffFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        Key.DirectionRight -> {
                                                            try { restartButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        Key.DirectionUp -> {
                                                            try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        Key.DirectionDown -> {
                                                            showControls = false
                                                            true
                                                        }
                                                        else -> false
                                                    }
                                                } else false
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = if (hasNext) "Siguiente episodio: ${nextEpisode?.title ?: ""}" else "Último episodio",
                                            tint = if (isNextEpFocused) Color.Black else if (hasNext) warmIvory else warmIvory.copy(alpha = 0.38f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Botones Derechos: [DESDE EL INICIO (Solo icono)] y opcionalmente [CAPÍTULOS]
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botón: DESDE EL INICIO (Solo icono, circular)
                                var isRestartFocused by remember { mutableStateOf(false) }
                                Button(
                                    onClick = {
                                        seekToMs(0L)
                                        WatchHistoryManager.resetMoviePosition(context, movie.url)
                                        seekToastText = "Reproduciendo desde el inicio"
                                        wakeUpControls()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRestartFocused) warmIvory else pillSurface,
                                        contentColor = if (isRestartFocused) Color.Black else warmIvory
                                    ),
                                    border = if (isRestartFocused) BorderStroke(2.dp, warmIvory)
                                    else BorderStroke(1.dp, subtleBorder),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .focusRequester(restartButtonFocusRequester)
                                        .onFocusChanged { isRestartFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                        seekToMs(0L)
                                                        WatchHistoryManager.resetMoviePosition(context, movie.url)
                                                        seekToastText = "Reproduciendo desde el inicio"
                                                        wakeUpControls()
                                                        true
                                                    }
                                                    Key.DirectionLeft -> {
                                                        try {
                                                            if (isSeriesContent) {
                                                                nextEpisodeDockFocusRequester.requestFocus()
                                                            } else {
                                                                ffFocusRequester.requestFocus()
                                                            }
                                                        } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionRight -> {
                                                        if (hasEpisodesButton) {
                                                            try { episodesButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        } else false
                                                    }
                                                    Key.DirectionUp -> {
                                                        try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionDown -> {
                                                        showControls = false
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = "Desde el inicio",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Botón: CAPÍTULOS (Solo visible en series con más de 1 episodio)
                                if (hasEpisodesButton) {
                                    var isEpisodesFocused by remember { mutableStateOf(false) }
                                    Button(
                                        onClick = {
                                            selectedModalSeason = activeSeasonInt
                                            showEpisodesDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isEpisodesFocused) warmIvory else pillSurface,
                                            contentColor = if (isEpisodesFocused) Color.Black else warmIvory
                                        ),
                                        border = if (isEpisodesFocused) BorderStroke(2.dp, warmIvory)
                                        else BorderStroke(1.dp, subtleBorder),
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .height(42.dp)
                                            .focusRequester(episodesButtonFocusRequester)
                                            .onFocusChanged { isEpisodesFocused = it.isFocused }
                                            .onKeyEvent { keyEvent ->
                                                if (keyEvent.type == KeyEventType.KeyDown) {
                                                    when (keyEvent.key) {
                                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                            selectedModalSeason = activeSeasonInt
                                                            showEpisodesDialog = true
                                                            true
                                                        }
                                                        Key.DirectionLeft -> {
                                                            try { restartButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        Key.DirectionUp -> {
                                                            try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        Key.DirectionDown -> {
                                                            showControls = false
                                                            true
                                                        }
                                                        else -> false
                                                    }
                                                } else false
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatListBulleted,
                                            contentDescription = if (isSeriesContent) "Episodios" else "Escenas",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isSeriesContent) "EPISODIOS" else "ESCENAS",
                                            fontFamily = Inter,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 1.6.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FLOATING NEXT EPISODE COUNTDOWN OVERLAY (Estilo Netflix / Disney+) ---
        if (isSeriesContent && nextEpisode != null) {
            val warmIvory = Color(0xFFF5F2EB)
            AnimatedVisibility(
                visible = showNextEpisodePrompt,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(350)) { it / 2 },
                exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(250)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 40.dp, bottom = 36.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xF20B0E14)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFE5DFD3).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .width(440.dp)
                        .shadow(28.dp, shape = RoundedCornerShape(18.dp), ambientColor = Color.Black, spotColor = Color.Black)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header superior: SIGUIENTE EPISODIO  |  EN 5s o ¿SIGUES AHÍ?
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    tint = warmIvory,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SIGUIENTE EPISODIO",
                                    color = warmIvory,
                                    fontSize = 12.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.6.sp
                                )
                            }

                            if (consecutiveAutoPlayCount < 3) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFFE5DFD3).copy(alpha = 0.18f))
                                        .border(1.dp, Color(0xFFE5DFD3).copy(alpha = 0.35f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "EN ${nextEpisodeCountdownSeconds}s",
                                        color = warmIvory,
                                        fontSize = 11.5.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFFE5DFD3).copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "¿SIGUES AHÍ?",
                                        color = warmIvory,
                                        fontSize = 11.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Thumbnail + Info del capítulo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(116.dp)
                                    .height(70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF151922))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(nextEpisode.image.ifBlank { detailResponse?.backdrop ?: movie.backdrop ?: movie.image })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = nextEpisode.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "TEMPORADA ${nextEpisode.seasonNumber} : EPISODIO ${nextEpisode.episodeNumber}",
                                    color = warmIvory.copy(alpha = 0.70f),
                                    fontSize = 10.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                val cleanNextEpTitle = remember(nextEpisode.title, cleanMainTitle, movie.title, nextEpisode.episodeNumber) {
                                    var s = nextEpisode.title.cleanHtml().trim()
                                    // Limpiar prefijo con el nombre de la serie (incluyendo variantes sin año)
                                    val nameCandidates = listOf(
                                        cleanMainTitle,
                                        cleanMainTitle.replace(Regex("""\s*\(\d{4}\).*$"""), "").trim(),
                                        movie.title,
                                        movie.title.replace(Regex("""\s*\(\d{4}\).*$"""), "").trim()
                                    ).filter { it.isNotBlank() }.distinct()

                                    nameCandidates.forEach { name ->
                                        s = s.replace(Regex("^\\Q$name\\E\\s*[:\\-·/]?\\s*", RegexOption.IGNORE_CASE), "")
                                        // También por si el nombre de la serie aparece en cualquier parte
                                        s = s.replace(Regex("\\Q$name\\E", RegexOption.IGNORE_CASE), "")
                                    }
                                    // Limpiar prefijos o residuos de Temporada/Capítulo (ej. "Temporada 1 Episodio 5", "T1:E5", "Temporada 1 : Episodio 2")
                                    s = s.replace(Regex("""(?i)\b(?:Temporada|Temp|T)\s*\d+\b\s*[:\-·/]?\s*"""), "")
                                    s = s.replace(Regex("""(?i)\b(?:Episodio|Capítulo|Cap|E)\s*\d+\b\s*[:\-·/]?\s*"""), "")
                                    s = s.trim().trim(':', '-', '·', ' ', '/', '.')
                                    if (s.isBlank() ||
                                        s.equals("Capítulo ${nextEpisode.episodeNumber}", ignoreCase = true) ||
                                        s.equals("Episodio ${nextEpisode.episodeNumber}", ignoreCase = true) ||
                                        s.equals("Episodio", ignoreCase = true) ||
                                        s.equals("Capítulo", ignoreCase = true) ||
                                        nameCandidates.any { candidate -> s.contains(candidate, ignoreCase = true) }
                                    ) {
                                        ""
                                    } else {
                                        s
                                    }
                                }

                                if (cleanNextEpTitle.isNotBlank()) {
                                    Text(
                                        text = cleanNextEpTitle,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (nextEpisode.overview.isNotBlank()) {
                                    Text(
                                        text = nextEpisode.overview.cleanHtml(),
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 10.5.sp,
                                        fontFamily = Inter,
                                        maxLines = if (cleanNextEpTitle.isNotBlank()) 2 else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Botones de acción (Control Remoto)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isPlayActive = nextEpisodeSelectedButton == 0
                            val isCancelActive = nextEpisodeSelectedButton == 1

                            Button(
                                onClick = {
                                    nextEpisodeSelectedButton = 0
                                    consecutiveAutoPlayCount = 0
                                    showNextEpisodePrompt = false
                                    playEpisode(nextEpisode)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlayActive) warmIvory else Color(0xFF1E2430),
                                    contentColor = if (isPlayActive) Color(0xFF0B0E14) else warmIvory
                                ),
                                border = if (isPlayActive) BorderStroke(2.dp, warmIvory)
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "REPRODUCIR AHORA",
                                    fontSize = 11.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    nextEpisodeSelectedButton = 1
                                    showNextEpisodePrompt = false
                                    isNextEpisodeDismissed = true
                                    consecutiveAutoPlayCount = 0
                                    wakeUpControls()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isCancelActive) warmIvory else Color.Transparent,
                                    contentColor = if (isCancelActive) Color(0xFF0B0E14) else Color(0xFF9CA3AF)
                                ),
                                border = if (isCancelActive) BorderStroke(2.dp, warmIvory)
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CANCELAR",
                                    fontSize = 10.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- MOVIE COMPLETED OVERLAY (Estilo Netflix / Apple TV) ---
        if (!isSeriesContent) {
            val warmIvory = Color(0xFFF5F2EB)
            AnimatedVisibility(
                visible = showMovieEndedPrompt,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(350)) { it / 3 },
                exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(250)) { it / 3 },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xF20B0E14)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFE5DFD3).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .width(480.dp)
                        .shadow(32.dp, shape = RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(NetflixRed.copy(alpha = 0.15f))
                                .border(1.5.dp, NetflixRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = warmIvory,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PELÍCULA FINALIZADA",
                                color = warmIvory,
                                fontSize = 13.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = cleanMainTitle,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "¿Qué deseas hacer ahora?",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.5.sp,
                                fontFamily = Inter
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isReplayActive = isMovieEndedReplayFocused
                            val isBackActive = isMovieEndedBackFocused || (!isMovieEndedReplayFocused && !isMovieEndedBackFocused)

                            Button(
                                onClick = {
                                    showMovieEndedPrompt = false
                                    seekToMs(0L)
                                    setPlaybackPlaying(true)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isReplayActive) warmIvory else Color(0xFF1E2430),
                                    contentColor = if (isReplayActive) Color(0xFF0B0E14) else warmIvory
                                ),
                                border = if (isReplayActive) BorderStroke(2.dp, warmIvory)
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .focusRequester(movieEndedReplayFocusRequester)
                                    .onFocusChanged { isMovieEndedReplayFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VOLVER A VER",
                                    fontSize = 11.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    showMovieEndedPrompt = false
                                    onBackClick()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isBackActive) warmIvory else Color.Transparent,
                                    contentColor = if (isBackActive) Color(0xFF0B0E14) else Color(0xFF9CA3AF)
                                ),
                                border = if (isBackActive) BorderStroke(2.dp, warmIvory)
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .focusRequester(movieEndedBackFocusRequester)
                                    .onFocusChanged { isMovieEndedBackFocused = it.isFocused }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DETALLES",
                                    fontSize = 11.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun formatTimeHms(seconds: Long): String {
    val totalSec = seconds.coerceAtLeast(0L)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

internal fun formatTimeSec(seconds: Long): String {
    val totalSec = seconds.coerceAtLeast(0L)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}


