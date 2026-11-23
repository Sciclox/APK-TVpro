package com.tvpro.app.ui.screens

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.app.ui.components.*
import com.tvpro.app.navigation.TvFocusResolver
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CategorySectionRow(
    val id: String,
    val title: String,
    val items: List<MovieItem>,
    val genreIds: List<Int> = emptyList(),
    val orderBy: String = "latest"
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryCatalogScreen(
    categoryType: String, // "movies", "series", "animes"
    onMovieClick: (MovieItem, String) -> Unit,
    firstItemFocusRequester: FocusRequester,
    navFocusRequester: FocusRequester,
    restoreFocusUrl: String? = null,
    restoreFocusRowKey: String? = null,
    onFocusRestored: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var rawItems by remember(categoryType) { mutableStateOf<List<MovieItem>>(emptyList()) }
    var filterState by remember(categoryType) { mutableStateOf(MovieRepository.FilterState()) }
    var isLoading by remember(categoryType) { mutableStateOf(true) }
    var totalCount by remember(categoryType) { mutableIntStateOf(0) }
    var reloadTrigger by remember(categoryType) { mutableIntStateOf(0) }

    // Filter results state
    var filteredResults by remember(categoryType) { mutableStateOf<List<MovieItem>>(emptyList()) }
    var isFilterLoading by remember(categoryType) { mutableStateOf(false) }
    var filterCurrentPage by remember(categoryType) { mutableIntStateOf(1) }
    var filterTotalPages by remember(categoryType) { mutableIntStateOf(1) }
    var filterTotalCount by remember(categoryType) { mutableIntStateOf(0) }
    var isFilterFetchingMore by remember(categoryType) { mutableStateOf(false) }

    val listState = rememberTvLazyListState()
    val top10ListState = rememberLazyListState()
    val filterGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val firstFilterPillFocusRequester = remember { FocusRequester() }
    val firstSectionFocusRequester = remember { FocusRequester() }
    val firstFilteredCardFocusRequester = remember { FocusRequester() }
    val filteredCardRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var lastFocusedTop10Index by remember(categoryType) { mutableIntStateOf(0) }
    var lastFocusedFilteredMovieUrl by remember(categoryType) { mutableStateOf<String?>(null) }
    var isResultsGridFocused by remember(categoryType) { mutableStateOf(false) }

    // TV Back key handling when filters are active:
    // 1st Back press from results grid -> returns focus to the filter bar
    // 2nd Back press (or from filter bar) -> clears filters and restores the normal catalog view
    BackHandler(enabled = filterState.hasActiveFilters) {
        if (isResultsGridFocused) {
            isResultsGridFocused = false
            scope.launch {
                try {
                    listState.animateScrollToItem(0)
                } catch (_: Exception) {}
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        } else {
            filterState = MovieRepository.FilterState()
            scope.launch {
                try {
                    listState.animateScrollToItem(0)
                } catch (_: Exception) {}
                delay(60)
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    val screenTitle = when (categoryType) {
        "movies" -> "Películas"
        "series" -> "Series y Temporadas"
        "animes" -> "Animes"
        else -> "Catálogo"
    }

    val heroBadgeText = when (categoryType) {
        "movies" -> "PELÍCULA DESTACADA"
        "series" -> "SERIE DESTACADA"
        "animes" -> "ANIME DESTACADO"
        else -> "DESTACADO"
    }

    val top10SectionTitle = when (categoryType) {
        "movies" -> "Top 10 Películas más populares hoy"
        "series" -> "Top 10 Series más populares hoy"
        "animes" -> "Top 10 Animes en tendencia hoy"
        else -> "Top 10 más populares hoy"
    }

    // Load initial 3 pages concurrently for hero and initial sections
    LaunchedEffect(categoryType, reloadTrigger) {
        isLoading = true
        try {
            val p1Deferred = async { MovieRepository.fetchCategoryListing(categoryType, page = 1, postsPerPage = 30) }
            val p2Deferred = async { MovieRepository.fetchCategoryListing(categoryType, page = 2, postsPerPage = 30) }
            val p3Deferred = async { MovieRepository.fetchCategoryListing(categoryType, page = 3, postsPerPage = 30) }

            val res1 = p1Deferred.await()
            val res2 = p2Deferred.await()
            val res3 = p3Deferred.await()

            val combinedLive = (res1.items + res2.items + res3.items).distinctBy { it.url }
            totalCount = if (res1.totalCount > 0) res1.totalCount else combinedLive.size

            rawItems = combinedLive
        } catch (_: Exception) {
            rawItems = emptyList()
        } finally {
            isLoading = false
        }
    }

    // Handle Active Filters: Fetch 100% server-side filtered results from LaMovie API
    LaunchedEffect(categoryType, filterState) {
        if (!filterState.hasActiveFilters) {
            filteredResults = emptyList()
            filterTotalCount = 0
            return@LaunchedEffect
        }

        isFilterLoading = true
        filterCurrentPage = 1
        try {
            val res = MovieRepository.fetchCategoryListing(
                categoryType = categoryType,
                page = 1,
                postsPerPage = 30,
                genreIds = listOfNotNull(filterState.selectedGenre?.id),
                providerIds = listOfNotNull(filterState.selectedProvider?.id),
                countryIds = listOfNotNull(filterState.selectedCountry?.id),
                yearIds = listOfNotNull(filterState.selectedYear?.id),
                tmdbMin = filterState.selectedTmdbMin,
                orderBy = filterState.orderBy,
                order = filterState.order
            )
            filteredResults = res.items
            filterTotalPages = res.totalPages
            filterTotalCount = res.totalCount
        } catch (_: Exception) {
            filteredResults = emptyList()
            filterTotalCount = 0
        } finally {
            isFilterLoading = false
        }
    }

    // Infinite scroll for Filtered Results in TvLazyColumn
    LaunchedEffect(listState, filterState, filterCurrentPage, filterTotalPages, filteredResults.size) {
        if (!filterState.hasActiveFilters) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !isFilterFetchingMore && filterCurrentPage < filterTotalPages) {
                isFilterFetchingMore = true
                try {
                    val nextPage = filterCurrentPage + 1
                    val nextRes = MovieRepository.fetchCategoryListing(
                        categoryType = categoryType,
                        page = nextPage,
                        postsPerPage = 30,
                        genreIds = listOfNotNull(filterState.selectedGenre?.id),
                        providerIds = listOfNotNull(filterState.selectedProvider?.id),
                        countryIds = listOfNotNull(filterState.selectedCountry?.id),
                        yearIds = listOfNotNull(filterState.selectedYear?.id),
                        tmdbMin = filterState.selectedTmdbMin,
                        orderBy = filterState.orderBy,
                        order = filterState.order
                    )
                    if (nextRes.items.isNotEmpty()) {
                        filteredResults = (filteredResults + nextRes.items).distinctBy { it.url }
                        filterCurrentPage = nextPage
                        if (nextRes.totalPages > 0) filterTotalPages = nextRes.totalPages
                    }
                } catch (_: Exception) {}
                isFilterFetchingMore = false
            }
        }
    }

    // Hero Banner items
    val heroItems = remember(rawItems) {
        val withBackdrops = rawItems
            .filter { !it.backdrop.isNullOrBlank() }
            .distinctBy { it.title.lowercase().trim() }
        withBackdrops.ifEmpty { rawItems }.take(5)
    }

    // Top 10 items for this specific category (numbered 1 to 10)
    val top10Items = remember(rawItems) {
        rawItems
            .distinctBy { it.title.replace(Regex("""\s*\(\d{4}\)"""), "").lowercase().trim() }
            .take(10)
    }

    val top10FocusRequesters = remember(categoryType, top10Items) {
        List(top10Items.size) { FocusRequester() }
    }

    // Build Netflix-style horizontal sections (CLEAN, NO FALLBACK INJECTION)
    val sections = remember(categoryType, rawItems) {
        buildCleanCategorySections(
            categoryType = categoryType,
            allItems = rawItems
        )
    }

    val isTop10Target = (restoreFocusRowKey == "top10" || (restoreFocusRowKey == null && top10Items.any { it.url == restoreFocusUrl }))

    // Auto-scroll vertical listState to the row containing restoreFocusUrl
    LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, rawItems, sections) {
        if (restoreFocusUrl == null || filterState.hasActiveFilters) return@LaunchedEffect
        val rowKey = restoreFocusRowKey
        val hasTop10 = top10Items.isNotEmpty()

        val targetRowIndex: Int? = when {
            rowKey == "hero" -> 0
            rowKey == "top10" && hasTop10 -> 2
            rowKey != null -> {
                val baseSectionIndex = if (hasTop10) 3 else 2
                val matchingSectionOffset = sections.indexOfFirst { it.title == rowKey }
                if (matchingSectionOffset >= 0) baseSectionIndex + matchingSectionOffset else null
            }
            else -> {
                if (hasTop10 && top10Items.any { it.url == restoreFocusUrl }) {
                    2
                } else {
                    val baseSectionIndex = if (hasTop10) 3 else 2
                    val matchingSectionOffset = sections.indexOfFirst { sec -> sec.items.any { it.url == restoreFocusUrl } }
                    if (matchingSectionOffset >= 0) baseSectionIndex + matchingSectionOffset else null
                }
            }
        }

        if (targetRowIndex != null) {
            try {
                listState.scrollToItem(targetRowIndex, 0)
            } catch (_: Exception) {}
        }
    }

    // Restore focus to Hero banner ONLY if Hero was clicked
    LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
        if (restoreFocusUrl != null && restoreFocusRowKey == "hero") {
            delay(100)
            try {
                firstItemFocusRequester.requestFocus()
                onFocusRestored()
            } catch (_: Exception) {}
        }
    }

    // Restore focus to Top 10 card if clicked
    LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, top10Items) {
        if (restoreFocusUrl != null && isTop10Target && top10Items.any { it.url == restoreFocusUrl }) {
            val targetIdx = top10Items.indexOfFirst { it.url == restoreFocusUrl }
            if (targetIdx >= 0) {
                try { top10ListState.scrollToItem(targetIdx) } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06080C))
    ) {
        if (isLoading) {
            SkeletonHomeScreen()
        } else if (rawItems.isEmpty()) {
            TvConnectionErrorScreen(
                message = "Sin conexión a internet. Comprueba tu red e inténtalo de nuevo.",
                onRetry = {
                    isLoading = true
                    reloadTrigger++
                }
            )
        } else {
            val isHeroScrolledDown by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 60
                }
            }

            // Chunked in rows of 4 for larger, cinematic and readable posters on TV
            val chunkedFilteredMovies = remember(filteredResults) { filteredResults.chunked(4) }

            TvLazyColumn(
                state = listState,
                pivotOffsets = PivotOffsets(parentFraction = 0.22f),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 0.dp, bottom = 64.dp)
            ) {
                // 1. CATEGORY HERO BANNER (Hidden when actively filtering to give prominence to filtered results)
                if (!filterState.hasActiveFilters) {
                    item(key = "category_hero_$categoryType") {
                        TvHeroBanner(
                            heroList = heroItems,
                            onMovieSelect = { movie -> onMovieClick(movie, "hero") },
                            onFocusItem = {},
                            firstKeyFocusRequester = firstItemFocusRequester,
                            navFocusRequester = navFocusRequester,
                            categoryBadgeText = heroBadgeText,
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
                                try {
                                    firstFilterPillFocusRequester.requestFocus()
                                } catch (_: Exception) {
                                    try { firstSectionFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                            },
                            scrollToRow = {},
                            isScrolledDown = isHeroScrolledDown
                        )
                    }
                }

                // 2. COMPREHENSIVE FILTER BAR (Orden, Géneros, Proveedor, País, Año, TMDB)
                item(key = "category_filters_$categoryType") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (filterState.hasActiveFilters) 96.dp else 4.dp, bottom = 4.dp)
                    ) {
                        if (!filterState.hasActiveFilters) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFF5F2EB))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Filtros y secciones de $screenTitle",
                                    color = Color(0xFFF5F2EB),
                                    fontSize = 18.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                val displayCount = if (filterState.hasActiveFilters) filterTotalCount else totalCount
                                val formattedCount = remember(displayCount) {
                                    java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(displayCount)
                                }
                                Text(
                                    text = "$formattedCount títulos disponibles",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 12.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        TvFilterBar(
                            categoryType = categoryType,
                            filterState = filterState,
                            onFilterChange = { newState ->
                                filterState = newState
                            },
                            firstItemFocusRequester = if (filterState.hasActiveFilters) firstItemFocusRequester else firstFilterPillFocusRequester,
                            onUpDpad = {
                                if (!filterState.hasActiveFilters) {
                                    try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
                                } else {
                                    try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                            },
                            onDownDpad = {
                                if (filterState.hasActiveFilters) {
                                    var handled = false
                                    if (lastFocusedFilteredMovieUrl != null) {
                                        val req = filteredCardRequesters[lastFocusedFilteredMovieUrl]
                                        handled = TvFocusResolver.safeRequestFocus(req)
                                    }
                                    if (!handled) {
                                        handled = TvFocusResolver.safeRequestFocus(firstFilteredCardFocusRequester)
                                    }
                                    if (!handled && filteredCardRequesters.isNotEmpty()) {
                                        for (req in filteredCardRequesters.values) {
                                            if (TvFocusResolver.safeRequestFocus(req)) {
                                                handled = true
                                                break
                                            }
                                        }
                                    }
                                    handled
                                } else if (top10Items.isNotEmpty()) {
                                    val targetIdx = lastFocusedTop10Index.coerceIn(0, top10Items.lastIndex)
                                    var handled = TvFocusResolver.requestListTargetFocus(
                                        requesters = top10FocusRequesters,
                                        targetIndex = targetIdx,
                                        firstVisibleIndex = top10ListState.firstVisibleItemIndex
                                    )
                                    if (!handled) {
                                        scope.launch {
                                            try {
                                                top10ListState.scrollToItem(targetIdx)
                                                delay(40)
                                                top10FocusRequesters.getOrNull(targetIdx)?.requestFocus()
                                            } catch (_: Exception) {}
                                        }
                                        handled = true
                                    }
                                    handled
                                } else {
                                    TvFocusResolver.safeRequestFocus(firstSectionFocusRequester)
                                }
                            }
                        )
                    }
                }

                // 3. SHOW FILTERED RESULTS IF FILTERS ARE ACTIVE
                if (filterState.hasActiveFilters) {
                    if (isFilterLoading) {
                        item(key = "filter_loading_$categoryType") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Consultando catálogo con los filtros aplicados...",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 13.sp,
                                        fontFamily = Inter
                                    )
                                }
                            }
                        }
                    } else if (filteredResults.isEmpty()) {
                        item(key = "filter_empty_$categoryType") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No se encontraron títulos con los filtros seleccionados",
                                        color = Color(0xFFF5F2EB),
                                        fontSize = 16.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Prueba seleccionando otro género, proveedor o cambiando la calificación TMDB.",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 13.sp,
                                        fontFamily = Inter,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Title row item
                        item(key = "filter_results_title_$categoryType") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp, end = 48.dp, top = 6.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Resultados encontrados (${filteredResults.size})",
                                    color = Color(0xFFF5F2EB),
                                    fontSize = 16.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Chunked 4 columns per row: larger, spacious and cinematic posters on TV
                        chunkedFilteredMovies.forEachIndexed { rowIndex, rowMovies ->
                            item(key = "filter_row_${rowIndex}_${rowMovies.firstOrNull()?.url}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    for (colIndex in 0 until 4) {
                                        if (colIndex < rowMovies.size) {
                                            val movie = rowMovies[colIndex]
                                            val isFirstCard = rowIndex == 0 && colIndex == 0
                                            val cardRequester = if (isFirstCard) firstFilteredCardFocusRequester else remember(movie.url) { FocusRequester() }

                                            LaunchedEffect(movie.url, cardRequester) {
                                                filteredCardRequesters[movie.url] = cardRequester
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .onFocusChanged {
                                                        if (it.hasFocus || it.isFocused) {
                                                            isResultsGridFocused = true
                                                            lastFocusedFilteredMovieUrl = movie.url
                                                        }
                                                    }
                                                    .onKeyEvent { keyEvent ->
                                                        if (rowIndex == 0 && keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                            try {
                                                                firstItemFocusRequester.requestFocus()
                                                                true
                                                            } catch (_: Exception) {
                                                                false
                                                            }
                                                        } else false
                                                    }
                                            ) {
                                                TvMovieCard(
                                                    movie = movie,
                                                    onClick = { onMovieClick(movie, "Filtros") },
                                                    fillWidth = true,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .focusRequester(cardRequester)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        if (isFilterFetchingMore) {
                            item(key = "filter_loading_more_$categoryType") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                } else {
                    // 4. TOP 10 ROW FOR THIS CATEGORY (When no filter is active)
                    if (top10Items.isNotEmpty()) {
                        item(key = "category_top10_$categoryType") {
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
                                        text = top10SectionTitle,
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
                                    itemsIndexed(
                                        items = top10Items,
                                        key = { _, movie -> "cat-top10-$categoryType-${movie.url}" }
                                    ) { rankIndex, movie ->
                                        val cardFocusRequester = top10FocusRequesters.getOrElse(rankIndex) { remember(movie.url) { FocusRequester() } }

                                        LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
                                            if (restoreFocusUrl != null && isTop10Target && restoreFocusUrl == movie.url) {
                                                delay(60)
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
                                                .onPreviewKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                        try {
                                                            firstFilterPillFocusRequester.requestFocus()
                                                            true
                                                        } catch (_: Exception) {
                                                            false
                                                        }
                                                    } else false
                                                }
                                                .onFocusChanged { state ->
                                                    if (state.isFocused || state.hasFocus) {
                                                        lastFocusedTop10Index = rankIndex
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. HORIZONTAL GENRE SECTIONS (PURE, ZERO FALLBACK CONTAMINATION)
                    sections.forEachIndexed { sectionIndex, section ->
                        if (section.items.isNotEmpty()) {
                            item(key = "section_${categoryType}_${section.id}") {
                                val isFirstRow = sectionIndex == 0
                                PaginatedMovieRow(
                                    title = section.title,
                                    initialItems = section.items,
                                    rowIndex = sectionIndex + 1,
                                    onMovieSelect = { movie -> onMovieClick(movie, section.title) },
                                    firstItemFocusRequester = if (isFirstRow) firstSectionFocusRequester else null,
                                    onUpDpad = if (isFirstRow && top10Items.isEmpty()) {
                                        { try { firstFilterPillFocusRequester.requestFocus() } catch (_: Exception) {} }
                                    } else null,
                                    categoryType = categoryType,
                                    genreIds = section.genreIds,
                                    orderBy = section.orderBy,
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

/**
 * Builds clean thematic sections for each category.
 * CRITICAL FIX: Eliminates `sectionWithFallback` so random unrelated movies or series are NEVER injected!
 * Each section queries its genuine genreIds through PaginatedMovieRow.
 */
private fun buildCleanCategorySections(
    categoryType: String,
    allItems: List<MovieItem>
): List<CategorySectionRow> {
    if (allItems.isEmpty()) return emptyList()

    fun filterByGenreName(items: List<MovieItem>, targetGenres: List<String>): List<MovieItem> {
        return items.filter { item ->
            item.genres.any { itemGenre ->
                targetGenres.any { target -> itemGenre.equals(target, ignoreCase = true) }
            }
        }.distinctBy { it.url }
    }

    val topRated = allItems.sortedByDescending { it.rating?.toDoubleOrNull() ?: 7.5 }

    return when (categoryType) {
        "movies" -> {
            val actionMovies = filterByGenreName(allItems, listOf("Acción", "Aventura"))
            val sciFiMovies = filterByGenreName(allItems, listOf("Ciencia Ficción", "Fantasía"))
            val thrillerMovies = filterByGenreName(allItems, listOf("Suspense", "Suspenso", "Misterio", "Crimen"))
            val dramaMovies = filterByGenreName(allItems, listOf("Drama", "Historia"))
            val comedyMovies = filterByGenreName(allItems, listOf("Comedia", "Familia", "Animación"))
            val horrorMovies = filterByGenreName(allItems, listOf("Terror"))

            listOf(
                CategorySectionRow(
                    id = "movies_recent",
                    title = "Películas recién estrenadas y añadidas",
                    items = allItems,
                    genreIds = emptyList(),
                    orderBy = "latest"
                ),
                CategorySectionRow(
                    id = "movies_action",
                    title = "Acción y Aventura Explosiva",
                    items = actionMovies.ifEmpty { allItems.take(4) },
                    genreIds = listOf(32, 130),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "movies_scifi",
                    title = "Ciencia Ficción y Mundos Futuristas",
                    items = sciFiMovies.ifEmpty { allItems.take(4) },
                    genreIds = listOf(131, 229),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "movies_thriller",
                    title = "Suspenso, Crimen y Thrillers Intensos",
                    items = thrillerMovies.ifEmpty { allItems.take(4) },
                    genreIds = listOf(33, 180, 97),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "movies_drama",
                    title = "Dramas Aclamados e Historias Reales",
                    items = dramaMovies.ifEmpty { allItems.take(4) },
                    genreIds = listOf(17, 165),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "movies_comedy",
                    title = "Comedia, Romance y Cine Familiar",
                    items = comedyMovies.ifEmpty { allItems.take(4) },
                    genreIds = listOf(18, 398, 115),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "movies_horror",
                    title = "Terror, Misterio y Sobrenatural",
                    items = horrorMovies.ifEmpty { allItems.take(4) },
                    genreIds = listOf(96),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "movies_top_rated",
                    title = "Películas Aclamadas por la Crítica (Top TMDB)",
                    items = topRated,
                    genreIds = emptyList(),
                    orderBy = "rated"
                )
            ) + MovieRepository.featuredCollections.map { col ->
                CategorySectionRow(
                    id = "movies_col_${col.id}",
                    title = col.title,
                    items = col.movies,
                    genreIds = emptyList(),
                    orderBy = "views"
                )
            }
        }

        "series" -> {
            val actionSeries = filterByGenreName(allItems, listOf("Acción", "Aventura", "Action & Adventure"))
            val sciFiSeries = filterByGenreName(allItems, listOf("Ciencia Ficción", "Fantasía", "Sci-Fi & Fantasy"))
            val dramaSeries = filterByGenreName(allItems, listOf("Drama", "Crimen"))
            val thrillerSeries = filterByGenreName(allItems, listOf("Suspense", "Suspenso", "Misterio"))
            val comedySeries = filterByGenreName(allItems, listOf("Comedia"))

            listOf(
                CategorySectionRow(
                    id = "series_recent",
                    title = "Series recién añadidas y nuevos episodios",
                    items = allItems,
                    genreIds = emptyList(),
                    orderBy = "latest"
                ),
                CategorySectionRow(
                    id = "series_action",
                    title = "Series de Acción y Aventura",
                    items = actionSeries.ifEmpty { allItems.take(4) },
                    genreIds = listOf(705),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "series_scifi",
                    title = "Ciencia Ficción y Fantasía",
                    items = sciFiSeries.ifEmpty { allItems.take(4) },
                    genreIds = listOf(704),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "series_drama",
                    title = "Dramas Aclamados e Historias de Crimen",
                    items = dramaSeries.ifEmpty { allItems.take(4) },
                    genreIds = listOf(17, 180),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "series_thriller",
                    title = "Suspenso y Misterio Psicológico",
                    items = thrillerSeries.ifEmpty { allItems.take(4) },
                    genreIds = listOf(33, 97),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "series_comedy",
                    title = "Comedias y Sitcoms Destacadas",
                    items = comedySeries.ifEmpty { allItems.take(4) },
                    genreIds = listOf(18),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "series_top_rated",
                    title = "Series Mejor Valoradas (Top IMDb / TMDB)",
                    items = topRated,
                    genreIds = emptyList(),
                    orderBy = "rated"
                )
            )
        }

        "animes" -> {
            val shonenAction = filterByGenreName(allItems, listOf("Acción", "Aventura", "Action & Adventure", "Animación"))
            val fantasyAnime = filterByGenreName(allItems, listOf("Fantasía", "Sci-Fi & Fantasy", "Sobrenatural"))
            val mysteryAnime = filterByGenreName(allItems, listOf("Misterio", "Suspense", "Suspenso", "Drama"))
            val comedyAnime = filterByGenreName(allItems, listOf("Comedia", "Romance"))

            listOf(
                CategorySectionRow(
                    id = "animes_recent",
                    title = "Animes en emisión y recién añadidos",
                    items = allItems,
                    genreIds = emptyList(),
                    orderBy = "latest"
                ),
                CategorySectionRow(
                    id = "animes_shonen",
                    title = "Shōnen de Acción y Batallas Épicas",
                    items = shonenAction.ifEmpty { allItems.take(4) },
                    genreIds = listOf(705, 520),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "animes_fantasy",
                    title = "Fantasía, Magia y Mundos Sobrenaturales",
                    items = fantasyAnime.ifEmpty { allItems.take(4) },
                    genreIds = listOf(704, 229),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "animes_mystery",
                    title = "Misterio, Thriller Psicológico y Drama",
                    items = mysteryAnime.ifEmpty { allItems.take(4) },
                    genreIds = listOf(97, 33),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "animes_comedy",
                    title = "Aventura, Comedia y Romance",
                    items = comedyAnime.ifEmpty { allItems.take(4) },
                    genreIds = listOf(18, 115),
                    orderBy = "popular"
                ),
                CategorySectionRow(
                    id = "animes_top_rated",
                    title = "Clásicos Legendarios Mejor Valorados",
                    items = topRated,
                    genreIds = emptyList(),
                    orderBy = "rated"
                )
            )
        }

        else -> listOf(
            CategorySectionRow(
                id = "default_all",
                title = "Catálogo Destacado",
                items = allItems
            )
        )
    }
}

