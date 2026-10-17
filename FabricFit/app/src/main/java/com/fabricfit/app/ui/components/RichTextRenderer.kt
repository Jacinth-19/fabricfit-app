package com.fabricfit.app.ui.components

import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonPlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * Renders markdown text using Markwon library.
 */
@Composable
fun RichTextRenderer(
    text: String,
    markwon: Markwon,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                // Configure TextView for markdown rendering
                setTextColor(android.graphics.Color.BLACK)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, text)
        }
    )
}

/**
 * Create a configured Markwon instance with plugins.
 */
@Composable
fun rememberMarkwon(): Markwon {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(object : MarkwonPlugin {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .headingBreakHeight(0)
                        .thematicBreakHeight(2)
                }
            })
            .build()
    }
}
