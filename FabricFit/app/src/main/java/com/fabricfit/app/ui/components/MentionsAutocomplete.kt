package com.fabricfit.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fabricfit.app.domain.model.CommunityUser

/**
 * Autocomplete dropdown for @mentions.
 */
@Composable
fun MentionsAutocomplete(
    users: List<CommunityUser>,
    onUserSelected: (CommunityUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (users.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        LazyColumn {
            items(users.take(5)) { user ->
                MentionItem(
                    user = user,
                    onClick = { onUserSelected(user) },
                )
            }
        }
    }
}

@Composable
private fun MentionItem(
    user: CommunityUser,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = user.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.clip(CircleShape),
                )
            } else {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = user.displayName.first().uppercase(),
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Parse text to detect @mentions and trigger autocomplete.
 */
object MentionsParser {
    private val MENTION_PATTERN = Regex("@(\\w*)$")

    /**
     * Extract the current mention being typed (if any).
     * Returns the partial username after @, or null if not typing a mention.
     */
    fun getCurrentMention(text: String, cursorPosition: Int): String? {
        val textBeforeCursor = text.substring(0, cursorPosition)
        val match = MENTION_PATTERN.find(textBeforeCursor)
        return match?.groupValues?.get(1)
    }

    /**
     * Replace the current mention with the selected user.
     */
    fun insertMention(
        text: String,
        cursorPosition: Int,
        user: CommunityUser,
    ): Pair<String, Int> {
        val textBeforeCursor = text.substring(0, cursorPosition)
        val match = MENTION_PATTERN.find(textBeforeCursor)
        
        return if (match != null) {
            val mentionStart = match.range.first
            val newText = text.substring(0, mentionStart) + 
                "@${user.username} " + 
                text.substring(cursorPosition)
            val newCursorPosition = mentionStart + user.username.length + 2
            Pair(newText, newCursorPosition)
        } else {
            Pair(text, cursorPosition)
        }
    }
}
