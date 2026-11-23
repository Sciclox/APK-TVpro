package com.tvpro.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.WatchHistoryItem
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.mobile.ui.components.*
import com.tvpro.mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onMovieClick: (MovieItem) -> Unit,
    onPlayMovie: (MovieItem) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: MobileHomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedCategory by remember { mutableStateOf("Todo") }
    var selectedGenre by remember { mutableStateOf("Todos") }
    var selectedSort by remember { mutableStateOf("Más Recientes") }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val watchHistory = remember { mutableStateListOf<WatchHistoryItem>() }

    fun refreshHistory() {
        watchHistory.clear()
        watchHistory.addAll(WatchHistoryManager.getHistory(context))
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvProDarkBg)
    ) {
        when (val state = uiState) {
            is MobileHomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WarmIvory)
                }
            }

            is MobileHomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MutedSlate,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadCatalog() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmIvory,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is MobileHomeUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        coroutineScope.launch {
                            isRefreshing = true
                            viewModel.loadCatalog()
                            refreshHistory()
                            kotlinx.coroutines.delay(650)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                    // Top Bar (Category Filter Pills, Genre Dropdown, Sort Dropdown & Search)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 10.dp, bottom = 4.dp)
                        ) {
                            MobileFilterBar(
                                selectedCategory = selectedCategory,
                                onCategorySelected = { 
                                    selectedCategory = it
                                    selectedGenre = "Todos"
                                },
                                selectedGenre = selectedGenre,
                                onGenreSelected = { selectedGenre = it },
                                selectedSort = selectedSort,
                                onSortSelected = { selectedSort = it },
                                onSearchClick = onSearchClick
                            )
                        }
                    }

                    // Mode 1: Genre Filter Active (When user selects a specific genre)
                    if (selectedGenre != "Todos") {
                        val filteredItems = state.allCatalog
                            .filter { it.genres.any { g -> g.contains(selectedGenre, ignoreCase = true) } }
                            .applySort(selectedSort)

                        item {
                            // Active Filter Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "GÉNERO SELECCIONADO",
                                        color = MutedSlate,
                                        fontFamily = Inter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "$selectedGenre (${filteredItems.size} Títulos)",
                                        color = WarmIvory,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        letterSpacing = (-0.2).sp
                                    )
                                }
                                IconButton(
                                    onClick = { selectedGenre = "Todos" },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF19202E))
                                        .border(1.dp, SubtleBorder, CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar filtro",
                                        tint = SoftSilver,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (filteredItems.isNotEmpty()) {
                            // Filtered Top Highlights
                            val filteredTop = filteredItems.filter { (it.rating?.toDoubleOrNull() ?: 0.0) >= 8.0 }
                            if (filteredTop.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Lo Mejor en $selectedGenre",
                                        items = filteredTop,
                                        badgeText = "★ DESTACADOS",
                                        subtitle = "Las producciones mejor valoradas del género",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Filtered Movies
                            val genreMovies = filteredItems.filter { it.type != "series" && it.type != "anime" && it.type != "drama" }
                            if (genreMovies.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Películas de $selectedGenre",
                                        items = genreMovies,
                                        badgeText = "PELÍCULAS",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Filtered Series & Anime
                            val genreSeries = filteredItems.filter { it.type == "series" || it.type == "anime" || it.type == "drama" }
                            if (genreSeries.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Series & Animes de $selectedGenre",
                                        items = genreSeries,
                                        badgeText = "SERIES",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp, horizontal = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No se encontraron títulos en $selectedGenre",
                                            color = SoftSilver,
                                            fontFamily = Inter,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { selectedGenre = "Todos" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = WarmIvory,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text("Ver todo el catálogo", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mode 2: Standard Curated Feed (Netflix / Max Style Layout)
                    else {
                        // 1. Hero Banner (Adaptive to category)
                        val heroList = when (selectedCategory) {
                            "Películas" -> state.featured.filter { it.type != "series" && it.type != "anime" }.ifEmpty { state.featured }
                            "Series" -> (state.featured.filter { it.type == "series" || it.type == "drama" } + state.trendingSeries).take(5)
                            "Animes" -> (state.featured.filter { it.type == "anime" } + state.trendingAnimes).take(5)
                            else -> state.featured
                        }

                        if (heroList.isNotEmpty()) {
                            item {
                                val currentHero = heroList.first()
                                var isInMyList by remember(currentHero.url) {
                                    mutableStateOf(WatchHistoryManager.isInMyList(context, currentHero.url))
                                }

                                MobileHeroBanner(
                                    heroList = heroList,
                                    isInMyList = isInMyList,
                                    onPlayClick = { movie -> onPlayMovie(movie) },
                                    onDetailClick = { movie -> onMovieClick(movie) },
                                    onToggleMyList = { movie ->
                                        isInMyList = WatchHistoryManager.toggleMyList(context, movie)
                                    }
                                )
                            }
                        }

                        // 2. Continuar Viendo Row (Filtered by category if selected)
                        val filteredHistory = when (selectedCategory) {
                            "Películas" -> watchHistory.filter { !it.isSeries }
                            "Series" -> watchHistory.filter { it.isSeries && !it.movieUrl.contains("/animes/") }
                            "Animes" -> watchHistory.filter { it.movieUrl.contains("/animes/") }
                            else -> watchHistory
                        }
                        if (filteredHistory.isNotEmpty()) {
                            item {
                                MobileContinueWatchingSection(
                                    history = filteredHistory,
                                    onPlayMovie = onPlayMovie
                                )
                            }
                        }

                        // === SECTIONS FOR: "TODO" (Comprehensive Master Feed) ===
                        if (selectedCategory == "Todo") {
                            // Section 1: Top 10 Hoy en TV Pro (Monumental Numbers Row)
                            if (state.top10.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 Hoy en TV Pro",
                                        items = state.top10.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "EN TENDENCIA",
                                        subtitle = "Lo más reproducido en las últimas 24 horas"
                                    )
                                }
                            }

                            // Section 2: Estrenos & Recién Añadidos
                            if (state.recentMovies.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Estrenos & Recién Añadidos",
                                        items = state.recentMovies.applySort(selectedSort),
                                        badgeText = "4K UHD",
                                        subtitle = "Nuevos lanzamientos cinematográficos",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 3: Aclamadas por la Crítica (Rating >= 8.5)
                            if (state.topRated.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Aclamadas por la Crítica",
                                        items = state.topRated.applySort(selectedSort),
                                        badgeText = "★ +8.5 TMDB",
                                        subtitle = "Obras maestras con las mejores calificaciones del mundo",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 4: Series que Marcan Tendencia
                            if (state.trendingSeries.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Series que Marcan Tendencia",
                                        items = state.trendingSeries.applySort(selectedSort),
                                        badgeText = "MARATÓN",
                                        subtitle = "Las historias televisivas más comentadas",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 5: Franchise Universe - Universo Cinematográfico de Marvel (UCM)
                            val mcuCollection = state.collections.find { it.id == "mcu" }
                            if (mcuCollection != null && mcuCollection.movies.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Universo Cinematográfico de Marvel",
                                        items = mcuCollection.movies.applySort(selectedSort),
                                        badgeText = "SAGA COMPLETA",
                                        subtitle = "Avengers, Spider-Man y el multiverso de héroes",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 6: Mundo Anime: Éxitos en Emisión
                            if (state.trendingAnimes.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Mundo Anime: Éxitos en Emisión",
                                        items = state.trendingAnimes.applySort(selectedSort),
                                        badgeText = "LAT & JAP",
                                        subtitle = "Animación japonesa de máximo nivel y combates épicos",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 7: Adrenalina Pura: Acción & Aventura
                            if (state.actionMovies.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Adrenalina Pura: Acción & Aventura",
                                        items = state.actionMovies.applySort(selectedSort),
                                        badgeText = "ALTO IMPACTO",
                                        subtitle = "Explosiones, artes marciales y velocidad sin pausa",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 8: Franchise Universe - Harry Potter & Tierra Media
                            val fantasyCollection = state.collections.filter { it.id == "harry_potter" || it.id == "lotr" }
                            fantasyCollection.forEach { col ->
                                if (col.movies.isNotEmpty()) {
                                    item {
                                        MobileRowSection(
                                            title = col.title,
                                            items = col.movies.applySort(selectedSort),
                                            badgeText = "COLECCIÓN ÉPICA",
                                            subtitle = "La magia y las leyendas más queridas del cine",
                                            onMovieClick = onMovieClick
                                        )
                                    }
                                }
                            }

                            // Section 9: La Hora del Drama & Telenovelas
                            if (state.dramas.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "La Hora del Drama & Telenovelas",
                                        items = state.dramas.applySort(selectedSort),
                                        badgeText = "PASIÓN & DRAMA",
                                        subtitle = "Romances apasionantes y tramas cautivadoras",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 10: Mundos Épicos: Ciencia Ficción & Futuro
                            if (state.sciFiEpics.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Mundos Épicos: Ciencia Ficción & Futuro",
                                        items = state.sciFiEpics.applySort(selectedSort),
                                        badgeText = "SCI-FI",
                                        subtitle = "Odiseas espaciales, IA y dimensiones paralelas",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 11: Franchise Universe - Cine de Autor: Christopher Nolan
                            val nolanCollection = state.collections.find { it.id == "nolan" }
                            if (nolanCollection != null && nolanCollection.movies.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Cine de Autor: Christopher Nolan",
                                        items = nolanCollection.movies.applySort(selectedSort),
                                        badgeText = "OBRAS MAESTRAS",
                                        subtitle = "Oppenheimer, Interstellar, Batman y cine de culto",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 12: Misterio, Terror & Suspenso
                            if (state.horrorThrillers.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Misterio, Terror & Suspenso",
                                        items = state.horrorThrillers.applySort(selectedSort),
                                        badgeText = "ALTA TENSIÓN",
                                        subtitle = "Pesadillas psicológicas y enigmas perturbadores",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 13: Risas & Comedia Familiar
                            if (state.comedies.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Risas & Comedia Familiar",
                                        items = state.comedies.applySort(selectedSort),
                                        badgeText = "PARA TODOS",
                                        subtitle = "Cine ligero, humor y diversión garantizada",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        }

                        // === SECTIONS FOR: "PELÍCULAS" ===
                        else if (selectedCategory == "Películas") {
                            val movieCatalog = state.allCatalog.filter { it.type != "series" && it.type != "anime" && it.type != "drama" }

                            // Top 10 Películas
                            val topMovies = movieCatalog.sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }.take(10)
                            if (topMovies.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 Películas Hoy",
                                        items = topMovies.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "TENDENCIA CINE",
                                        subtitle = "Las películas más vistas por los usuarios"
                                    )
                                }
                            }

                            // Recién Añadidas
                            if (state.recentMovies.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Películas recién añadidas",
                                        items = state.recentMovies.applySort(selectedSort),
                                        badgeText = "4K UHD",
                                        subtitle = "Últimos estrenos de cartelera",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Taquillazos & Aclamadas por la Crítica
                            val movieTopRated = movieCatalog.filter { (it.rating?.toDoubleOrNull() ?: 0.0) >= 8.3 }
                            if (movieTopRated.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Taquillazos Aclamados por la Crítica",
                                        items = movieTopRated.applySort(selectedSort),
                                        badgeText = "★ +8.3 TMDB",
                                        subtitle = "Grandes producciones galardonadas",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Acción & Aventura
                            val movieAction = movieCatalog.filter { it.genres.any { g -> g.contains("Acción", true) || g.contains("Aventura", true) } }
                            if (movieAction.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Películas de Acción & Adrenalina",
                                        items = movieAction.applySort(selectedSort),
                                        badgeText = "ALTO IMPACTO",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Ciencia Ficción en Cine
                            val movieSciFi = movieCatalog.filter { it.genres.any { g -> g.contains("Ciencia Ficción", true) || g.contains("Fantasía", true) } }
                            if (movieSciFi.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Ciencia Ficción & Universos Cinematográficos",
                                        items = movieSciFi.applySort(selectedSort),
                                        badgeText = "SCI-FI",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Terror & Suspenso en Pantalla Grande
                            val movieHorror = movieCatalog.filter { it.genres.any { g -> g.contains("Terror", true) || g.contains("Suspenso", true) } }
                            if (movieHorror.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Terror & Suspenso en Pantalla Grande",
                                        items = movieHorror.applySort(selectedSort),
                                        badgeText = "ALTA TENSIÓN",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Colecciones de Cine (UCM, Harry Potter, LOTR, Nolan)
                            state.collections.forEach { col ->
                                if (col.movies.isNotEmpty()) {
                                    item {
                                        MobileRowSection(
                                            title = col.title,
                                            items = col.movies.applySort(selectedSort),
                                            badgeText = "SAGA",
                                            onMovieClick = onMovieClick
                                        )
                                    }
                                }
                            }
                        }

                        // === SECTIONS FOR: "SERIES" ===
                        else if (selectedCategory == "Series") {
                            // Top 10 Series
                            val topSeries = state.trendingSeries.sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }.take(10)
                            if (topSeries.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 Series del Momento",
                                        items = topSeries.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "TENDENCIA TV",
                                        subtitle = "Las producciones que todos están viendo"
                                    )
                                }
                            }

                            // Series más populares
                            if (state.trendingSeries.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Series más populares",
                                        items = state.trendingSeries.applySort(selectedSort),
                                        badgeText = "MARATÓN",
                                        subtitle = "Temporadas completas disponibles",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Series Aclamadas por la Crítica
                            val seriesTopRated = state.trendingSeries.filter { (it.rating?.toDoubleOrNull() ?: 0.0) >= 8.5 }
                            if (seriesTopRated.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Series Aclamadas por la Crítica",
                                        items = seriesTopRated.applySort(selectedSort),
                                        badgeText = "★ +8.5 TMDB",
                                        subtitle = "The Last of Us, Shōgun, Breaking Bad y más",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // La Hora del Drama (Novelas)
                            if (state.dramas.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "La Hora del Drama (Novelas)",
                                        items = state.dramas.applySort(selectedSort),
                                        badgeText = "PASIÓN & DRAMA",
                                        subtitle = "Dramas románticos y producciones intensas",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Series de Suspenso, Crimen & Misterio
                            val seriesMystery = state.trendingSeries.filter { it.genres.any { g -> g.contains("Suspenso", true) || g.contains("Crimen", true) || g.contains("Misterio", true) } }
                            if (seriesMystery.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Series de Suspenso & Crimen",
                                        items = seriesMystery.applySort(selectedSort),
                                        badgeText = "INTRIGA",
                                        subtitle = "Peaky Blinders, Dark, Severance...",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Series de Fantasía & Ciencia Ficción
                            val seriesSciFi = state.trendingSeries.filter { it.genres.any { g -> g.contains("Fantasía", true) || g.contains("Ciencia Ficción", true) } }
                            if (seriesSciFi.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Series de Fantasía & Ciencia Ficción",
                                        items = seriesSciFi.applySort(selectedSort),
                                        badgeText = "FANTASÍA",
                                        subtitle = "House of the Dragon, Stranger Things, Fallout...",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        }

                        // === SECTIONS FOR: "ANIMES" ===
                        else if (selectedCategory == "Animes") {
                            // Top 10 Animes
                            val topAnimes = state.trendingAnimes.sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }.take(10)
                            if (topAnimes.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 Animes Más Vistos",
                                        items = topAnimes.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "TOP ANIME",
                                        subtitle = "Las series anime con mayor audiencia"
                                    )
                                }
                            }

                            // Animes en Emisión
                            if (state.trendingAnimes.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Animes en Emisión",
                                        items = state.trendingAnimes.applySort(selectedSort),
                                        badgeText = "LAT & JAP",
                                        subtitle = "Nuevos episodios semanales y simulcast",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Shonen, Combates & Adrenalina
                            val shonenAnimes = state.trendingAnimes.filter { it.genres.any { g -> g.contains("Acción", true) || g.contains("Aventura", true) } }
                            if (shonenAnimes.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Shonen, Combates & Adrenalina",
                                        items = shonenAnimes.applySort(selectedSort),
                                        badgeText = "SHONEN",
                                        subtitle = "Solo Leveling, Kimetsu no Yaiba, Jujutsu Kaisen...",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Fantasía Oscura & Sobrenatural
                            val darkFantasyAnimes = state.trendingAnimes.filter { it.genres.any { g -> g.contains("Fantasía", true) || g.contains("Misterio", true) || g.contains("Sobrenatural", true) } }
                            if (darkFantasyAnimes.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Fantasía Oscura & Sobrenatural",
                                        items = darkFantasyAnimes.applySort(selectedSort),
                                        badgeText = "SOBRENATURAL",
                                        subtitle = "Death Note, Chainsaw Man, Attack on Titan...",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Clásicos Legendarios
                            val classicAnimes = state.trendingAnimes.filter { it.title.contains("Dragon Ball", true) || it.title.contains("Naruto", true) || it.title.contains("One Piece", true) || it.title.contains("Bleach", true) }
                            if (classicAnimes.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Clásicos & Leyendas del Anime",
                                        items = classicAnimes.applySort(selectedSort),
                                        badgeText = "LEGENDARIOS",
                                        subtitle = "Las historias que definieron generaciones",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        }

                        // Bottom padding for bottom navigation bar
                        item {
                            Spacer(modifier = Modifier.height(36.dp))
                        }
                    }
                }
            }
        }
    }
}
}

private fun List<MovieItem>.applySort(sortOption: String): List<MovieItem> {
    return when (sortOption) {
        "Mayor Puntuación TMDB" -> sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }
        "Año Descendente" -> sortedByDescending { it.year?.filter { c -> c.isDigit() }?.toIntOrNull() ?: 0 }
        "Título A-Z" -> sortedBy { it.title.lowercase() }
        else -> this // "Más Recientes" - preserve curated placement
    }
}

@Composable
fun MobileRowSection(
    title: String,
    items: List<MovieItem>,
    onMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        MobileSectionHeader(
            title = title,
            badgeText = badgeText,
            subtitle = subtitle,
            actionText = actionText,
            onActionClick = onActionClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = items,
                key = { it.url.ifBlank { it.title } }
            ) { movie ->
                MobileMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun MobileTop10Section(
    title: String,
    items: List<MovieItem>,
    onMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = "EN TENDENCIA",
    subtitle: String? = "Lo más reproducido hoy en TV Pro"
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        MobileSectionHeader(
            title = title,
            badgeText = badgeText,
            subtitle = subtitle
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = items.take(10),
                key = { _, item -> item.url.ifBlank { item.title } }
            ) { index, movie ->
                MobileTop10Card(
                    movie = movie,
                    rank = index + 1,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun MobileContinueWatchingSection(
    history: List<WatchHistoryItem>,
    onPlayMovie: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        MobileSectionHeader(
            title = "Continuar Viendo",
            badgeText = "${history.size}",
            subtitle = "Retoma donde lo dejaste"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = history,
                key = { it.movieUrl }
            ) { historyItem ->
                MobileContinueWatchingCard(
                    item = historyItem,
                    onClick = {
                        val movieItem = MovieItem(
                            title = historyItem.title,
                            url = historyItem.movieUrl,
                            image = historyItem.image,
                            rating = historyItem.rating,
                            postId = historyItem.postId,
                            slug = historyItem.slug,
                            type = if (historyItem.isSeries) "series" else "movie"
                        )
                        onPlayMovie(movieItem)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun MobileSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = title,
                    color = WarmIvory,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.5.sp,
                    letterSpacing = (-0.2).sp,
                    maxLines = 2,
                    lineHeight = 22.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF19202E))
                            .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = SoftSilver,
                            fontFamily = Inter,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionText,
                    color = SoftSilver,
                    fontFamily = Inter,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onActionClick() }
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = MutedSlate,
                fontFamily = Inter,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
fun MobileContinueWatchingCard(
    item: WatchHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (item.durationMs > 0) {
        (item.playbackPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val seriesInfo = remember(item) { extractSeriesInfo(item) }

    Column(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(8.dp))
                .background(TvProCardBg)
        ) {
            AsyncImage(
                model = item.image,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
            )

            // Play icon in center
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0x9903090F))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Continuar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Series Badge if applicable
            if (seriesInfo != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PillSurface)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = seriesInfo.formattedLabel,
                        color = WarmIvory,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter
                    )
                }
            }

            // Progress bar at bottom (Matching TV version: Warm Ivory on translucent track)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.20f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(WarmIvory)
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = item.title.cleanTitle(),
            color = WarmIvory,
            fontFamily = Inter,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
