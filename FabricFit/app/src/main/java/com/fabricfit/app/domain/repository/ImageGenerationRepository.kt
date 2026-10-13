package com.fabricfit.app.domain.repository

import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.Outcome
import java.io.File

data class GenerationInput(
    val fabric: Fabric,
    val style: DressStyle,
    val personImagePath: String,
    val customization: com.fabricfit.app.domain.model.DesignCustomization = com.fabricfit.app.domain.model.DesignCustomization(),
    val variationSeed: Int = 0,
    /** Honours the Data Saver setting: smaller working images mean less memory and storage. */
    val maxDimension: Int = 1000,
)

/**
 * Produces the result image of a try-on. The bundled demo implementation composites on-device;
 * a production virtual try-on / diffusion backend implements the same contract.
 */
interface ImageGenerationRepository {
    suspend fun generate(input: GenerationInput): Outcome<File>
}
