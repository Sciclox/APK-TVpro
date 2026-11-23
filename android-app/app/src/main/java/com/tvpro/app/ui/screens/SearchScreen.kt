package com.tvpro.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.SearchHistoryManager
import com.tvpro.app.ui.components.TvMovieCard
import com.tvpro.app.navigation.TvFocusResolver
import com.tvpro.app.ui.theme.Inter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onMovieClick: (MovieItem) -> Unit,
    firstItemFocusRequester: FocusRequester = remember { FocusRequester() },
    navFocusRequester: FocusRequester = remember { FocusRequester() },
    restoreFocusUrl: String? = null,
    onFocusRestored: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var searchResults by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var defaultRecommendations by remember { mutableStateOf<List<MovieItem>>(MovieRepository.localCatalog) }
    var isSearching by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val recentSearches = remember { mutableStateListOf<String>() }

    val scope = rememberCoroutineScope()
    val firstChipFocusRequester = remember { FocusRequester() }
    val firstRecentChipFocusRequester = remember { FocusRequester() }
    val clearHistoryFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }
    val firstKeyFocusRequester = remember { FocusRequester() }
    val zeroFocusRequester = remember { FocusRequester() }
    val spaceFocusRequester = remember { FocusRequester() }
    val backspaceFocusRequester = remember { FocusRequester() }
    val clearFocusRequester = remember { FocusRequester() }
    val filtersListState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val movieFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val keyFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var lastFocusedKeyChar by remember { mutableStateOf("A") }

    val categories = listOf("Todos", "Estrenos", "Acción", "Ciencia Ficción", "Terror", "Comedia", "Anime", "Series")

    // Ergonomic 6-column Alpha-Numeric TV Keyboard Matrix with 'Ñ'
    val keyboardRows = listOf(
        listOf("A", "B", "C", "D", "E", "F"),
        listOf("G", "H", "I", "J", "K", "L"),
        listOf("M", "N", "Ñ", "O", "P", "Q"),
        listOf("R", "S", "T", "U", "V", "W"),
        listOf("X", "Y", "Z", "1", "2", "3"),
        listOf("4", "5", "6", "7", "8", "9")
    )

    suspend fun fetchCategoryPage(category: String, page: Int): MovieRepository.CatalogPageResult {
        return when (category) {
            "Series" -> MovieRepository.fetchCategoryListing("series", page, 30, orderBy = "popular")
            "Anime" -> MovieRepository.fetchCategoryListing("animes", page, 30, orderBy = "popular")
            "Acción" -> MovieRepository.fetchCategoryListing("movies", page, 30, genreIds = listOf(32), orderBy = "popular")
            "Ciencia Ficción" -> MovieRepository.fetchCategoryListing("movies", page, 30, genreIds = listOf(131), orderBy = "popular")
            "Terror" -> MovieRepository.fetchCategoryListing("movies", page, 30, genreIds = listOf(96), orderBy = "popular")
            "Comedia" -> MovieRepository.fetchCategoryListing("movies", page, 30, genreIds = listOf(18), orderBy = "popular")
            "Estrenos" -> MovieRepository.fetchCategoryListing("movies", page, 30, orderBy = "latest")
            else -> MovieRepository.fetchCategoryListing("movies", page, 30, orderBy = "latest")
        }
    }

    // Voice search support check & launcher
    val hasSpeech = remember {
        val pm = context.packageManager
        pm.queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).isNotEmpty()
    }
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
                selectedCategory = "Todos"
                SearchHistoryManager.addRecentSearch(context, spokenText)
                recentSearches.clear()
                recentSearches.addAll(SearchHistoryManager.getRecentSearches(context))
            }
        }
    }
    fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el nombre de una película o serie...")
        }
        try { voiceLauncher.launch(intent) } catch (_: Exception) {}
    }

    // Load initial recommendations and search history on launch
    LaunchedEffect(Unit) {
        recentSearches.clear()
        recentSearches.addAll(SearchHistoryManager.getRecentSearches(context))
        try {
            val catalog = MovieRepository.getCatalog(1)
            if (catalog.movies.isNotEmpty()) {
                defaultRecommendations = catalog.movies
                totalPages = catalog.totalPages.coerceAtLeast(1)
            }
        } catch (_: Exception) {}
    }

    // Debounced search query and category filter (320ms debounce for TV remotes)
    LaunchedEffect(searchQuery, selectedCategory) {
        currentPage = 1
        isLoadingMore = false
        try { gridState.scrollToItem(0) } catch (_: Exception) {}

        if (searchQuery.isBlank() && selectedCategory == "Todos") {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        delay(320)

        try {
            if (searchQuery.isNotBlank()) {
                val results = MovieRepository.searchMovies(searchQuery)
                searchResults = if (selectedCategory != "Todos") {
                    results.filter { movie ->
                        when (selectedCategory) {
                            "Estrenos" -> (movie.year?.toIntOrNull() ?: 0) >= 2024
                            "Acción" -> movie.genres.any { it.contains("Acción", ignoreCase = true) }
                            "Ciencia Ficción" -> movie.genres.any { it.contains("Ciencia Ficción", ignoreCase = true) || it.contains("Ficción", ignoreCase = true) }
                            "Terror" -> movie.genres.any { it.contains("Terror", ignoreCase = true) || it.contains("Suspenso", ignoreCase = true) }
                            "Comedia" -> movie.genres.any { it.contains("Comedia", ignoreCase = true) }
                            "Series" -> movie.type == "series" || movie.url.contains("/series/") || movie.url.contains("/tvshows/")
                            "Anime" -> movie.type == "anime" || movie.url.contains("/animes/")
                            else -> true
                        }
                    }
                } else {
                    results
                }
                totalPages = 1
            } else {
                // searchQuery is blank, query live catalog for selectedCategory
                val catResult = fetchCategoryPage(selectedCategory, 1)
                searchResults = catResult.items
                totalPages = catResult.totalPages.coerceAtLeast(1)
            }
        } catch (_: Exception) {
            searchResults = emptyList()
            totalPages = 1
        } finally {
            isSearching = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || isSearching || searchQuery.isNotBlank() || currentPage >= totalPages) return
        isLoadingMore = true
        scope.launch {
            try {
                val nextPage = currentPage + 1
                if (selectedCategory == "Todos") {
                    val catalog = MovieRepository.getCatalog(nextPage)
                    if (catalog.movies.isNotEmpty()) {
                        val existingUrls = defaultRecommendations.map { it.url }.toSet()
                        val newItems = catalog.movies.filter { it.url !in existingUrls }
                        if (newItems.isNotEmpty()) {
                            defaultRecommendations = defaultRecommendations + newItems
                            currentPage = nextPage
                            if (catalog.totalPages > 0) totalPages = catalog.totalPages
                        } else {
                            totalPages = currentPage
                        }
                    } else {
                        totalPages = currentPage
                    }
                } else {
                    val catResult = fetchCategoryPage(selectedCategory, nextPage)
                    if (catResult.items.isNotEmpty()) {
                        val existingUrls = searchResults.map { it.url }.toSet()
                        val newItems = catResult.items.filter { it.url !in existingUrls }
                        if (newItems.isNotEmpty()) {
                            searchResults = searchResults + newItems
                            currentPage = nextPage
                            if (catResult.totalPages > 0) totalPages = catResult.totalPages
                        } else {
                            totalPages = currentPage
                        }
                    } else {
                        totalPages = currentPage
                    }
                }
            } catch (_: Exception) {}
            isLoadingMore = false
        }
    }

    // Infinite scroll detection via snapshotFlow
    LaunchedEffect(gridState, selectedCategory, searchQuery, currentPage, totalPages) {
        if (searchQuery.isNotBlank()) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 6
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) {
                loadMore()
            }
        }
    }

    val displayMovies = if (searchQuery.isNotBlank() || selectedCategory != "Todos") {
        searchResults
    } else {
        defaultRecommendations
    }

    var lastFocusedResultIndex by remember { mutableIntStateOf(0) }
    var lastFocusedMovieUrl by remember { mutableStateOf<String?>(null) }

    fun requestGridFocus(): Boolean {
        if (displayMovies.isEmpty()) return false
        if (lastFocusedMovieUrl != null) {
            val req = movieFocusRequesters[lastFocusedMovieUrl]
            if (TvFocusResolver.safeRequestFocus(req)) return true
        }
        if (TvFocusResolver.safeRequestFocus(firstResultFocusRequester)) return true
        val visibleFirst = gridState.firstVisibleItemIndex
        val visibleMovie = displayMovies.getOrNull(visibleFirst)
        if (visibleMovie != null) {
            val req = movieFocusRequesters[visibleMovie.url]
            if (TvFocusResolver.safeRequestFocus(req)) return true
        }
        for (req in movieFocusRequesters.values) {
            if (TvFocusResolver.safeRequestFocus(req)) return true
        }
        scope.launch {
            try {
                val target = lastFocusedResultIndex.coerceIn(0, displayMovies.lastIndex)
                gridState.scrollToItem(target)
                delay(40)
                val movie = displayMovies.getOrNull(target)
                if (movie != null) movieFocusRequesters[movie.url]?.requestFocus()
                else firstResultFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
        return true
    }

    // Restore focus to previously clicked movie card in search results
    LaunchedEffect(restoreFocusUrl, displayMovies) {
        if (restoreFocusUrl != null && displayMovies.any { it.url == restoreFocusUrl }) {
            val targetIdx = displayMovies.indexOfFirst { it.url == restoreFocusUrl }
            if (targetIdx >= 0) {
                try {
                    gridState.scrollToItem(targetIdx)
                } catch (_: Exception) {}
                delay(150)
                try {
                    movieFocusRequesters[restoreFocusUrl]?.requestFocus()
                    onFocusRestored()
                } catch (_: Exception) {}
            }
        }
    }

    // Luxury Design Tokens
    val deepBackground = Color(0xFF06080C)
    val warmIvory = Color(0xFFF5F2EB)
    val pillSurface = Color(0xBF0B0E14)
    val subtleBorder = Color.White.copy(alpha = 0.12f)
    val mutedSlate = Color(0xFF9CA3AF)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(deepBackground)
            .padding(start = 48.dp, end = 48.dp, top = 82.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        // =========================================================================
        // LEFT PANEL: Search Input Bar + 6x6 TV Remote Keyboard + Actions
        // =========================================================================
        Column(
            modifier = Modifier.width(320.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Search Input Bar
            var isSearchBoxFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSearchBoxFocused) Color(0xFF161B26) else pillSurface)
                    .border(
                        width = if (isSearchBoxFocused) 2.dp else 1.dp,
                        color = if (isSearchBoxFocused) warmIvory else subtleBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .focusRequester(firstItemFocusRequester)
                    .onFocusChanged { isSearchBoxFocused = it.isFocused || it.hasFocus }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            val char = event.nativeKeyEvent.unicodeChar.toChar()
                            if (char.isLetterOrDigit() || char == ' ' || char == 'ñ' || char == 'Ñ') {
                                searchQuery += char
                                selectedCategory = "Todos"
                                return@onPreviewKeyEvent true
                            } else if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL) {
                                if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1)
                                return@onPreviewKeyEvent true
                            }
                            when (event.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                    if (hasSpeech) {
                                        launchVoiceSearch()
                                    } else {
                                        val targetReq = keyFocusRequesters[lastFocusedKeyChar] ?: firstKeyFocusRequester
                                        try { targetReq.requestFocus() } catch (_: Exception) {}
                                    }
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    val targetReq = keyFocusRequesters[lastFocusedKeyChar] ?: firstKeyFocusRequester
                                    try { targetReq.requestFocus() } catch (_: Exception) {}
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (hasSpeech) {
                            launchVoiceSearch()
                        } else {
                            val targetReq = keyFocusRequesters[lastFocusedKeyChar] ?: firstKeyFocusRequester
                            try { targetReq.requestFocus() } catch (_: Exception) {}
                        }
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = if (isSearchBoxFocused) warmIvory else mutedSlate,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (hasSpeech && isSearchBoxFocused) "Presiona OK para dictar..." else "Buscar películas, series...",
                                color = mutedSlate,
                                fontSize = 13.5.sp,
                                fontFamily = Inter
                            )
                        } else {
                            Text(
                                text = searchQuery,
                                color = warmIvory,
                                fontSize = 14.5.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (hasSpeech) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Búsqueda por voz",
                            tint = if (isSearchBoxFocused) warmIvory else mutedSlate,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // 2. Ergonomic 6-column TV Remote Virtual Keyboard with 'Ñ'
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x800A0D14))
                    .border(1.dp, subtleBorder, RoundedCornerShape(14.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                keyboardRows.forEachIndexed { rowIndex, rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowKeys.forEachIndexed { colIndex, keyChar ->
                            var isKeyFocused by remember(keyChar) { mutableStateOf(false) }
                            val scaleAnim by animateFloatAsState(
                                targetValue = if (isKeyFocused) 1.12f else 1.0f,
                                animationSpec = tween(120),
                                label = "keyScale_$keyChar"
                            )

                            val keyRequester = remember(keyChar) {
                                if (rowIndex == 0 && colIndex == 0) firstKeyFocusRequester else FocusRequester()
                            }
                            LaunchedEffect(keyChar, keyRequester) {
                                keyFocusRequesters[keyChar] = keyRequester
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .zIndex(if (isKeyFocused) 10f else 1f)
                                    .graphicsLayer {
                                        scaleX = scaleAnim
                                        scaleY = scaleAnim
                                    }
                                    .shadow(
                                        elevation = if (isKeyFocused) 10.dp else 0.dp,
                                        shape = RoundedCornerShape(8.dp),
                                        spotColor = Color.White.copy(alpha = 0.65f),
                                        ambientColor = Color.White.copy(alpha = 0.35f)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isKeyFocused) warmIvory else pillSurface
                                    )
                                    .border(
                                        width = if (isKeyFocused) 2.dp else 1.dp,
                                        color = if (isKeyFocused) Color.White else subtleBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .focusRequester(keyRequester)
                                    .onFocusChanged {
                                        isKeyFocused = it.isFocused || it.hasFocus
                                        if (it.isFocused) {
                                            lastFocusedKeyChar = keyChar
                                        }
                                    }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown) {
                                            when (event.nativeKeyEvent.keyCode) {
                                                KeyEvent.KEYCODE_DPAD_UP -> {
                                                    if (rowIndex == 0) {
                                                        try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    } else false
                                                }
                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    if (rowIndex == keyboardRows.size - 1) {
                                                        when (colIndex) {
                                                            0 -> {
                                                                try { zeroFocusRequester.requestFocus() } catch (_: Exception) {}
                                                                true
                                                            }
                                                            1, 2 -> {
                                                                try { spaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                                                true
                                                            }
                                                            3, 4 -> {
                                                                try { backspaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                                                true
                                                            }
                                                            else -> {
                                                                if (searchQuery.isNotEmpty()) {
                                                                    try { clearFocusRequester.requestFocus() } catch (_: Exception) {}
                                                                } else {
                                                                    try { backspaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                                                }
                                                                true
                                                            }
                                                        }
                                                    } else false
                                                }
                                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                    if (colIndex == rowKeys.size - 1) {
                                                        if (displayMovies.isNotEmpty()) {
                                                            if (!requestGridFocus()) {
                                                                try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            }
                                                        } else {
                                                            try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        }
                                                        true
                                                    } else false
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        searchQuery += keyChar.lowercase()
                                        selectedCategory = "Todos"
                                    }
                            ) {
                                Text(
                                    text = keyChar,
                                    color = if (isKeyFocused) Color(0xFF06080C) else warmIvory,
                                    fontSize = 13.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = if (isKeyFocused) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Compact Utility Action Bar ('0', Espacio, Borrar, Limpiar)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Key '0'
                var isZeroFocused by remember { mutableStateOf(false) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(if (searchQuery.isNotEmpty()) 0.85f else 0.9f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isZeroFocused) warmIvory else pillSurface)
                        .border(
                            width = if (isZeroFocused) 2.dp else 1.dp,
                            color = if (isZeroFocused) Color.White else subtleBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .focusRequester(zeroFocusRequester)
                        .onFocusChanged {
                            isZeroFocused = it.isFocused || it.hasFocus
                            if (it.isFocused) lastFocusedKeyChar = "4"
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        try { keyFocusRequesters["4"]?.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        try { spaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_LEFT -> true
                                    KeyEvent.KEYCODE_DPAD_DOWN -> true
                                    else -> false
                                }
                            } else false
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            searchQuery += "0"
                            selectedCategory = "Todos"
                        }
                ) {
                    Text(
                        text = "0",
                        fontSize = 13.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = if (isZeroFocused) Color(0xFF06080C) else warmIvory
                    )
                }

                // Espacio
                var isSpaceFocused by remember { mutableStateOf(false) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(if (searchQuery.isNotEmpty()) 1.5f else 1.8f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSpaceFocused) warmIvory else pillSurface)
                        .border(
                            width = if (isSpaceFocused) 2.dp else 1.dp,
                            color = if (isSpaceFocused) Color.White else subtleBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .focusRequester(spaceFocusRequester)
                        .onFocusChanged {
                            isSpaceFocused = it.isFocused || it.hasFocus
                            if (it.isFocused) lastFocusedKeyChar = "5"
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        try { keyFocusRequesters["5"]?.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        try { zeroFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        try { backspaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_DOWN -> true
                                    else -> false
                                }
                            } else false
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { searchQuery += " " }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SpaceBar,
                            contentDescription = "Espacio",
                            tint = if (isSpaceFocused) Color(0xFF06080C) else warmIvory,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Espacio",
                            fontSize = 11.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSpaceFocused) Color(0xFF06080C) else warmIvory
                        )
                    }
                }

                // Borrar (Backspace)
                var isBackspaceFocused by remember { mutableStateOf(false) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(if (searchQuery.isNotEmpty()) 1.25f else 1.5f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBackspaceFocused) warmIvory else pillSurface)
                        .border(
                            width = if (isBackspaceFocused) 2.dp else 1.dp,
                            color = if (isBackspaceFocused) Color.White else subtleBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .focusRequester(backspaceFocusRequester)
                        .onFocusChanged {
                            isBackspaceFocused = it.isFocused || it.hasFocus
                            if (it.isFocused) lastFocusedKeyChar = "7"
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        val req = keyFocusRequesters["7"] ?: keyFocusRequesters["8"]
                                        try { req?.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        try { spaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        if (searchQuery.isNotEmpty()) {
                                            try { clearFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        } else {
                                            if (displayMovies.isNotEmpty()) {
                                                if (!requestGridFocus()) {
                                                    try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                }
                                            } else {
                                                try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                            }
                                            true
                                        }
                                    }
                                    KeyEvent.KEYCODE_DPAD_DOWN -> true
                                    else -> false
                                }
                            } else false
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = searchQuery.dropLast(1)
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Borrar",
                            tint = if (isBackspaceFocused) Color(0xFF06080C) else warmIvory,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Borrar",
                            fontSize = 11.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBackspaceFocused) Color(0xFF06080C) else warmIvory
                        )
                    }
                }

                // Limpiar (Clear) - Visible when query has text
                if (searchQuery.isNotEmpty()) {
                    var isClearFocused by remember { mutableStateOf(false) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1.1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isClearFocused) warmIvory else pillSurface)
                            .border(
                                width = if (isClearFocused) 2.dp else 1.dp,
                                color = if (isClearFocused) Color.White else subtleBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .focusRequester(clearFocusRequester)
                            .onFocusChanged {
                                isClearFocused = it.isFocused || it.hasFocus
                                if (it.isFocused) lastFocusedKeyChar = "9"
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { keyFocusRequesters["9"]?.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            try { backspaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            if (displayMovies.isNotEmpty()) {
                                                if (!requestGridFocus()) {
                                                    try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                }
                                            } else {
                                                try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                            }
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> true
                                        else -> false
                                    }
                                } else false
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                searchQuery = ""
                                try { backspaceFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = if (isClearFocused) Color(0xFF06080C) else warmIvory,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Limpiar",
                                fontSize = 11.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isClearFocused) Color(0xFF06080C) else warmIvory
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // RIGHT PANEL: Category Chips + Live Results Header + Spacious Movie Grid
        // =========================================================================
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Quick Category Filter Chips
            LazyRow(
                state = filtersListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                itemsIndexed(categories) { index, cat ->
                    val isSelected = (selectedCategory == cat && searchQuery.isBlank())
                    var isChipFocused by remember(cat) { mutableStateOf(false) }

                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isChipFocused) 1.08f else 1.0f,
                        animationSpec = tween(durationMillis = 150),
                        label = "chipScale_$cat"
                    )

                    val chipShape = RoundedCornerShape(50)

                    Box(
                        modifier = Modifier
                            .zIndex(if (isChipFocused) 10f else 1f)
                            .graphicsLayer {
                                scaleX = scaleAnim
                                scaleY = scaleAnim
                            }
                            .shadow(
                                elevation = if (isChipFocused) 12.dp else 0.dp,
                                shape = chipShape,
                                spotColor = Color.White.copy(alpha = 0.65f),
                                ambientColor = Color.White.copy(alpha = 0.35f)
                            )
                            .clip(chipShape)
                            .background(
                                when {
                                    isChipFocused -> warmIvory
                                    isSelected -> Color(0xFF1E2433)
                                    else -> pillSurface
                                }
                            )
                            .border(
                                width = if (isChipFocused) 2.5.dp else 1.dp,
                                color = when {
                                    isChipFocused -> Color.White
                                    isSelected -> warmIvory.copy(alpha = 0.60f)
                                    else -> subtleBorder
                                },
                                shape = chipShape
                            )
                            .then(if (index == 0) Modifier.focusRequester(firstChipFocusRequester) else Modifier)
                            .onFocusChanged { focusState ->
                                isChipFocused = focusState.isFocused || focusState.hasFocus
                                if (focusState.isFocused || focusState.hasFocus) {
                                    scope.launch {
                                        try { filtersListState.animateScrollToItem(index) } catch (_: Exception) {}
                                    }
                                }
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                                                try { firstRecentChipFocusRequester.requestFocus() } catch (_: Exception) {
                                                    requestGridFocus()
                                                }
                                                true
                                            } else if (displayMovies.isNotEmpty()) {
                                                requestGridFocus()
                                                true
                                            } else false
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            if (index == 0) {
                                                try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            } else false
                                        }
                                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                            selectedCategory = cat
                                            searchQuery = ""
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedCategory = cat
                                searchQuery = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            color = when {
                                isChipFocused -> Color(0xFF06080C)
                                isSelected -> warmIvory
                                else -> mutedSlate
                            },
                            fontSize = 12.sp,
                            fontFamily = Inter,
                            fontWeight = if (isSelected || isChipFocused) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Recent Searches Row (visible when query is empty and recent searches exist)
            if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                val visibleRecents = remember(recentSearches.toList()) { recentSearches.take(4) }
                val recentRequesters = remember(visibleRecents) {
                    List(visibleRecents.size) { FocusRequester() }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Búsquedas recientes",
                        tint = mutedSlate,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Recientes:",
                        color = mutedSlate,
                        fontSize = 11.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium
                    )
                    visibleRecents.forEachIndexed { termIndex, term ->
                        var isRecentChipFocused by remember(term) { mutableStateOf(false) }
                        val requester = if (termIndex == 0) firstRecentChipFocusRequester else recentRequesters.getOrElse(termIndex) { remember(term) { FocusRequester() } }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isRecentChipFocused) warmIvory else pillSurface)
                                .border(
                                    width = if (isRecentChipFocused) 2.dp else 1.dp,
                                    color = if (isRecentChipFocused) Color.White else subtleBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .focusRequester(requester)
                                .onFocusChanged { isRecentChipFocused = it.isFocused || it.hasFocus }
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                if (displayMovies.isNotEmpty()) {
                                                    requestGridFocus()
                                                    true
                                                } else false
                                            }
                                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (termIndex == 0) {
                                                    val targetReq = keyFocusRequesters[lastFocusedKeyChar] ?: firstKeyFocusRequester
                                                    try { targetReq.requestFocus() } catch (_: Exception) {}
                                                    true
                                                } else {
                                                    val prevReq = if (termIndex == 1) firstRecentChipFocusRequester else recentRequesters[termIndex - 1]
                                                    try { prevReq.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                            }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (termIndex < visibleRecents.size - 1) {
                                                    try { recentRequesters[termIndex + 1].requestFocus() } catch (_: Exception) {}
                                                    true
                                                } else {
                                                    try { clearHistoryFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                            }
                                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                                searchQuery = term
                                                selectedCategory = "Todos"
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    searchQuery = term
                                    selectedCategory = "Todos"
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = term,
                                color = if (isRecentChipFocused) Color(0xFF06080C) else warmIvory,
                                fontSize = 11.5.sp,
                                fontFamily = Inter,
                                fontWeight = if (isRecentChipFocused) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    var isClearHistFocused by remember { mutableStateOf(false) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isClearHistFocused) warmIvory else Color.Transparent)
                            .border(
                                width = if (isClearHistFocused) 2.dp else 1.dp,
                                color = if (isClearHistFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            )
                            .focusRequester(clearHistoryFocusRequester)
                            .onFocusChanged { isClearHistFocused = it.isFocused || it.hasFocus }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (displayMovies.isNotEmpty()) {
                                                requestGridFocus()
                                                true
                                            } else false
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            val lastReq = if (visibleRecents.size == 1) firstRecentChipFocusRequester else recentRequesters.getOrNull(visibleRecents.size - 1) ?: firstRecentChipFocusRequester
                                            try { lastReq.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> true
                                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                            SearchHistoryManager.clearHistory(context)
                                            recentSearches.clear()
                                            try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                SearchHistoryManager.clearHistory(context)
                                recentSearches.clear()
                                try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Borrar historial",
                            color = if (isClearHistFocused) Color(0xFF06080C) else mutedSlate.copy(alpha = 0.8f),
                            fontSize = 10.5.sp,
                            fontFamily = Inter,
                            fontWeight = if (isClearHistFocused) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 3. Results Header with Discreet Inline Loading
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(warmIvory)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isSearching) {
                    CircularProgressIndicator(
                        color = warmIvory,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                }
                Text(
                    text = when {
                        isSearching && searchQuery.isNotBlank() -> "Buscando \"$searchQuery\"..."
                        isSearching -> "Cargando..."
                        searchQuery.isNotBlank() -> "Resultados para \"$searchQuery\""
                        selectedCategory != "Todos" -> "Películas de $selectedCategory"
                        else -> "Títulos recomendados para ti"
                    },
                    color = warmIvory,
                    fontSize = 16.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold
                )
                if (!isSearching && displayMovies.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "(${displayMovies.size} títulos)",
                        color = mutedSlate,
                        fontSize = 12.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium
                    )
                    if (isLoadingMore) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            color = warmIvory,
                            strokeWidth = 1.5.dp,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // 4. Grid of Movie Posters (Adaptive 3-4 Columns with Smooth In-Place Updates)
            if (isSearching && displayMovies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = warmIvory, strokeWidth = 2.5.dp)
                }
            } else if (!isSearching && displayMovies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = mutedSlate.copy(alpha = 0.45f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "No encontramos resultados para \"$searchQuery\""
                                selectedCategory != "Todos" -> "No encontramos títulos en \"$selectedCategory\""
                                else -> "No hay títulos disponibles"
                            },
                            color = warmIvory,
                            fontSize = 15.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Prueba con otro término o selecciona una categoría de la lista."
                                   else "Intenta seleccionar otra categoría o buscar por título.",
                            color = mutedSlate,
                            fontSize = 12.5.sp,
                            fontFamily = Inter
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 142.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (isSearching) 0.60f else 1.0f
                        }
                ) {
                    itemsIndexed(displayMovies, key = { idx, m -> "${m.url}_$idx" }) { index, movie ->
                        val cardRequester = if (index == 0) firstResultFocusRequester else remember(movie.url) { FocusRequester() }

                        LaunchedEffect(movie.url, cardRequester) {
                            movieFocusRequesters[movie.url] = cardRequester
                        }

                        LaunchedEffect(restoreFocusUrl) {
                            if (restoreFocusUrl != null && restoreFocusUrl == movie.url) {
                                delay(60)
                                try {
                                    cardRequester.requestFocus()
                                    onFocusRestored()
                                } catch (_: Exception) {}
                            }
                        }

                        TvMovieCard(
                            movie = movie,
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    SearchHistoryManager.addRecentSearch(context, searchQuery)
                                }
                                onMovieClick(movie)
                            },
                            fillWidth = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .focusRequester(cardRequester)
                                .onFocusChanged {
                                    if (it.isFocused || it.hasFocus) {
                                        lastFocusedResultIndex = index
                                        lastFocusedMovieUrl = movie.url
                                        if (index >= displayMovies.size - 6 && searchQuery.isBlank()) {
                                            loadMore()
                                        }
                                    }
                                }
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                if (index < 3) {
                                                    if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                                                        try { firstRecentChipFocusRequester.requestFocus() } catch (_: Exception) {
                                                            try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        }
                                                    } else {
                                                        try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    }
                                                    true
                                                } else false
                                            }
                                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (index % 3 == 0) {
                                                    val targetReq = keyFocusRequesters[lastFocusedKeyChar] ?: firstKeyFocusRequester
                                                    try { targetReq.requestFocus() } catch (_: Exception) {}
                                                    true
                                                } else false
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                        )
                    }

                    if (isLoadingMore) {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "search_loading_more_footer"
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = warmIvory,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Cargando más títulos...",
                                        color = mutedSlate,
                                        fontSize = 12.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Medium
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
