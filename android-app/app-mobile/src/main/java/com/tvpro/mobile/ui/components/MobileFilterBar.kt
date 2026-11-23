package com.tvpro.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.mobile.ui.theme.*

val GENRES_LIST = listOf(
    "Todos", "Acción", "Ciencia Ficción", "Drama", "Suspenso",
    "Terror", "Comedia", "Animación", "Aventura", "Fantasía",
    "Crimen", "Romance", "Misterio", "Bélica", "Documental"
)

val SORT_OPTIONS = listOf(
    "Más Recientes", "Mayor Puntuación TMDB", "Año Descendente", "Título A-Z"
)

val YEARS_LIST = listOf(
    "Todos los años", "2025", "2024", "2023", "2022", "2021", "2020", "2010s", "Clásicos"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileFilterBar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedGenre: String,
    onGenreSelected: (String) -> Unit,
    selectedSort: String,
    onSortSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null
) {
    var activeSheet by remember { mutableStateOf<String?>(null) } // "genre" or "sort"

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main categories row (Todo, Películas, Series, Animes)
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = if (onSearchClick != null) 8.dp else 16.dp, top = 0.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val categories = listOf("Todo", "Películas", "Series", "Animes")
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) WarmIvory else PillSurface)
                        .border(
                            1.dp,
                            if (isSelected) WarmIvory else SubtleBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onCategorySelected(cat) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.Black else SoftSilver,
                        fontSize = 12.5.sp,
                        fontFamily = Inter,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Separator dot
            item {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }

            // Genre Dropdown Pill
            item {
                val hasCustomGenre = selectedGenre != "Todos"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (hasCustomGenre) Color(0xFF1E2433) else PillSurface)
                        .border(
                            1.dp,
                            if (hasCustomGenre) WarmIvory.copy(alpha = 0.75f) else SubtleBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { activeSheet = "genre" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (hasCustomGenre) selectedGenre else "Géneros",
                        color = if (hasCustomGenre) WarmIvory else SoftSilver,
                        fontSize = 12.sp,
                        fontFamily = Inter,
                        fontWeight = if (hasCustomGenre) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = if (hasCustomGenre) WarmIvory else MutedSlate,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Sort Dropdown Pill
            item {
                val hasCustomSort = selectedSort != "Más Recientes"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (hasCustomSort) Color(0xFF1E2433) else PillSurface)
                        .border(
                            1.dp,
                            if (hasCustomSort) WarmIvory.copy(alpha = 0.75f) else SubtleBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { activeSheet = "sort" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedSort,
                        color = if (hasCustomSort) WarmIvory else SoftSilver,
                        fontSize = 12.sp,
                        fontFamily = Inter,
                        fontWeight = if (hasCustomSort) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = if (hasCustomSort) WarmIvory else MutedSlate,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (onSearchClick != null) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clip(CircleShape)
                    .background(PillSurface)
                    .border(1.dp, SubtleBorder, CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = WarmIvory,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Modal Bottom Sheet for Filters
    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            containerColor = Color(0xFF141923),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeSheet == "genre") "Seleccionar Género" else "Ordenar Por",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = WarmIvory
                    )
                    IconButton(onClick = { activeSheet = null }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MutedSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeSheet == "genre") {
                    GENRES_LIST.forEach { genre ->
                        val isSelected = genre == selectedGenre
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable {
                                    onGenreSelected(genre)
                                    activeSheet = null
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = genre,
                                color = if (isSelected) WarmIvory else SoftSilver,
                                fontSize = 15.sp,
                                fontFamily = Inter,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = WarmIvory,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else if (activeSheet == "sort") {
                    SORT_OPTIONS.forEach { sort ->
                        val isSelected = sort == selectedSort
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable {
                                    onSortSelected(sort)
                                    activeSheet = null
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sort,
                                color = if (isSelected) WarmIvory else SoftSilver,
                                fontSize = 15.sp,
                                fontFamily = Inter,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = WarmIvory,
                                    modifier = Modifier.size(20.dp)
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
