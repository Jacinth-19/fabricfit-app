package com.fabricfit.app.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.fabricfit.app.R
import java.io.File
import java.io.FileOutputStream

/**
 * Share manager for sharing fabrics and looks to social media and other apps.
 */
object ShareManager {
    
    /**
     * Share a fabric image with details to social media or other apps.
     * 
     * @param context Application context
     * @param imageUri URI of the fabric image
     * @param fabricName Name of the fabric
     * @param fabricCategory Category of the fabric
     * @param fabricPattern Pattern of the fabric
     */
    fun shareFabric(
        context: Context,
        imageUri: String,
        fabricName: String?,
        fabricCategory: String,
        fabricPattern: String
    ) {
        val shareText = context.getString(
            R.string.share_fabric_text,
            fabricName ?: fabricCategory,
            fabricPattern
        )
        
        shareImage(context, imageUri, shareText)
    }
    
    /**
     * Share a look/outfit image with details.
     * 
     * @param context Application context
     * @param imageUri URI of the look image
     * @param lookTitle Title of the look
     * @param fabricName Name of the fabric used
     */
    fun shareLook(
        context: Context,
        imageUri: String,
        lookTitle: String,
        fabricName: String?
    ) {
        val shareText = if (fabricName != null) {
            context.getString(R.string.share_look_text_with_fabric, lookTitle, fabricName)
        } else {
            context.getString(R.string.share_look_text, lookTitle)
        }
        
        shareImage(context, imageUri, shareText)
    }
    
    /**
     * Share a bitmap image with text.
     * 
     * @param context Application context
     * @param bitmap Bitmap to share
     * @param text Text to share with the image
     */
    fun shareBitmap(context: Context, bitmap: Bitmap, text: String) {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        
        val file = File(cachePath, "shared_image.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        shareImage(context, uri.toString(), text)
    }
    
    /**
     * Share an image with text using Android's share sheet.
     * 
     * @param context Application context
     * @param imageUri URI of the image to share
     * @param text Text to share with the image
     */
    private fun shareImage(context: Context, imageUri: String, text: String) {
        val uri = if (imageUri.startsWith("content://")) {
            Uri.parse(imageUri)
        } else {
            val file = File(imageUri)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_title))
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
    
    /**
     * Share text only (no image).
     * 
     * @param context Application context
     * @param text Text to share
     * @param subject Optional subject for email shares
     */
    fun shareText(context: Context, text: String, subject: String? = null) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_title))
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}
