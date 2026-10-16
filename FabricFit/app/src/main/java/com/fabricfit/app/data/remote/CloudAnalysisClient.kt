package com.fabricfit.app.data.remote

import com.fabricfit.app.domain.model.AnalysisEngine
import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.PatternType
import com.fabricfit.app.domain.model.TextureType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Optional cloud CV & Virtual Try-On (VTON) backend. Dormant until an operator sets a base URL
 * in settings; the app never ships a hidden endpoint and the default engine stays on-device.
 *
 * Expected server contracts:
 *   POST {base}/v1/analyze   multipart "image" (jpeg) -> JSON RemoteAnalysisDto
 *   POST {base}/v1/tryon     multipart "person_image" + "fabric_image" + style params -> JSON RemoteTryOnDto
 */
interface AnalysisApi {
    @Multipart
    @POST("v1/analyze")
    suspend fun analyze(@Part image: MultipartBody.Part): retrofit2.Response<RemoteAnalysisDto>

    @Multipart
    @POST("v1/tryon")
    suspend fun synthesizeTryOn(
        @Part personImage: MultipartBody.Part,
        @Part fabricImage: MultipartBody.Part,
        @Part("style_id") styleId: okhttp3.RequestBody,
        @Part("sleeve") sleeve: okhttp3.RequestBody,
        @Part("neckline") neckline: okhttp3.RequestBody,
        @Part("length") length: okhttp3.RequestBody,
        @Part("placement") placement: okhttp3.RequestBody,
    ): retrofit2.Response<RemoteTryOnDto>
}

data class RemoteAnalysisDto(
    val category: String? = null,
    val confidence: Float = 0f,
    val pattern: String? = null,
    val texture: String? = null,
)

data class RemoteTryOnDto(
    val imageBase64: String? = null,
    val imageUrl: String? = null,
    val modelVersion: String? = null,
)

/** Result of a remote analysis; mapped into the domain with caps so honesty holds. */
data class CloudVerdict(
    val category: FabricCategory,
    val confidence: Float,
    val pattern: PatternType?,
    val texture: TextureType?,
)

class CloudAnalysisClient {

    private var api: AnalysisApi? = null
    private var builtFor: String? = null

    private fun apiFor(baseUrl: String): AnalysisApi {
        api?.let { if (builtFor == baseUrl) return it }
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(AnalysisApi::class.java).also {
            api = it
            builtFor = baseUrl
        }
    }

    /** Null when no backend is configured or the call fails — caller falls back to on-device. */
    suspend fun analyze(baseUrl: String, jpeg: ByteArray): CloudVerdict? {
        return try {
            val body = jpeg.toRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("image", "fabric.jpg", body)
            val response = apiFor(baseUrl).analyze(part)
            if (!response.isSuccessful) return null
            val dto = response.body() ?: return null
            CloudVerdict(
                category = FabricCategory.values()
                    .firstOrNull { it.name.equals(dto.category, ignoreCase = true) }
                    ?: FabricCategory.UNKNOWN,
                confidence = dto.confidence.coerceIn(0f, 0.8f), // never overstate, even remotely
                pattern = PatternType.values().firstOrNull { it.name.equals(dto.pattern, ignoreCase = true) },
                texture = TextureType.values().firstOrNull { it.name.equals(dto.texture, ignoreCase = true) },
            )
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Calls the remote generative Virtual Try-On (IDM-VTON / TryOnDiffusion) endpoint when a
     * cloud base URL is configured, returning null to fall back cleanly to on-device warping.
     */
    suspend fun tryOn(
        baseUrl: String,
        personJpeg: ByteArray,
        fabricJpeg: ByteArray,
        styleId: String,
        customization: DesignCustomization,
    ): RemoteTryOnDto? {
        if (baseUrl.isBlank()) return null
        return try {
            val textMedia = "text/plain".toMediaType()
            val personPart = MultipartBody.Part.createFormData(
                "person_image",
                "person.jpg",
                personJpeg.toRequestBody("image/jpeg".toMediaType()),
            )
            val fabricPart = MultipartBody.Part.createFormData(
                "fabric_image",
                "fabric.jpg",
                fabricJpeg.toRequestBody("image/jpeg".toMediaType()),
            )
            val response = apiFor(baseUrl).synthesizeTryOn(
                personImage = personPart,
                fabricImage = fabricPart,
                styleId = styleId.toRequestBody(textMedia),
                sleeve = customization.sleeve.name.toRequestBody(textMedia),
                neckline = customization.neckline.name.toRequestBody(textMedia),
                length = customization.length.name.toRequestBody(textMedia),
                placement = customization.placement.name.toRequestBody(textMedia),
            )
            if (!response.isSuccessful) null else response.body()
        } catch (_: Throwable) {
            null
        }
    }

    fun engineFor(baseUrl: String): AnalysisEngine =
        if (baseUrl.isBlank()) AnalysisEngine.ON_DEVICE_DEMO else AnalysisEngine.CLOUD
}
