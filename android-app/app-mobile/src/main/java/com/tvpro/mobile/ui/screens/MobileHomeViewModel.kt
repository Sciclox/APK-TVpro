package com.tvpro.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MobileHomeUiState {
    data object Loading : MobileHomeUiState
    data class Success(
        val featured: List<MovieItem>,
        val top10: List<MovieItem>,
        val recentMovies: List<MovieItem>,
        val topRated: List<MovieItem>,
        val trendingSeries: List<MovieItem>,
        val trendingAnimes: List<MovieItem>,
        val actionMovies: List<MovieItem>,
        val sciFiEpics: List<MovieItem>,
        val horrorThrillers: List<MovieItem>,
        val comedies: List<MovieItem>,
        val dramas: List<MovieItem>,
        val collections: List<MovieRepository.MovieCollectionSection>,
        val allCatalog: List<MovieItem>
    ) : MobileHomeUiState
    data class Error(val message: String) : MobileHomeUiState
}

class MobileHomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MobileHomeUiState>(MobileHomeUiState.Loading)
    val uiState: StateFlow<MobileHomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    private fun List<MovieItem>.distinctByUrl(): List<MovieItem> {
        val seen = HashSet<String>()
        return filter { movie ->
            val key = movie.url.ifBlank { movie.title }
            seen.add(key)
        }
    }

    private fun MovieItem.ratingValue(): Double {
        return rating?.toDoubleOrNull() ?: 0.0
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = MobileHomeUiState.Loading
            try {
                val moviesDeferred = async { try { MovieRepository.getCatalog(1) } catch (_: Exception) { null } }
                val seriesDeferred = async { try { MovieRepository.fetchCategoryListing("series", page = 1, postsPerPage = 30) } catch (_: Exception) { null } }
                val animesDeferred = async { try { MovieRepository.fetchCategoryListing("animes", page = 1, postsPerPage = 30) } catch (_: Exception) { null } }

                val page1 = moviesDeferred.await()
                val liveSeriesResult = seriesDeferred.await()
                val liveAnimesResult = animesDeferred.await()

                val liveMovies = page1?.movies ?: emptyList()
                val liveSeries = liveSeriesResult?.items ?: emptyList()
                val liveAnimes = liveAnimesResult?.items ?: emptyList()

                val allMovies = (liveMovies.ifEmpty { MovieRepository.localCatalog } + MovieRepository.localCatalog).distinctByUrl()
                val allSeries = (liveSeries.ifEmpty { MovieRepository.localSeriesCatalog } + MovieRepository.localSeriesCatalog).distinctByUrl()
                val allAnimes = (liveAnimes.ifEmpty { MovieRepository.localAnimeCatalog } + MovieRepository.localAnimeCatalog).distinctByUrl()
                val allDramas = (MovieRepository.localDramaCatalog + allSeries.filter { it.genres.any { g -> g.contains("Drama", true) || g.contains("Romance", true) } }).distinctByUrl()

                val masterCatalog = (allMovies + allSeries + allAnimes + allDramas).distinctByUrl()

                val featuredList = page1?.featured?.ifEmpty {
                    MovieRepository.localCatalog.filter { it.type == "featured" }
                } ?: MovieRepository.localCatalog.take(5)

                // Top 10: Top rated & most prominent across movies & series
                val top10List = masterCatalog
                    .sortedByDescending { it.ratingValue() }
                    .take(10)

                // Aclamadas por la Crítica (rating >= 8.5)
                val topRatedList = masterCatalog
                    .filter { it.ratingValue() >= 8.4 }
                    .sortedByDescending { it.ratingValue() }
                    .distinctByUrl()

                // Acción & Aventura
                val actionList = masterCatalog
                    .filter { it.genres.any { g -> g.contains("Acción", true) || g.contains("Aventura", true) } }
                    .distinctByUrl()

                // Ciencia Ficción & Mundos Épicos
                val sciFiList = masterCatalog
                    .filter { it.genres.any { g -> g.contains("Ciencia Ficción", true) || g.contains("Fantasía", true) } }
                    .distinctByUrl()

                // Terror & Suspenso
                val horrorList = masterCatalog
                    .filter { it.genres.any { g -> g.contains("Terror", true) || g.contains("Suspenso", true) || g.contains("Misterio", true) || g.contains("Crimen", true) } }
                    .distinctByUrl()

                // Comedia & Familiar
                val comedyList = masterCatalog
                    .filter { it.genres.any { g -> g.contains("Comedia", true) || g.contains("Familia", true) || g.contains("Animación", true) } }
                    .distinctByUrl()

                val curatedCollections = MovieRepository.featuredCollections

                _uiState.value = MobileHomeUiState.Success(
                    featured = featuredList,
                    top10 = top10List,
                    recentMovies = allMovies,
                    topRated = topRatedList,
                    trendingSeries = allSeries,
                    trendingAnimes = allAnimes,
                    actionMovies = actionList,
                    sciFiEpics = sciFiList,
                    horrorThrillers = horrorList,
                    comedies = comedyList,
                    dramas = allDramas,
                    collections = curatedCollections,
                    allCatalog = masterCatalog
                )
            } catch (e: Exception) {
                // Fallback to local offline catalog
                val fallbackMovies = MovieRepository.localCatalog
                val fallbackSeries = MovieRepository.localSeriesCatalog
                val fallbackAnimes = MovieRepository.localAnimeCatalog
                val fallbackDramas = MovieRepository.localDramaCatalog
                val fallbackCombined = (fallbackMovies + fallbackSeries + fallbackAnimes + fallbackDramas).distinctByUrl()

                _uiState.value = MobileHomeUiState.Success(
                    featured = fallbackMovies.take(5),
                    top10 = fallbackCombined.sortedByDescending { it.ratingValue() }.take(10),
                    recentMovies = fallbackMovies,
                    topRated = fallbackCombined.filter { it.ratingValue() >= 8.4 }.sortedByDescending { it.ratingValue() },
                    trendingSeries = fallbackSeries,
                    trendingAnimes = fallbackAnimes,
                    actionMovies = fallbackCombined.filter { it.genres.any { g -> g.contains("Acción", true) || g.contains("Aventura", true) } },
                    sciFiEpics = fallbackCombined.filter { it.genres.any { g -> g.contains("Ciencia Ficción", true) || g.contains("Fantasía", true) } },
                    horrorThrillers = fallbackCombined.filter { it.genres.any { g -> g.contains("Terror", true) || g.contains("Suspenso", true) || g.contains("Misterio", true) || g.contains("Crimen", true) } },
                    comedies = fallbackCombined.filter { it.genres.any { g -> g.contains("Comedia", true) || g.contains("Familia", true) } },
                    dramas = fallbackDramas,
                    collections = MovieRepository.featuredCollections,
                    allCatalog = fallbackCombined
                )
            }
        }
    }
}
