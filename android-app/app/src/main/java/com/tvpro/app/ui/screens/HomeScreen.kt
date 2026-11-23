package com.tvpro.app.ui.screens

import android.view.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.compose.material3.Icon as M3Icon
import androidx.tv.material3.*
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.WatchHistoryItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.app.ui.components.*
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState

import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (MovieItem, String) -> Unit,
    onPlayClick: (MovieItem) -> Unit,
    playButtonFocusRequester: FocusRequester,
    navFocusRequester: FocusRequester,
    selectedNavId: String = "home",
    onFocusItem: (FocusRequester) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    shouldRequestInitialFocus: Boolean = false,
    onInitialFocusLanded: () -> Unit = {},
    restoreFocusUrl: String? = null,
    restoreFocusRowKey: String? = null,
    onFocusRestored: () -> Unit = {}
) {
    val listState = rememberTvLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val watchHistory = remember { mutableStateListOf<WatchHistoryItem>() }
    val firstRowFocusRequester = remember { FocusRequester() }

    // Reload history on launch and whenever returning to Home (e.g. from Detail/Player)
    LaunchedEffect(context, restoreFocusUrl, restoreFocusRowKey) {
        watchHistory.clear()
        watchHistory.addAll(WatchHistoryManager.getHistory(context))
    }

    // Scroll vertical list to row containing restoreFocusUrl when returning from Detail
    LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, uiState) {
        val url = restoreFocusUrl ?: return@LaunchedEffect
        val rowKey = restoreFocusRowKey ?: return@LaunchedEffect
        val state = uiState as? HomeUiState.Success ?: return@LaunchedEffect

        val hasHistory = watchHistory.isNotEmpty()

        val targetRowIndex = when (rowKey) {
            "hero" -> 0
            "history" -> if (hasHistory) 1 else 0
            "top10" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.recentMovies.isNotEmpty()) idx++
                idx
            }
            "Películas recién añadidas" -> if (hasHistory) 2 else 1
            "Series recién añadidas" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.recentMovies.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                idx
            }
            "Animes recién añadidos" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.recentMovies.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                if (state.recentSeries.isNotEmpty()) idx++
                idx
            }
            "La Hora del Drama (Novelas)" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.recentMovies.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                if (state.recentSeries.isNotEmpty()) idx++
                if (state.recentAnimes.isNotEmpty()) idx++
                idx
            }
            else -> {
                var idx = if (hasHistory) 2 else 1
                if (state.recentMovies.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                if (state.recentSeries.isNotEmpty()) idx++
                if (state.recentAnimes.isNotEmpty()) idx++
                if (state.dramas.isNotEmpty()) idx++
                var found: Int? = null
                for (col in state.collections) {
                    if (col.movies.isNotEmpty()) {
                        if (col.title == rowKey) {
                            found = idx
                            break
                        }
                        idx++
                    }
                }
                found
            }
        }

        if (targetRowIndex != null) {
            try {
                listState.scrollToItem(targetRowIndex, 0)
            } catch (_: Exception) {}
        }
    }

    // Land initial focus on Hero Play button ONLY ONCE on initial app launch
    LaunchedEffect(shouldRequestInitialFocus, uiState) {
        if (shouldRequestInitialFocus && uiState is HomeUiState.Success) {
            delay(120)
            try {
                playButtonFocusRequester.requestFocus()
                onInitialFocusLanded()
            } catch (_: Exception) {}
        }
    }

    // React to top navigation selection (jump to corresponding category)
    LaunchedEffect(selectedNavId) {
        if (restoreFocusUrl != null) return@LaunchedEffect
        val hasHistory = watchHistory.isNotEmpty()
        val moviesIndex = if (hasHistory) 2 else 1
        val seriesIndex = if (hasHistory) 4 else 3
        val animesIndex = if (hasHistory) 5 else 4

        when (selectedNavId) {
            "home" -> {
                try {
                    listState.animateScrollToItem(0, 0)
                } catch (_: Exception) {}
            }
            "movies" -> {
                try {
                    listState.animateScrollToItem(moviesIndex, 0)
                } catch (_: Exception) {}
            }
            "series" -> {
                try {
                    listState.animateScrollToItem(seriesIndex, 0)
                } catch (_: Exception) {}
            }
            "animes" -> {
                try {
                    listState.animateScrollToItem(animesIndex, 0)
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06080C))
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                SkeletonHomeScreen()
            }
            is HomeUiState.Error -> {
                TvConnectionErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.loadCatalog() }
                )
            }
            is HomeUiState.Success -> {
                val featured = state.featured
                val recentMovies = state.recentMovies
                val heroMovies = featured.ifEmpty { recentMovies.take(5) }
                val top10Movies = state.top10
                val series = state.recentSeries
                val animes = state.recentAnimes
                val dramas = state.dramas
                val collections = state.collections

                val historyListState = rememberLazyListState()
                val historyCardRequesters = remember { mutableMapOf<String, FocusRequester>() }
                var lastFocusedHistoryIndex by remember { mutableIntStateOf(0) }

                val top10ListState = rememberLazyListState()
                val top10CardRequesters = remember { mutableMapOf<String, FocusRequester>() }
                var lastFocusedTop10Index by remember { mutableIntStateOf(0) }

                // Restore focus to Hero Play button ONLY if Hero was clicked
                LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
                    if (restoreFocusUrl != null && restoreFocusRowKey == "hero") {
                        delay(100)
                        try {
                            playButtonFocusRequester.requestFocus()
                            onFocusRestored()
                        } catch (_: Exception) {}
                    }
                }

                // Restore focus to Continue Watching card if clicked
                val isHistoryTarget = (restoreFocusRowKey == "history" || (restoreFocusRowKey == null && watchHistory.any { it.movieUrl == restoreFocusUrl }))
                LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, watchHistory.toList()) {
                    if (restoreFocusUrl != null && isHistoryTarget && watchHistory.any { it.movieUrl == restoreFocusUrl }) {
                        val targetIdx = watchHistory.indexOfFirst { it.movieUrl == restoreFocusUrl }
                        if (targetIdx >= 0) {
                            try { historyListState.scrollToItem(targetIdx) } catch (_: Exception) {}
                        }
                    }
                }

                // Restore focus to Top 10 card if clicked
                val isTop10Target = (restoreFocusRowKey == "top10" || (restoreFocusRowKey == null && top10Movies.any { it.url == restoreFocusUrl }))
                LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, top10Movies) {
                    if (restoreFocusUrl != null && isTop10Target && top10Movies.any { it.url == restoreFocusUrl }) {
                        val targetIdx = top10Movies.indexOfFirst { it.url == restoreFocusUrl }
                        if (targetIdx >= 0) {
                            try { top10ListState.scrollToItem(targetIdx) } catch (_: Exception) {}
                        }
                    }
                }

                // Silent background preload for Continue Watching and Top items
                LaunchedEffect(watchHistory.toList(), top10Movies) {
                    withContext(Dispatchers.IO) {
                        watchHistory.take(4).forEach { item ->
                            try {
                                MovieRepository.preloadMovie(
                                    MovieItem(
                                        title = item.title,
                                        url = item.movieUrl,
                                        image = item.image,
                                        rating = item.rating,
                                        postId = item.postId,
                                        slug = item.slug,
                                        type = if (item.isSeries) "series" else "movie"
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                        top10Movies.take(2).forEach { m ->
                            try {
                                MovieRepository.preloadMovie(m)
                            } catch (_: Exception) {}
                        }
                    }
                }

                val isHeroScrolledDown by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 60
                    }
                }

                TvLazyColumn(
                    state = listState,
                    pivotOffsets = PivotOffsets(parentFraction = 0.22f),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 64.dp)
                ) {
                    // 1. FULL-BLEED CINEMATIC HERO BANNER (Edge-to-Edge)
                    item(key = "hero_banner") {
                        TvHeroBanner(
                            heroList = heroMovies,
                            onMovieSelect = { movie -> onMovieClick(movie, "hero") },
                            onFocusItem = onFocusItem,
                            firstKeyFocusRequester = playButtonFocusRequester,
                            navFocusRequester = navFocusRequester,
                            onHeroFocusChanged = { isFocused ->
                                if (isFocused) {
                                    scope.launch {
                                        try {
                                            listState.animateScrollToItem(0, 0)
                                        } catch (_: Exception) {}
                                    }
                                }
                            },
                            onDownDpad = {
                                scope.launch {
                                    if (watchHistory.isNotEmpty()) {
                                        var focusLanded = false

                                        // 1. Target the card that was last focused in this row
                                        val lastUrl = watchHistory.getOrNull(lastFocusedHistoryIndex)?.movieUrl
                                        if (lastUrl != null) {
                                            val req = historyCardRequesters[lastUrl]
                                            if (req != null) {
                                                try {
                                                    req.requestFocus()
                                                    focusLanded = true
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        // 2. Target the first visible item in the scrolled LazyRow
                                        if (!focusLanded) {
                                            val visibleIdx = historyListState.firstVisibleItemIndex.coerceIn(0, watchHistory.size - 1)
                                            val visibleUrl = watchHistory.getOrNull(visibleIdx)?.movieUrl
                                            if (visibleUrl != null) {
                                                val req = historyCardRequesters[visibleUrl]
                                                if (req != null) {
                                                    try {
                                                        req.requestFocus()
                                                        focusLanded = true
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        }

                                        // 3. Target any active card currently composed
                                        if (!focusLanded && historyCardRequesters.isNotEmpty()) {
                                            val anyReq = historyCardRequesters.values.firstOrNull()
                                            if (anyReq != null) {
                                                try {
                                                    anyReq.requestFocus()
                                                    focusLanded = true
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        // 4. Fallback: Scroll to target item and request focus
                                        if (!focusLanded) {
                                            val targetIdx = lastFocusedHistoryIndex.coerceIn(0, watchHistory.size - 1)
                                            try {
                                                historyListState.scrollToItem(targetIdx)
                                                delay(50)
                                                val targetUrl = watchHistory.getOrNull(targetIdx)?.movieUrl
                                                if (targetUrl != null && historyCardRequesters.containsKey(targetUrl)) {
                                                    historyCardRequesters[targetUrl]?.requestFocus()
                                                } else {
                                                    firstRowFocusRequester.requestFocus()
                                                }
                                            } catch (_: Exception) {
                                                try {
                                                    firstRowFocusRequester.requestFocus()
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    } else {
                                        try {
                                            firstRowFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    }
                                }
                            },
                            scrollToRow = {},
                            isScrolledDown = isHeroScrolledDown
                        )
                    }

                    // 2. CONTINUAR VIENDO (Rendered only when history exists)
                    if (watchHistory.isNotEmpty()) {
                        item(key = "continue_watching") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.5.dp)
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFF5F2EB))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Continuar viendo para Ti",
                                        color = Color(0xFFF5F2EB),
                                        fontSize = 18.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LazyRow(
                                    state = historyListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(295.dp)
                                        .focusGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(start = 48.dp, top = 8.dp, bottom = 18.dp, end = 48.dp)
                                ) {
                                    itemsIndexed(items = watchHistory.toList(), key = { _, h -> "history-${h.movieUrl}" }) { idx, historyItem ->
                                        val defaultRequester = remember(historyItem.movieUrl) { FocusRequester() }
                                        val cardFocusRequester = if (idx == 0) firstRowFocusRequester else defaultRequester

                                        DisposableEffect(historyItem.movieUrl) {
                                            historyCardRequesters[historyItem.movieUrl] = cardFocusRequester
                                            onDispose {
                                                historyCardRequesters.remove(historyItem.movieUrl)
                                            }
                                        }

                                        LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
                                            if (restoreFocusUrl != null && isHistoryTarget && restoreFocusUrl == historyItem.movieUrl) {
                                                kotlinx.coroutines.delay(60)
                                                try {
                                                    cardFocusRequester.requestFocus()
                                                    onFocusRestored()
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        TvHistoryCard(
                                            historyItem = historyItem,
                                            onClick = {
                                                onMovieClick(
                                                    MovieItem(
                                                        title = historyItem.title,
                                                        url = historyItem.movieUrl,
                                                        image = historyItem.image,
                                                        rating = historyItem.rating,
                                                        postId = historyItem.postId,
                                                        slug = historyItem.slug,
                                                        type = if (historyItem.isSeries) "series" else "movie"
                                                    ),
                                                    "history"
                                                )
                                            },
                                            modifier = Modifier
                                                .focusRequester(cardFocusRequester)
                                                .onPreviewKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                        try {
                                                            playButtonFocusRequester.requestFocus()
                                                            true
                                                        } catch (_: Exception) {
                                                            false
                                                        }
                                                    } else false
                                                }
                                                .onFocusChanged {
                                                    if (it.isFocused) {
                                                        lastFocusedHistoryIndex = idx
                                                        onFocusItem(cardFocusRequester)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. ROW: Películas recién añadidas
                    if (recentMovies.isNotEmpty()) {
                        item(key = "row_movies") {
                            val moviesNeedFirstFocus = watchHistory.isEmpty()
                            PaginatedMovieRow(
                                title = "Películas recién añadidas",
                                initialItems = recentMovies,
                                rowIndex = 1,
                                onMovieSelect = { movie -> onMovieClick(movie, "Películas recién añadidas") },
                                onFocusItem = { req ->
                                    onFocusItem(req)
                                },
                                firstItemFocusRequester = if (moviesNeedFirstFocus) firstRowFocusRequester else null,
                                onUpDpad = if (moviesNeedFirstFocus) {
                                    { try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {} }
                                } else null,
                                categoryType = "movies",
                                orderBy = "latest",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // 4. LOS 10 MÁS POPULARES (Tendencias de LaMovie con números grandes)
                    if (top10Movies.isNotEmpty()) {
                        item(key = "top10") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.5.dp)
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFF5F2EB))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tendencias y más populares",
                                        color = Color(0xFFF5F2EB),
                                        fontSize = 18.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LazyRow(
                                    state = top10ListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(285.dp)
                                        .focusGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(start = 48.dp, top = 8.dp, bottom = 18.dp, end = 48.dp)
                                ) {
                                    itemsIndexed(items = top10Movies, key = { _, movie -> "top10-${movie.url}" }) { rankIndex, movie ->
                                        val cardFocusRequester = remember(movie.url) { FocusRequester() }

                                        LaunchedEffect(movie.url, cardFocusRequester) {
                                            top10CardRequesters[movie.url] = cardFocusRequester
                                        }

                                        LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
                                            if (restoreFocusUrl != null && isTop10Target && restoreFocusUrl == movie.url) {
                                                kotlinx.coroutines.delay(60)
                                                try {
                                                    cardFocusRequester.requestFocus()
                                                    onFocusRestored()
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        TvTop10Card(
                                            movie = movie,
                                            rank = rankIndex + 1,
                                            onClick = { onMovieClick(movie, "top10") },
                                            modifier = Modifier
                                                .focusRequester(cardFocusRequester)
                                                .onFocusChanged {
                                                    if (it.isFocused) {
                                                        lastFocusedTop10Index = rankIndex
                                                        onFocusItem(cardFocusRequester)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. ROW: Series recién añadidas
                    if (series.isNotEmpty()) {
                        item(key = "row_series") {
                            PaginatedMovieRow(
                                title = "Series recién añadidas",
                                initialItems = series,
                                rowIndex = 2,
                                onMovieSelect = { movie -> onMovieClick(movie, "Series recién añadidas") },
                                onFocusItem = onFocusItem,
                                categoryType = "series",
                                orderBy = "latest",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // 6. ROW: Animes recién añadidos
                    if (animes.isNotEmpty()) {
                        item(key = "row_animes") {
                            PaginatedMovieRow(
                                title = "Animes recién añadidos",
                                initialItems = animes,
                                rowIndex = 3,
                                onMovieSelect = { movie -> onMovieClick(movie, "Animes recién añadidos") },
                                onFocusItem = onFocusItem,
                                categoryType = "animes",
                                orderBy = "latest",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // 7. ROW: La Hora del Drama (Novelas y Dramas)
                    if (dramas.isNotEmpty()) {
                        item(key = "row_dramas") {
                            PaginatedMovieRow(
                                title = "La Hora del Drama (Novelas)",
                                initialItems = dramas,
                                rowIndex = 4,
                                onMovieSelect = { movie -> onMovieClick(movie, "La Hora del Drama (Novelas)") },
                                onFocusItem = onFocusItem,
                                categoryType = "series",
                                genreIds = listOf(17, 115),
                                orderBy = "popular",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // 8. ROWS: Dedicated section for each Collection / Saga (UCM, Harry Potter, LOTR, Christopher Nolan)
                    collections.forEachIndexed { idx, collectionSection ->
                        if (collectionSection.movies.isNotEmpty()) {
                            item(key = "collection_${collectionSection.id}") {
                                PaginatedMovieRow(
                                    title = collectionSection.title,
                                    initialItems = collectionSection.movies,
                                    rowIndex = 5 + idx,
                                    onMovieSelect = { movie -> onMovieClick(movie, collectionSection.title) },
                                    onFocusItem = onFocusItem,
                                    restoreFocusUrl = restoreFocusUrl,
                                    restoreFocusRowKey = restoreFocusRowKey,
                                    onFocusRestored = onFocusRestored
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvConnectionErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    val retryFocusRequester = remember { FocusRequester() }
    var isRetryFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        try { retryFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06080C)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            // Elegant Wi-Fi Off Pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF141923))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
            ) {
                M3Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = Color(0xFFF5F2EB),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "SIN CONEXIÓN A INTERNET",
                color = Color(0xFFF5F2EB),
                fontSize = 24.sp,
                fontFamily = BebasNeue,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.8.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message.ifBlank { "No se pudo conectar con los servidores de TV Pro. Comprueba tu conexión de red local e inténtalo de nuevo." },
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp,
                fontFamily = Inter,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.widthIn(max = 520.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            M3Button(
                onClick = onRetry,
                colors = M3ButtonDefaults.buttonColors(
                    containerColor = if (isRetryFocused) Color(0xFFF5F2EB) else Color(0xBF0B0E14),
                    contentColor = if (isRetryFocused) Color.Black else Color(0xFFF5F2EB)
                ),
                border = if (isRetryFocused) BorderStroke(2.dp, Color(0xFFF5F2EB))
                else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                modifier = Modifier
                    .height(44.dp)
                    .focusRequester(retryFocusRequester)
                    .onFocusChanged { isRetryFocused = it.isFocused }
            ) {
                M3Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isRetryFocused) Color.Black else Color(0xFFF5F2EB)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REINTENTAR CONEXIÓN",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}

