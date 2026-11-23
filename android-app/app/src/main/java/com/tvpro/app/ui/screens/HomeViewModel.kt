package com.tvpro.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val featured: List<MovieItem>,
        val recentMovies: List<MovieItem>,
        val top10: List<MovieItem>,
        val recentSeries: List<MovieItem>,
        val recentAnimes: List<MovieItem>,
        val dramas: List<MovieItem>,
        val collections: List<MovieRepository.MovieCollectionSection>,
        val allMovies: List<MovieItem>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // Load primary catalog, live series, and live animes concurrently
                val moviesDeferred = async { try { MovieRepository.getCatalog(1) } catch (_: Exception) { null } }
                val seriesDeferred = async { try { MovieRepository.fetchCategoryListing("series", page = 1, postsPerPage = 24) } catch (_: Exception) { null } }
                val animesDeferred = async { try { MovieRepository.fetchCategoryListing("animes", page = 1, postsPerPage = 24) } catch (_: Exception) { null } }

                val page1 = moviesDeferred.await()
                val liveSeriesResult = seriesDeferred.await()
                val liveAnimesResult = animesDeferred.await()

                val movies = page1?.movies ?: emptyList()
                val liveSeries = liveSeriesResult?.items ?: emptyList()
                val liveAnimes = liveAnimesResult?.items ?: emptyList()

                // If no network data could be fetched from any live endpoint, we are offline!
                if (movies.isEmpty() && liveSeries.isEmpty() && liveAnimes.isEmpty()) {
                    _uiState.value = HomeUiState.Error("Sin conexión a internet. Comprueba tu red e inténtalo de nuevo.")
                    return@launch
                }

                val allWithBackdrops = (movies + liveSeries)
                    .filter { !it.backdrop.isNullOrBlank() }
                    .distinctBy { it.title.lowercase().trim() }
                val featured = allWithBackdrops.take(6).ifEmpty { (movies + liveSeries).take(6) }

                val top10 = (featured + movies.take(5) + liveSeries.take(3) + liveAnimes.take(2))
                    .distinctBy { it.title.lowercase().trim() }
                    .take(10)

                _uiState.value = HomeUiState.Success(
                    featured = featured,
                    recentMovies = movies,
                    top10 = top10,
                    recentSeries = liveSeries,
                    recentAnimes = liveAnimes,
                    dramas = emptyList(),
                    collections = MovieRepository.featuredCollections,
                    allMovies = movies + liveSeries + liveAnimes
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Sin conexión a internet. Comprueba tu red e inténtalo de nuevo.")
            }
        }
    }
}

