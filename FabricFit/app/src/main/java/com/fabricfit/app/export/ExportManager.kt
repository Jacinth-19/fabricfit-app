package com.fabricfit.app.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.GeneratedLook
import java.io.File
import java.io.FileOutputStream

/**
 * Export manager for exporting wardrobe data to PDF and images.
 */
object ExportManager {
    
    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 50f
    
    /**
     * Export wardrobe to PDF document.
     * 
     * @param context Application context
     * @param fabrics List of fabrics to export
     * @param looks List of looks to export
     * @param fileName Name of the output PDF file
     * @return File object of the created PDF
     */
    fun exportWardrobeToPdf(
        context: Context,
        fabrics: List<Fabric>,
        looks: List<GeneratedLook>,
        fileName: String = "FabricFit_Wardrobe.pdf"
    ): File {
        val document = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 32f
        }
        val subtitlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textSize = 18f
        }
        val bodyPaint = Paint().apply {
            textSize = 14f
        }
        
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var yPosition = MARGIN
        
        // Title page
        canvas.drawText("My FabricFit Wardrobe", MARGIN, yPosition, titlePaint)
        yPosition += 50f
        
        canvas.drawText("${fabrics.size} Fabrics · ${looks.size} Looks", MARGIN, yPosition, subtitlePaint)
        yPosition += 100f
        
        // Fabrics section
        canvas.drawText("Fabrics", MARGIN, yPosition, titlePaint)
        yPosition += 40f
        
        for ((index, fabric) in fabrics.withIndex()) {
            if (yPosition > PAGE_HEIGHT - 100f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                yPosition = MARGIN
            }
            
            val fabricName = fabric.name ?: "Fabric ${index + 1}"
            val category = fabric.category.name
            val pattern = fabric.pattern.name
            
            canvas.drawText("$fabricName", MARGIN, yPosition, subtitlePaint)
            yPosition += 25f
            
            canvas.drawText("Category: $category", MARGIN + 20f, yPosition, bodyPaint)
            yPosition += 20f
            
            canvas.drawText("Pattern: $pattern", MARGIN + 20f, yPosition, bodyPaint)
            yPosition += 20f
            
            canvas.drawText("Confidence: ${(fabric.confidence * 100).toInt()}%", MARGIN + 20f, yPosition, bodyPaint)
            yPosition += 40f
        }
        
        // Looks section
        if (looks.isNotEmpty()) {
            if (yPosition > PAGE_HEIGHT - 200f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                yPosition = MARGIN
            }
            
            yPosition += 50f
            canvas.drawText("Looks", MARGIN, yPosition, titlePaint)
            yPosition += 40f
            
            for ((index, look) in looks.withIndex()) {
                if (yPosition > PAGE_HEIGHT - 100f) {
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    yPosition = MARGIN
                }
                
                // The display title lives in string resources behind DressStyle.titleKey,
                // which is not reachable from here, so fall back to the style id.
                val lookName = look.dressStyleId
                    .replace('_', ' ')
                    .replaceFirstChar { it.uppercase() }
                canvas.drawText(lookName, MARGIN, yPosition, subtitlePaint)
                yPosition += 25f
                
                canvas.drawText("Created: ${look.createdAt}", MARGIN + 20f, yPosition, bodyPaint)
                yPosition += 40f
            }
        }
        
        document.finishPage(page)
        
        // Save to file
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
        }
        
        document.close()
        return file
    }
    
    /**
     * Export a single fabric as an image with details overlay.
     * 
     * @param context Application context
     * @param fabric Fabric to export
     * @param fabricBitmap Bitmap of the fabric image
     * @return Bitmap of the exported image
     */
    fun exportFabricAsImage(
        context: Context,
        fabric: Fabric,
        fabricBitmap: Bitmap
    ): Bitmap {
        val width = fabricBitmap.width
        val height = fabricBitmap.height + 200 // Add space for text
        
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        
        // Draw fabric image
        canvas.drawBitmap(fabricBitmap, 0f, 0f, null)
        
        // Draw white background for text
        val backgroundPaint = Paint().apply {
            color = android.graphics.Color.WHITE
        }
        canvas.drawRect(0f, fabricBitmap.height.toFloat(), width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Draw text
        val textPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 32f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        
        val fabricName = fabric.name ?: fabric.category.name
        canvas.drawText(fabricName, 20f, fabricBitmap.height + 50f, textPaint)
        
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("${fabric.category.name} · ${fabric.pattern.name}", 20f, fabricBitmap.height + 90f, textPaint)
        canvas.drawText("Confidence: ${(fabric.confidence * 100).toInt()}%", 20f, fabricBitmap.height + 130f, textPaint)
        
        return resultBitmap
    }
    
    /**
     * Get the export directory for the app.
     * 
     * @param context Application context
     * @return File object of the export directory
     */
    fun getExportDirectory(context: Context): File {
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return exportDir
    }
}
