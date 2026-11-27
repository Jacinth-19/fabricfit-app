package com.fabricfit.app.domain.model

/**
 * A scored pairing of a fabric and a garment design, with human-readable reasons.
 * Reasons are modelled (not pre-formatted) so the UI can render them localized with arguments.
 */
data class Recommendation(
    val fabricId: Long?,
    val style: DressStyle,
    /** 0..100 */
    val score: Int,
    val reasons: List<WhyReason>,
) {
    val matchLabel: Int get() = score
}

sealed interface WhyReason {
    data object PatternStructured : WhyReason
    data object PatternFlowing : WhyReason
    data object ScaleLarge : WhyReason
    data object ScaleSmall : WhyReason
    data object TextureHeavy : WhyReason
    data object TextureLight : WhyReason
    data object ColorsRich : WhyReason
    data object ColorsMuted : WhyReason
    data class OccasionFit(val occasion: Occasion) : WhyReason
    data object Tradition : WhyReason
    data class GenderFit(val gender: GenderCategory) : WhyReason

    /**
     * A plain-language reason supplied by a configured recommendations backend. Kept as a
     * separate case so the UI never presents remote copy as if the on-device engine had
     * derived it.
     */
    data class ReasonFromCloud(val text: String) : WhyReason
}
