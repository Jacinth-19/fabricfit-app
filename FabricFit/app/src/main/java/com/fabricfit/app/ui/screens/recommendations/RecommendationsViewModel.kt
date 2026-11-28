package com.fabricfit.app.ui.screens.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.data.remote.CloudRecommendationClient
import com.fabricfit.app.domain.engine.RecommendationEngine
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.GenderCategory
import com.fabricfit.app.domain.model.Measurements
import com.fabricfit.app.domain.model.Occasion
import com.fabricfit.app.domain.model.Recommendation
import com.fabricfit.app.domain.model.WhyReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class RecsUiState(
    val fabric: Fabric? = null,
    val looks: List<com.fabricfit.app.domain.model.InspirationLook> = emptyList(),
    val filtered: List<Recommendation> = emptyList(),
    val gender: GenderCategory? = null,
    val occasions: Set<Occasion> = emptySet(),
    val favoriteStyles: Set<String> = emptySet(),
    val loaded: Boolean = false,
    /** Which engine actually produced [filtered]; shown in the UI, not guessed at. */
    val ranker: RankerSource = RankerSource.ON_DEVICE,
    /** True while a configured cloud ranker is in flight and on-device results are shown. */
    val cloudPending: Boolean = false,
)

enum class RankerSource { ON_DEVICE, CLOUD, CLOUD_FAILED }

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

    /** Set once a cloud reply arrives; merged into the ranking on the next rebuild. */
    private var cloudScores: Map<String, CloudRecommendationClient.ScoredStyle> = emptyMap()
    private val cloudFlow = MutableStateFlow<CloudRanking>(CloudRanking.Idle)

    /** Lifecycle of the optional remote ranking pass. */
    private sealed interface CloudRanking {
        data object Idle : CloudRanking
        data object Pending : CloudRanking
        data class Loaded(val response: CloudRecommendationClient.RecommendationResponse) : CloudRanking
        data object Failed : CloudRanking
    }

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
                cloudFlow,
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
                val cloud = values[7] as CloudRanking

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

                // A cloud reply re-ranks the same candidate set rather than replacing it:
                // every style the backend did not mention keeps its on-device score, so a
                // partial or empty reply degrades to the local ranking instead of a blank
                // screen.
                val merged = when (cloud) {
                    is CloudRanking.Loaded -> applyCloudRanking(recs, cloud.response)
                    else -> recs
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
                    ranker = when (cloud) {
                        is CloudRanking.Loaded -> RankerSource.CLOUD
                        CloudRanking.Failed -> RankerSource.CLOUD_FAILED
                        else -> RankerSource.ON_DEVICE
                    },
                    cloudPending = cloud is CloudRanking.Pending,
                    filtered = merged.filter { rec ->
                        (gender == null || rec.style.gender == gender || rec.style.gender == GenderCategory.UNISEX) &&
                            (occasions.isEmpty() || rec.style.occasions.intersect(occasions).isNotEmpty())
                    },
                )
            }.collect { _state.value = it }
        }
    }

    /**
     * Starts the optional cloud ranking pass. Safe to call repeatedly; a blank base URL is a
     * no-op and leaves the on-device ranking untouched.
     */
    fun requestCloudRanking() {
        val fabric = _state.value.fabric ?: return
        viewModelScope.launch {
            // current() is suspend, so the preference read belongs inside the coroutine -
            // reading it before the launch would silently skip the cloud pass.
            val baseUrl = runCatching { container.settings.current().cloudBaseUrl }
                .getOrDefault("")
            if (baseUrl.isBlank()) return@launch

            cloudFlow.value = CloudRanking.Pending
            val candidates = container.dressStyles.all()
            val reply = container.cloudRecommendations.recommend(
                baseUrl = baseUrl,
                fabric = fabric,
                candidates = candidates,
                gender = _state.value.gender?.name,
                occasions = _state.value.occasions,
                favorites = _state.value.favoriteStyles,
            )
            cloudFlow.value = if (reply == null) CloudRanking.Failed else CloudRanking.Loaded(reply)
        }
    }

    /** Overlays backend scores onto the local ranking, keeping provenance visible. */
    private fun applyCloudRanking(
        local: List<Recommendation>,
        response: CloudRecommendationClient.RecommendationResponse,
    ): List<Recommendation> {
        val byId = response.scored.associateBy { it.styleId }
        return local.map { rec ->
            val remote = byId[rec.style.id] ?: return@map rec
            rec.copy(
                score = (remote.score * 100f).roundToInt().coerceIn(0, 98),
                reasons = rec.reasons + remote.reasons.map { WhyReason.ReasonFromCloud(it) },
            )
        }.sortedByDescending { it.score }
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
