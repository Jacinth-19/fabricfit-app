package com.fabricfit.app.ui.screens.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.engine.RecommendationEngine
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.GenderCategory
import com.fabricfit.app.domain.model.Measurements
import com.fabricfit.app.domain.model.Occasion
import com.fabricfit.app.domain.model.Recommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RecsUiState(
    val fabric: Fabric? = null,
    val looks: List<com.fabricfit.app.domain.model.InspirationLook> = emptyList(),
    val filtered: List<Recommendation> = emptyList(),
    val gender: GenderCategory? = null,
    val occasions: Set<Occasion> = emptySet(),
    val favoriteStyles: Set<String> = emptySet(),
    val loaded: Boolean = false,
)

class RecommendationsViewModel(
    private val container: AppContainer,
    private val fabricId: Long,
) : ViewModel() {

    private val fabricFlow = MutableStateFlow<Fabric?>(null)
    private val genderFlow = MutableStateFlow<GenderCategory?>(null)
    private val occasionsFlow = MutableStateFlow<Set<Occasion>>(emptySet())
    private val favoritesFlow = MutableStateFlow<Set<String>>(emptySet())
    private val dismissedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val measurementsFlow = MutableStateFlow(Measurements())
    private val allLooksFlow = MutableStateFlow<List<com.fabricfit.app.domain.model.InspirationLook>>(emptyList())

    private val _state = MutableStateFlow(RecsUiState())
    val state: StateFlow<RecsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            fabricFlow.value = container.fabricAnalysis.get(fabricId)
        }
        viewModelScope.launch {
            allLooksFlow.value = try {
                container.catalog.fetchInspirationLooks()
            } catch (_: Exception) {
                emptyList()
            }
        }
        viewModelScope.launch {
            container.favorites.observeFavoriteDesigns().collect { favs ->
                favoritesFlow.value = favs.map { it.id }.toSet()
            }
        }
        viewModelScope.launch {
            container.feedback.observeDismissed().collect { dismissed ->
                dismissedFlow.value = dismissed
            }
        }
        viewModelScope.launch {
            val m = container.measurements.get()
            measurementsFlow.value = m
        }
        viewModelScope.launch {
            val allStyles = container.dressStyles.all()
            combine(
                fabricFlow,
                genderFlow,
                occasionsFlow,
                favoritesFlow,
                dismissedFlow,
                measurementsFlow,
                allLooksFlow,
            ) { values ->
                val fabric = values[0] as Fabric?
                val gender = values[1] as GenderCategory?
                @Suppress("UNCHECKED_CAST")
                val occasions = values[2] as Set<Occasion>
                @Suppress("UNCHECKED_CAST")
                val favs = values[3] as Set<String>
                @Suppress("UNCHECKED_CAST")
                val dismissed = values[4] as Set<String>
                val measurements = values[5] as Measurements
                @Suppress("UNCHECKED_CAST")
                val allLooks = values[6] as List<com.fabricfit.app.domain.model.InspirationLook>

                val recs = if (fabric != null) {
                    RecommendationEngine.recommend(
                        fabric = fabric,
                        styles = allStyles,
                        dismissed = dismissed,
                        context = RecommendationEngine.RecommendationContext(
                            measurements = measurements,
                            favoriteStyleIds = favs,
                            preferredGender = gender,
                        ),
                    )
                } else {
                    emptyList()
                }

                RecsUiState(
                    fabric = fabric,
                    looks = allLooks.filter { look ->
                        com.fabricfit.app.domain.model.InspirationLook.matches(look, fabric?.category, gender)
                    },
                    loaded = fabric != null,
                    gender = gender,
                    occasions = occasions,
                    favoriteStyles = favs,
                    filtered = recs.filter { rec ->
                        (gender == null || rec.style.gender == gender || rec.style.gender == GenderCategory.UNISEX) &&
                            (occasions.isEmpty() || rec.style.occasions.intersect(occasions).isNotEmpty())
                    },
                )
            }.collect { _state.value = it }
        }
    }

    fun setGender(gender: GenderCategory) {
        genderFlow.value = if (genderFlow.value == gender) null else gender
    }

    fun toggleOccasion(occ: Occasion) {
        val current = occasionsFlow.value
        occasionsFlow.value = if (occ in current) current - occ else current + occ
    }

    fun clearFilters() {
        genderFlow.value = null
        occasionsFlow.value = emptySet()
    }

    fun toggleFavorite(style: DressStyle) {
        viewModelScope.launch {
            val now = !favoritesFlow.value.contains(style.id)
            container.favorites.setDesignFavorite(style.id, now)
        }
    }

    fun dismiss(style: DressStyle) {
        viewModelScope.launch { container.feedback.dismiss(style.id) }
    }

    fun undoDismiss(style: DressStyle) {
        viewModelScope.launch { container.feedback.undo(style.id) }
    }
}
