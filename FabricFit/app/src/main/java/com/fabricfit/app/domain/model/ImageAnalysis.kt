package com.fabricfit.app.domain.model

/**
 * Detected clothing item in an image.
 */
data class DetectedItem(
    val type: ClothingType,
    val confidence: Float, // 0.0 to 1.0
    val boundingBox: BoundingBox? = null,
    val attributes: List<ItemAttribute> = emptyList(),
)

enum class ClothingType {
    SHIRT,
    PANTS,
    DRESS,
    SKIRT,
    JACKET,
    SHOES,
    BAG,
    ACCESSORIES,
    HAT,
    UNKNOWN,
}

/**
 * Bounding box for detected item.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * Attribute of a detected item (color, pattern, brand, etc.).
 */
data class ItemAttribute(
    val type: AttributeType,
    val value: String,
    val confidence: Float,
)

enum class AttributeType {
    COLOR,
    PATTERN,
    MATERIAL,
    BRAND,
    STYLE,
}

/**
 * Complete image analysis result.
 */
data class ImageAnalysisResult(
    val detectedItems: List<DetectedItem>,
    val dominantColors: List<ColorInfo>,
    val suggestedHashtags: List<String>,
    val suggestedTags: List<String>,
    val overallStyle: String?,
    val confidence: Float,
)

/**
 * Color information from image analysis.
 */
data class ColorInfo(
    val name: String,
    val hexCode: String,
    val percentage: Float, // Percentage of image
)
