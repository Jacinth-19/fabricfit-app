package com.fabricfit.app.data.remote

import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.FabricPlacement
import com.fabricfit.app.domain.model.LengthOption
import com.fabricfit.app.domain.model.NecklineOption
import com.fabricfit.app.domain.model.Occasion
import com.fabricfit.app.domain.model.SleeveOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Optional recommendations backend.
 *
 * The app's recommendation ranking runs on-device ([com.fabricfit.app.domain.engine.
 * RecommendationEngine]) and that is the default. This client is the other half: when an
 * operator sets `cloudBaseUrl` in Settings, a fabric analysis is sent to
 * `POST {base}/v1/recommend` and its ranked styles are used instead.
 *
 * Dormant by design — there is no bundled endpoint and no key. If no URL is configured,
 * or the call fails, or the reply is malformed, every method returns null and the caller
 * keeps the on-device result. It never throws into the UI.
 *
 * Expected request body (`application/json`):
 * ```json
 * {
 *   "fabric": {
 *     "id": 12,
 *     "name": "Kente Royale",
 *     "category": "KENTE",
 *     "pattern": "GEOMETRIC",
 *     "texture": "WOVEN",
 *     "colors": ["#C8952B", "#0F3D2E"],
 *     "confidence": 0.87
 *   },
 *   "candidates": [{"id": "kaba_asymmetric", "name": "...", "category": "DRESS"}],
 *   "preferences": {"gender": "FEMALE", "occasions": ["WEDDING"], "favorites": ["a","b"]},
 *   "limit": 24
 * }
 * ```
 *
 * Expected response body:
 * ```json
 * {
 *   "recommendations": [
 *     {"styleId": "kaba_asymmetric", "score": 0.93, "reasons": ["matches woven drape"],
 *      "customization": {"sleeve": "CAP","neckline":"OFF_SHOULDER","length":"MAXI","placement":"FULL"}
 *     }
 *   ],
 *   "modelVersion": "fabricfit-recs-1"
 * }
 * ```
 *
 * Both shapes are this client's contract, not an existing service's. `docs/RECS_API.md`
 * documents them and includes a reference implementation.
 */
class CloudRecommendationClient(
    private val client: OkHttpClient = defaultClient(),
) {

    /** A single ranked style from the backend. */
    data class ScoredStyle(
        val styleId: String,
        val score: Float,
        val reasons: List<String> = emptyList(),
        val customization: DesignCustomization? = null,
    )

    data class RecommendationResponse(
        val scored: List<ScoredStyle>,
        val modelVersion: String? = null,
    )

    /**
     * Asks the backend to rank [candidates] for [fabric].
     *
     * @return null when no backend is configured, the call fails, or the response cannot
     *   be understood. Callers must treat null as "keep the on-device ranking".
     */
    suspend fun recommend(
        baseUrl: String,
        fabric: Fabric,
        candidates: List<DressStyle>,
        gender: String?,
        occasions: Set<Occasion>,
        favorites: Set<String>,
        limit: Int = 24,
    ): RecommendationResponse? {
        if (baseUrl.isBlank() || candidates.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply {
                    put("fabric", fabricJson(fabric))
                    put("candidates", JSONArray().apply {
                        candidates.forEach { style ->
                            put(
                                JSONObject().apply {
                                    // titleKey/descriptionKey are i18n keys, not display
                                    // copy - the backend only needs an identifier to rank.
                                    put("id", style.id)
                                    put("titleKey", style.titleKey)
                                    put("category", style.category.name)
                                    put("silhouette", style.silhouette.name)
                                },
                            )
                        }
                    })
                    put(
                        "preferences",
                        JSONObject().apply {
                            put("gender", gender ?: JSONObject.NULL)
                            put("occasions", JSONArray(occasions.map { it.name }))
                            put("favorites", JSONArray(favorites.toList()))
                        },
                    )
                    put("limit", limit)
                }
                val request = Request.Builder()
                    .url(endpoint(baseUrl))
                    .post(body.toString().toRequestBody(JSON_MEDIA))
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val text = response.body?.string() ?: return@runCatching null
                    parseResponse(text, candidates)
                }
            }.getOrNull()
        }
    }

    /** Health probe used by the settings screen's "Test connection" button. */
    suspend fun health(baseUrl: String): Boolean {
        if (baseUrl.isBlank()) return false
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(endpoint(baseUrl).replace("/v1/recommend", "/v1/health"))
                    .get()
                    .build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
        }
    }

    private fun endpoint(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return if (trimmed.endsWith("/v1")) "$trimmed/recommend" else "$trimmed/v1/recommend"
    }

    /** Exposed for the unit test that pins URL normalisation. */
    internal fun endpointForTest(baseUrl: String): String = endpoint(baseUrl)

    private fun fabricJson(fabric: Fabric): JSONObject = JSONObject().apply {
        put("id", fabric.id)
        put("name", fabric.name ?: JSONObject.NULL)
        put("category", fabric.category.name)
        put("pattern", fabric.pattern.name)
        put("texture", fabric.texture.name)
        put("colors", JSONArray(fabric.colors.map { String.format("#%06X", it.argb and 0xFFFFFF) }))
        put("confidence", fabric.confidence)
    }

    /**
     * Parses the reply defensively. Anything unrecognised is dropped rather than guessed
     * at: a style id that is not in [candidates] is discarded, so a buggy or hostile
     * backend cannot invent catalogue entries.
     */
    internal fun parseResponse(
        text: String,
        candidates: List<DressStyle>,
    ): RecommendationResponse? {
        return runCatching {
            val root = JSONObject(text)
            val known = candidates.map { it.id }.toSet()
            val array = root.optJSONArray("recommendations") ?: JSONArray()
            val scored = buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("styleId").takeIf { it.isNotBlank() } ?: continue
                    if (id !in known) continue
                    add(
                        ScoredStyle(
                            styleId = id,
                            // Clamped so a backend cannot claim a 1.0 certainty; the UI
                            // prints this number next to the fabric it describes.
                            score = item.optDouble("score", 0.0).toFloat().coerceIn(0f, 1f),
                            reasons = item.optJSONArray("reasons")?.let { reasons ->
                                buildList {
                                    for (r in 0 until reasons.length()) {
                                        reasons.optString(r).takeIf { it.isNotBlank() }?.let(::add)
                                    }
                                }
                            }.orEmpty(),
                            customization = item.optJSONObject("customization")?.let(::parseCustomization),
                        ),
                    )
                }
            }
            if (scored.isEmpty()) null
            else RecommendationResponse(scored, root.optString("modelVersion").takeIf { it.isNotBlank() })
        }.getOrNull()
    }

    private fun parseCustomization(json: JSONObject): DesignCustomization {
        val defaults = DesignCustomization()
        return defaults.copy(
            sleeve = json.optString("sleeve")
                .let { name -> SleeveOption.values().firstOrNull { it.name.equals(name, true) } }
                ?: defaults.sleeve,
            neckline = json.optString("neckline")
                .let { name -> NecklineOption.values().firstOrNull { it.name.equals(name, true) } }
                ?: defaults.neckline,
            length = json.optString("length")
                .let { name -> LengthOption.values().firstOrNull { it.name.equals(name, true) } }
                ?: defaults.length,
            placement = json.optString("placement")
                .let { name -> FabricPlacement.values().firstOrNull { it.name.equals(name, true) } }
                ?: defaults.placement,
        )
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
