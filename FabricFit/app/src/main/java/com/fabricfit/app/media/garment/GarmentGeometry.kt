package com.fabricfit.app.media.garment

import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.GarmentSilhouette
import com.fabricfit.app.domain.model.LengthOption
import com.fabricfit.app.domain.model.NecklineOption
import com.fabricfit.app.domain.model.SleeveOption

/** A closed polygon of normalised (0..1) points. */
typealias Poly = List<Pair<Float, Float>>

/**
 * Normalised garment outlines (0..1 in both axes) shared by the Compose illustrations and the
 * demo try-on compositor, so a design always looks like itself everywhere.
 *
 * Each shape is a list of sub-paths; every sub-path is a closed polygon of points.
 */
object GarmentGeometry {

    fun shapesFor(
        silhouette: GarmentSilhouette,
        customization: DesignCustomization? = null,
    ): List<Poly> {
        val base = baseShapesFor(silhouette)
        if (customization == null) return base
        return applyCustomization(base, customization)
    }

    private fun applyCustomization(
        polys: List<Poly>,
        custom: DesignCustomization,
    ): List<Poly> {
        val targetBottomY = when (custom.length) {
            LengthOption.SHORT -> 0.72f
            LengthOption.MIDI -> 0.84f
            LengthOption.LONG -> 0.95f
            LengthOption.FLOOR -> 0.995f
            null -> null
        }

        val transformed = polys.map { poly ->
            poly.map { (rawX, rawY) ->
                var x = rawX
                var y = rawY

                // 1. Neckline reshaping on upper torso vertices
                when (custom.neckline) {
                    NecklineOption.V_NECK -> {
                        if (y < 0.08f && x in 0.44f..0.56f) {
                            y = 0.14f
                        }
                    }
                    NecklineOption.OFF_SHOULDER -> {
                        if (y < 0.09f) {
                            y = 0.11f
                            if (x < 0.4f) x = (x - 0.04f).coerceAtLeast(0.18f)
                            if (x > 0.6f) x = (x + 0.04f).coerceAtMost(0.82f)
                        }
                    }
                    NecklineOption.BOAT -> {
                        if (y < 0.08f) {
                            y = 0.035f
                        }
                    }
                    NecklineOption.HIGH -> {
                        if (y < 0.06f && x in 0.4f..0.6f) {
                            y = 0.0f
                        }
                    }
                    NecklineOption.ROUND, null -> Unit
                }

                // 2. Sleeve reshaping on outer shoulder/arm vertices
                when (custom.sleeve) {
                    SleeveOption.SLEEVELESS -> {
                        if (y in 0.04f..0.38f) {
                            if (x < 0.36f) x = 0.36f
                            if (x > 0.64f) x = 0.64f
                        }
                    }
                    SleeveOption.PUFF -> {
                        if (y in 0.05f..0.32f) {
                            if (x < 0.38f) x = (x - 0.07f).coerceAtLeast(0.12f)
                            if (x > 0.62f) x = (x + 0.07f).coerceAtMost(0.88f)
                        }
                    }
                    SleeveOption.SHORT, SleeveOption.THREE_QUARTER, SleeveOption.LONG, null -> Unit
                }

                // 3. Hem length scaling on lower garment vertices
                if (targetBottomY != null && y > 0.48f) {
                    val t = ((y - 0.48f) / 0.50f).coerceIn(0f, 1f)
                    y = (0.48f + t * (targetBottomY - 0.48f)).coerceIn(0.48f, 0.995f)
                    if (custom.length == LengthOption.FLOOR && t > 0.7f) {
                        if (x < 0.45f) x = (x - 0.03f).coerceAtLeast(0.10f)
                        if (x > 0.55f) x = (x + 0.03f).coerceAtMost(0.90f)
                    }
                }

                x to y
            }
        }.toMutableList()

        // Add explicit 3/4 or Long sleeve polygons when selected
        when (custom.sleeve) {
            SleeveOption.THREE_QUARTER -> {
                transformed += poly(0.26f, 0.07f, 0.34f, 0.07f, 0.28f, 0.44f, 0.19f, 0.42f)
                transformed += poly(0.66f, 0.07f, 0.74f, 0.07f, 0.81f, 0.42f, 0.72f, 0.44f)
            }
            SleeveOption.LONG -> {
                transformed += poly(0.26f, 0.07f, 0.34f, 0.07f, 0.25f, 0.58f, 0.17f, 0.56f)
                transformed += poly(0.66f, 0.07f, 0.74f, 0.07f, 0.83f, 0.56f, 0.75f, 0.58f)
            }
            else -> Unit
        }

        // Add high-collar band when HIGH neckline is selected
        if (custom.neckline == NecklineOption.HIGH) {
            transformed += poly(0.42f, 0.0f, 0.58f, 0.0f, 0.60f, 0.055f, 0.40f, 0.055f)
        }

        return transformed
    }

    private fun baseShapesFor(silhouette: GarmentSilhouette): List<Poly> = when (silhouette) {
        GarmentSilhouette.KABA -> listOf(
            poly(0.32f, 0.06f, 0.5f, 0.02f, 0.68f, 0.06f, 0.74f, 0.3f, 0.62f, 0.34f, 0.66f, 0.98f, 0.34f, 0.98f, 0.38f, 0.34f, 0.26f, 0.3f),
        )
        GarmentSilhouette.MODERN_KABA -> listOf(
            poly(0.3f, 0.05f, 0.5f, 0.01f, 0.7f, 0.05f, 0.78f, 0.22f, 0.68f, 0.26f, 0.64f, 0.42f, 0.72f, 0.98f, 0.28f, 0.98f, 0.36f, 0.42f, 0.32f, 0.26f, 0.22f, 0.22f),
        )
        GarmentSilhouette.MAXI -> listOf(
            poly(0.34f, 0.04f, 0.5f, 0.0f, 0.66f, 0.04f, 0.7f, 0.26f, 0.6f, 0.3f, 0.8f, 0.98f, 0.2f, 0.98f, 0.4f, 0.3f, 0.3f, 0.26f),
        )
        GarmentSilhouette.STRAIGHT -> listOf(
            poly(0.34f, 0.04f, 0.5f, 0.0f, 0.66f, 0.04f, 0.7f, 0.3f, 0.66f, 0.96f, 0.34f, 0.96f, 0.3f, 0.3f),
        )
        GarmentSilhouette.MERMAID -> listOf(
            poly(0.36f, 0.03f, 0.5f, 0.0f, 0.64f, 0.03f, 0.66f, 0.3f, 0.6f, 0.6f, 0.58f, 0.74f, 0.76f, 0.98f, 0.24f, 0.98f, 0.42f, 0.74f, 0.4f, 0.6f, 0.34f, 0.3f),
        )
        GarmentSilhouette.PEPLUM -> listOf(
            poly(0.34f, 0.04f, 0.5f, 0.0f, 0.66f, 0.04f, 0.68f, 0.34f, 0.76f, 0.5f, 0.24f, 0.5f, 0.32f, 0.34f),
            poly(0.36f, 0.5f, 0.64f, 0.5f, 0.68f, 0.98f, 0.32f, 0.98f),
        )
        GarmentSilhouette.JUMPSUIT -> listOf(
            poly(0.34f, 0.04f, 0.5f, 0.0f, 0.66f, 0.04f, 0.68f, 0.42f, 0.7f, 0.98f, 0.54f, 0.98f, 0.5f, 0.56f, 0.46f, 0.98f, 0.3f, 0.98f, 0.32f, 0.42f),
        )
        GarmentSilhouette.SKIRT_BLOUSE -> listOf(
            poly(0.32f, 0.04f, 0.5f, 0.0f, 0.68f, 0.04f, 0.7f, 0.4f, 0.3f, 0.4f),
            poly(0.34f, 0.44f, 0.66f, 0.44f, 0.74f, 0.98f, 0.26f, 0.98f),
        )
        GarmentSilhouette.GOWN -> listOf(
            poly(0.36f, 0.03f, 0.5f, 0.0f, 0.64f, 0.03f, 0.66f, 0.26f, 0.58f, 0.32f, 0.86f, 0.98f, 0.14f, 0.98f, 0.42f, 0.32f, 0.34f, 0.26f),
        )
        GarmentSilhouette.FORMAL_DRESS -> listOf(
            poly(0.35f, 0.04f, 0.5f, 0.0f, 0.65f, 0.04f, 0.68f, 0.32f, 0.62f, 0.92f, 0.38f, 0.92f, 0.32f, 0.32f),
        )
        GarmentSilhouette.KAFTAN -> listOf(
            poly(0.34f, 0.04f, 0.5f, 0.0f, 0.66f, 0.04f, 0.76f, 0.5f, 0.7f, 0.98f, 0.3f, 0.98f, 0.24f, 0.5f),
            poly(0.46f, 0.08f, 0.54f, 0.08f, 0.54f, 0.3f, 0.46f, 0.3f),
        )
        GarmentSilhouette.AGBADA -> listOf(
            poly(0.36f, 0.03f, 0.5f, 0.0f, 0.64f, 0.03f, 0.92f, 0.5f, 0.74f, 0.56f, 0.76f, 0.98f, 0.24f, 0.98f, 0.26f, 0.56f, 0.08f, 0.5f),
        )
        GarmentSilhouette.PRINT_SHIRT -> listOf(
            poly(0.32f, 0.06f, 0.5f, 0.0f, 0.68f, 0.06f, 0.78f, 0.3f, 0.68f, 0.34f, 0.68f, 0.62f, 0.32f, 0.62f, 0.32f, 0.34f, 0.22f, 0.3f),
        )
        GarmentSilhouette.SENATOR -> listOf(
            poly(0.34f, 0.04f, 0.5f, 0.0f, 0.66f, 0.04f, 0.74f, 0.42f, 0.68f, 0.46f, 0.66f, 0.86f, 0.34f, 0.86f, 0.32f, 0.46f, 0.26f, 0.42f),
        )
        GarmentSilhouette.SMOCK -> listOf(
            poly(0.3f, 0.08f, 0.5f, 0.0f, 0.7f, 0.08f, 0.82f, 0.6f, 0.7f, 0.98f, 0.3f, 0.98f, 0.18f, 0.6f),
            poly(0.42f, 0.06f, 0.58f, 0.06f, 0.58f, 0.16f, 0.42f, 0.16f),
        )
        GarmentSilhouette.AFRICAN_SUIT -> listOf(
            poly(0.3f, 0.05f, 0.48f, 0.0f, 0.48f, 0.52f, 0.32f, 0.52f, 0.32f, 0.3f, 0.22f, 0.28f),
            poly(0.7f, 0.05f, 0.52f, 0.0f, 0.52f, 0.52f, 0.68f, 0.52f, 0.68f, 0.3f, 0.78f, 0.28f),
            poly(0.34f, 0.56f, 0.48f, 0.56f, 0.48f, 0.98f, 0.34f, 0.98f),
            poly(0.52f, 0.56f, 0.66f, 0.56f, 0.66f, 0.98f, 0.52f, 0.98f),
        )
        GarmentSilhouette.SHIRT_TROUSERS -> listOf(
            poly(0.32f, 0.04f, 0.5f, 0.0f, 0.68f, 0.04f, 0.74f, 0.26f, 0.66f, 0.3f, 0.66f, 0.48f, 0.34f, 0.48f, 0.34f, 0.3f, 0.26f, 0.26f),
            poly(0.36f, 0.52f, 0.48f, 0.52f, 0.47f, 0.98f, 0.36f, 0.98f),
            poly(0.52f, 0.52f, 0.64f, 0.52f, 0.64f, 0.98f, 0.53f, 0.98f),
        )
        GarmentSilhouette.MODERN_TRADITIONAL -> listOf(
            poly(0.34f, 0.04f, 0.5f, 0.0f, 0.66f, 0.04f, 0.74f, 0.36f, 0.66f, 0.4f, 0.64f, 0.9f, 0.36f, 0.9f, 0.34f, 0.4f, 0.26f, 0.36f),
            poly(0.44f, 0.04f, 0.56f, 0.04f, 0.56f, 0.2f, 0.44f, 0.2f),
        )
        GarmentSilhouette.COVER_UP -> listOf(
            poly(0.3f, 0.06f, 0.5f, 0.0f, 0.7f, 0.06f, 0.84f, 0.44f, 0.72f, 0.5f, 0.74f, 0.96f, 0.26f, 0.96f, 0.28f, 0.5f, 0.16f, 0.44f),
        )
        GarmentSilhouette.CO_ORDS -> listOf(
            poly(0.32f, 0.04f, 0.5f, 0.0f, 0.68f, 0.04f, 0.72f, 0.28f, 0.64f, 0.32f, 0.64f, 0.5f, 0.36f, 0.5f, 0.36f, 0.32f, 0.28f, 0.28f),
            poly(0.34f, 0.54f, 0.66f, 0.54f, 0.7f, 0.86f, 0.54f, 0.86f, 0.5f, 0.7f, 0.46f, 0.86f, 0.3f, 0.86f),
        )
    }

    private fun poly(vararg coords: Float): Poly {
        require(coords.size % 2 == 0)
        return coords.toList().chunked(2).map { it[0] to it[1] }
    }
}
