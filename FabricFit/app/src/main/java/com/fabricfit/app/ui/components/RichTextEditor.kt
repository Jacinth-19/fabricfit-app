package com.fabricfit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabricfit.app.R
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher

/**
 * Rich text editor with markdown support and formatting toolbar.
 */
@Composable
fun RichTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    markwon: Markwon,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    minLines: Int = 3,
    maxLines: Int = 10,
    enabled: Boolean = true,
    users: List<com.fabricfit.app.domain.model.CommunityUser> = emptyList(),
    onMentionSearch: (String) -> Unit = {},
) {
    val editor = remember(markwon) { MarkwonEditor.create(markwon) }
    var textSelection by remember { mutableStateOf(0..0) }
    var cursorPosition by remember { mutableStateOf(0) }
    
    // Detect current mention being typed
    val currentMention = remember(value, cursorPosition) {
        MentionsParser.getCurrentMention(value, cursorPosition)
    }

    Column(modifier = modifier) {
        // Formatting toolbar
        FormattingToolbar(
            onBoldClick = { insertFormatting(value, onValueChange, "**", "**") },
            onItalicClick = { insertFormatting(value, onValueChange, "*", "*") },
            onCodeClick = { insertFormatting(value, onValueChange, "`", "`") },
            onLinkClick = { insertFormatting(value, onValueChange, "[", "](url)") },
            onQuoteClick = { insertFormatting(value, onValueChange, "> ", "") },
            onListClick = { insertFormatting(value, onValueChange, "- ", "") },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Text field with markdown highlighting
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                cursorPosition = newValue.length
            },
            label = if (label.isNotEmpty()) {
                { Text(label) }
            } else null,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            maxLines = maxLines,
            enabled = enabled,
        )

        // Mentions autocomplete
        if (currentMention != null) {
            // Trigger search for users matching the mention
            onMentionSearch(currentMention)
            
            val filteredUsers = users.filter { user ->
                user.username.startsWith(currentMention, ignoreCase = true) ||
                user.displayName.contains(currentMention, ignoreCase = true)
            }
            
            if (filteredUsers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                MentionsAutocomplete(
                    users = filteredUsers,
                    onUserSelected = { user ->
                        val (newText, newCursor) = MentionsParser.insertMention(
                            text = value,
                            cursorPosition = cursorPosition,
                            user = user,
                        )
                        onValueChange(newText)
                        cursorPosition = newCursor
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Helper text
        Text(
            text = stringResource(R.string.richtext_helper),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Formatting toolbar with markdown action buttons.
 */
@Composable
private fun FormattingToolbar(
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onCodeClick: () -> Unit,
    onLinkClick: () -> Unit,
    onQuoteClick: () -> Unit,
    onListClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FormatButton(
                icon = Icons.Filled.FormatBold,
                contentDescription = stringResource(R.string.format_bold),
                onClick = onBoldClick,
            )
            FormatButton(
                icon = Icons.Filled.FormatItalic,
                contentDescription = stringResource(R.string.format_italic),
                onClick = onItalicClick,
            )
            FormatButton(
                icon = Icons.Filled.Code,
                contentDescription = stringResource(R.string.format_code),
                onClick = onCodeClick,
            )
            FormatButton(
                icon = Icons.Filled.Link,
                contentDescription = stringResource(R.string.format_link),
                onClick = onLinkClick,
            )
            FormatButton(
                icon = Icons.Filled.FormatQuote,
                contentDescription = stringResource(R.string.format_quote),
                onClick = onQuoteClick,
            )
            FormatButton(
                icon = Icons.Filled.FormatListBulleted,
                contentDescription = stringResource(R.string.format_list),
                onClick = onListClick,
            )
        }
    }
}

@Composable
private fun FormatButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Insert markdown formatting around selected text or at cursor.
 */
private fun insertFormatting(
    currentValue: String,
    onValueChange: (String) -> Unit,
    prefix: String,
    suffix: String,
) {
    // For simplicity, append formatting at the end
    // In a full implementation, you'd track selection and insert around it
    val newValue = currentValue + prefix + "text" + suffix
    onValueChange(newValue)
}

/**
 * Parse mentions and hashtags from text.
 */
object RichTextParser {
    private val MENTION_REGEX = Regex("@(\\w+)")
    private val HASHTAG_REGEX = Regex("#(\\w+)")

    data class ParsedContent(
        val mentions: List<String>,
        val hashtags: List<String>,
    )

    fun parse(text: String): ParsedContent {
        val mentions = MENTION_REGEX.findAll(text).map { it.groupValues[1] }.toList()
        val hashtags = HASHTAG_REGEX.findAll(text).map { it.groupValues[1] }.toList()
        return ParsedContent(mentions = mentions, hashtags = hashtags)
    }

    fun extractMentions(text: String): List<String> {
        return MENTION_REGEX.findAll(text).map { it.groupValues[1] }.toList()
    }

    fun extractHashtags(text: String): List<String> {
        return HASHTAG_REGEX.findAll(text).map { it.groupValues[1] }.toList()
    }
}
