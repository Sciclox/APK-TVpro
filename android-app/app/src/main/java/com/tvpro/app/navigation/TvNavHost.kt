package com.tvpro.app.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.StreamItem
import com.tvpro.app.ui.components.NetflixTopNav
import com.tvpro.app.ui.screens.*
import com.tvpro.app.ui.theme.NetflixBlack
import com.tvpro.app.ui.theme.TvProTheme
import kotlinx.coroutines.delay

@Composable
fun TvNavHost(activityIntent: Intent?) {
    TvProTheme {
        val screenStack = remember { mutableStateListOf(TvScreen.SPLASH) }
        val currentScreen = screenStack.lastOrNull() ?: TvScreen.HOME
        var selectedMovie by remember { mutableStateOf<MovieItem?>(null) }
        var playerMovie by remember { mutableStateOf<MovieItem?>(null) }
        var selectedStreamUrl by remember { mutableStateOf<String?>(null) }
        var selectedStreams by remember { mutableStateOf<List<StreamItem>>(emptyList()) }

        // Focus restoration: remember which movie card was clicked before going to Detail
        var lastClickedMovieUrl by remember { mutableStateOf<String?>(null) }
        var lastClickedRowKey by remember { mutableStateOf<String?>(null) }
        var restoreFocusUrl by remember { mutableStateOf<String?>(null) }
        var restoreFocusRowKey by remember { mutableStateOf<String?>(null) }

        // Auto-clear restoreFocusUrl after safety timeout
        LaunchedEffect(restoreFocusUrl) {
            if (restoreFocusUrl != null) {
                delay(350)
                restoreFocusUrl = null
                restoreFocusRowKey = null
            }
        }

        // Nav state
        var selectedNavId by remember { mutableStateOf("home") }
        var hasLandedInitialFocus by remember { mutableStateOf(false) }
        var isTopNavFocused by remember { mutableStateOf(false) }
        val searchFocusRequester = remember { FocusRequester() }
        val homeFocusRequester = remember { FocusRequester() }
        val moviesFocusRequester = remember { FocusRequester() }
        val seriesFocusRequester = remember { FocusRequester() }
        val animesFocusRequester = remember { FocusRequester() }
        val myListFocusRequester = remember { FocusRequester() }
        val playButtonFocusRequester = remember { FocusRequester() }
        val catalogFirstFocusRequester = remember { FocusRequester() }

        // Deep Link Handler
        LaunchedEffect(activityIntent) {
            val intentUri = activityIntent?.data
            if (intentUri != null && intentUri.scheme == "tvpro" && intentUri.host == "play") {
                val movieUrl = intentUri.getQueryParameter("url") ?: ""
                val title = intentUri.getQueryParameter("title") ?: "Pelicula"
                if (movieUrl.isNotBlank()) {
                    val isSeries = movieUrl.contains("/tvshows/") || movieUrl.contains("/series/") || movieUrl.contains("/animes/")
                    val m = MovieItem(
                        title = title,
                        url = movieUrl,
                        image = "",
                        type = if (isSeries) "series" else "movie"
                    )
                    selectedMovie = m
                    playerMovie = m
                    screenStack.add(TvScreen.PLAYER)
                }
            }
        }

        // Map nav ids to screens
        val navToScreen = mapOf(
            "home" to TvScreen.HOME,
            "movies" to TvScreen.MOVIES,
            "series" to TvScreen.SERIES,
            "animes" to TvScreen.ANIMES,
            "search" to TvScreen.SEARCH,
            "my_list" to TvScreen.MY_LIST
        )

        // Back Key Handler: If in stack > 1, pop stack. If at root of a category (MOVIES, SERIES, ANIMES, etc.), go back to HOME.
        BackHandler(enabled = screenStack.size > 1 || (currentScreen != TvScreen.HOME && currentScreen != TvScreen.SPLASH)) {
            if (screenStack.size > 1) {
                val removedScreen = screenStack.removeAt(screenStack.size - 1)
                // Restore focus to the clicked card when returning from Detail
                if (removedScreen == TvScreen.DETAIL) {
                    restoreFocusUrl = lastClickedMovieUrl
                    restoreFocusRowKey = lastClickedRowKey
                }
                // Restore nav selection based on screen we're going back to
                val backTo = screenStack.lastOrNull()
                when (backTo) {
                    TvScreen.HOME -> selectedNavId = "home"
                    TvScreen.MOVIES -> selectedNavId = "movies"
                    TvScreen.SERIES -> selectedNavId = "series"
                    TvScreen.ANIMES -> selectedNavId = "animes"
                    TvScreen.SEARCH -> selectedNavId = "search"
                    TvScreen.MY_LIST -> selectedNavId = "my_list"
                    else -> {}
                }
            } else if (currentScreen != TvScreen.HOME && currentScreen != TvScreen.SPLASH) {
                screenStack.clear()
                screenStack.add(TvScreen.HOME)
                selectedNavId = "home"
            }
        }

        val overlayScreens = listOf(TvScreen.DETAIL, TvScreen.PLAYER)
        val baseScreen = screenStack.lastOrNull { it !in overlayScreens } ?: TvScreen.HOME

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NetflixBlack)
        ) {
            // Content Layer: Base screen (Home, Movies, Series, Animes, Search, My List)
            // Kept alive so navigating back from Detail retains exact scroll and focus positions
            Box(modifier = Modifier.fillMaxSize()) {
                        when (baseScreen) {
                    TvScreen.SPLASH -> {
                        SplashScreen(
                            onSplashFinished = {
                                screenStack.clear()
                                screenStack.add(TvScreen.HOME)
                            }
                        )
                    }
                    TvScreen.HOME -> {
                        HomeScreen(
                            onMovieClick = { movie, rowKey ->
                                lastClickedMovieUrl = movie.url
                                lastClickedRowKey = rowKey
                                selectedMovie = movie
                                screenStack.add(TvScreen.DETAIL)
                            },
                            onPlayClick = { movie ->
                                selectedMovie = movie
                                playerMovie = movie
                                val fallbackStreams = com.tvpro.core.data.repository.MovieRepository.getStreamsForMovie(movie.url)
                                selectedStreamUrl = fallbackStreams.firstOrNull()?.url
                                selectedStreams = fallbackStreams
                                screenStack.add(TvScreen.PLAYER)
                            },
                            playButtonFocusRequester = playButtonFocusRequester,
                            navFocusRequester = homeFocusRequester,
                            selectedNavId = selectedNavId,
                            shouldRequestInitialFocus = !hasLandedInitialFocus && !isTopNavFocused,
                            onInitialFocusLanded = { hasLandedInitialFocus = true },
                            restoreFocusUrl = restoreFocusUrl,
                            restoreFocusRowKey = restoreFocusRowKey,
                            onFocusRestored = {
                                restoreFocusUrl = null
                                restoreFocusRowKey = null
                            }
                        )
                    }
                    TvScreen.MOVIES -> {
                        CategoryCatalogScreen(
                            categoryType = "movies",
                            onMovieClick = { movie, rowKey ->
                                lastClickedMovieUrl = movie.url
                                lastClickedRowKey = rowKey
                                selectedMovie = movie
                                screenStack.add(TvScreen.DETAIL)
                            },
                            firstItemFocusRequester = catalogFirstFocusRequester,
                            navFocusRequester = moviesFocusRequester,
                            restoreFocusUrl = restoreFocusUrl,
                            restoreFocusRowKey = restoreFocusRowKey,
                            onFocusRestored = {
                                restoreFocusUrl = null
                                restoreFocusRowKey = null
                            }
                        )
                    }
                    TvScreen.SERIES -> {
                        CategoryCatalogScreen(
                            categoryType = "series",
                            onMovieClick = { movie, rowKey ->
                                lastClickedMovieUrl = movie.url
                                lastClickedRowKey = rowKey
                                selectedMovie = movie
                                screenStack.add(TvScreen.DETAIL)
                            },
                            firstItemFocusRequester = catalogFirstFocusRequester,
                            navFocusRequester = seriesFocusRequester,
                            restoreFocusUrl = restoreFocusUrl,
                            restoreFocusRowKey = restoreFocusRowKey,
                            onFocusRestored = {
                                restoreFocusUrl = null
                                restoreFocusRowKey = null
                            }
                        )
                    }
                    TvScreen.ANIMES -> {
                        CategoryCatalogScreen(
                            categoryType = "animes",
                            onMovieClick = { movie, rowKey ->
                                lastClickedMovieUrl = movie.url
                                lastClickedRowKey = rowKey
                                selectedMovie = movie
                                screenStack.add(TvScreen.DETAIL)
                            },
                            firstItemFocusRequester = catalogFirstFocusRequester,
                            navFocusRequester = animesFocusRequester,
                            restoreFocusUrl = restoreFocusUrl,
                            restoreFocusRowKey = restoreFocusRowKey,
                            onFocusRestored = {
                                restoreFocusUrl = null
                                restoreFocusRowKey = null
                            }
                        )
                    }
                    TvScreen.SEARCH -> {
                        SearchScreen(
                            onMovieClick = { movie ->
                                lastClickedMovieUrl = movie.url
                                lastClickedRowKey = "search_grid"
                                selectedMovie = movie
                                screenStack.add(TvScreen.DETAIL)
                            },
                            firstItemFocusRequester = catalogFirstFocusRequester,
                            navFocusRequester = searchFocusRequester,
                            restoreFocusUrl = restoreFocusUrl,
                            onFocusRestored = {
                                restoreFocusUrl = null
                                restoreFocusRowKey = null
                            }
                        )
                    }
                    TvScreen.MY_LIST -> {
                        MyListScreen(
                            onMovieClick = { movie ->
                                lastClickedMovieUrl = movie.url
                                lastClickedRowKey = "mylist_grid"
                                selectedMovie = movie
                                screenStack.add(TvScreen.DETAIL)
                            },
                            navFocusRequester = myListFocusRequester,
                            firstItemFocusRequester = catalogFirstFocusRequester,
                            restoreFocusUrl = restoreFocusUrl,
                            onFocusRestored = {
                                restoreFocusUrl = null
                                restoreFocusRowKey = null
                            }
                        )
                    }
                    else -> {}
                }
            }

            // Overlay Screen: Detail
            if (screenStack.contains(TvScreen.DETAIL)) {
                selectedMovie?.let { movie ->
                    DetailScreen(
                        movie = movie,
                        isPlayerActive = screenStack.contains(TvScreen.PLAYER),
                        onPlayClick = { m, url, streams ->
                            playerMovie = m
                                  selectedStreamUrl = url
                            selectedStreams = streams
                            screenStack.add(TvScreen.PLAYER)
                        },
                        onBackClick = {
                            restoreFocusUrl = lastClickedMovieUrl
                            restoreFocusRowKey = lastClickedRowKey
                            screenStack.removeAll { it == TvScreen.DETAIL }
                        },
                        onMovieSelect = { nextMovie ->
                            selectedMovie = nextMovie
                        }
                    )
                }
            }

            // Overlay Screen: Player
            if (screenStack.contains(TvScreen.PLAYER)) {
                (playerMovie ?: selectedMovie)?.let { movie ->
                    PlayerScreen(
                        movie = movie,
                        streamUrl = selectedStreamUrl,
                        streams = selectedStreams,
                        onBackClick = {
                            screenStack.removeAll { it == TvScreen.PLAYER }
                        }
                    )
                }
            }

            // Top Navigation Overlay (Hidden on Splash, Player, and Detail screens)
            if (currentScreen != TvScreen.SPLASH && currentScreen != TvScreen.PLAYER && currentScreen != TvScreen.DETAIL) {
                // Gradient Scrim to ensure scrolled movies never clash with nav buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.98f),
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                NetflixTopNav(
                    selectedId = selectedNavId,
                    onFocusChanged = { isFocused ->
                        isTopNavFocused = isFocused
                    },
                    onSelect = { id ->
                        selectedNavId = id
                        val targetScreen = navToScreen[id]
                        if (targetScreen != null && currentScreen != targetScreen) {
                            // Navigate to screen - replace current non-overlay screen
                            val overlayScreens = listOf(TvScreen.DETAIL, TvScreen.PLAYER)
                            // Pop back to root, then push new screen
                            while (screenStack.size > 1 && screenStack.last() !in overlayScreens) {
                                screenStack.removeAt(screenStack.size - 1)
                            }
                            if (screenStack.lastOrNull() != targetScreen) {
                                if (screenStack.isEmpty()) {
                                    screenStack.add(targetScreen)
                                } else {
                                    screenStack[0] = targetScreen
                                    while (screenStack.size > 1) {
                                        screenStack.removeAt(screenStack.size - 1)
                                    }
                                }
                            }
                        }
                    },
                    searchFocusRequester = searchFocusRequester,
                    homeFocusRequester = homeFocusRequester,
                    moviesFocusRequester = moviesFocusRequester,
                    seriesFocusRequester = seriesFocusRequester,
                    animesFocusRequester = animesFocusRequester,
                    myListFocusRequester = myListFocusRequester,
                    onDownDpad = {
                        val activeScreen = screenStack.lastOrNull() ?: currentScreen
                        when (activeScreen) {
                            TvScreen.HOME -> {
                                try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                            TvScreen.MOVIES, TvScreen.SERIES, TvScreen.ANIMES, TvScreen.SEARCH, TvScreen.MY_LIST -> {
                                try { catalogFirstFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier.statusBarsPadding()
                )
            }
        }
    }
}

