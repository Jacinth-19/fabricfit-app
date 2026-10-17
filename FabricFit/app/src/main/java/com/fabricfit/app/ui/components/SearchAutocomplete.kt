package com.fabricfit.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Search autocomplete dropdown with suggestions.
 */
@Composable
fun SearchAutocomplete(
    suggestions: List<SearchSuggestion>,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        LazyColumn {
            items(suggestions.take(10)) { suggestion ->
                SuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionClick(suggestion) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = suggestion.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            if (suggestion.subtitle != null) {
                Text(
                    text = suggestion.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Search suggestion data model.
 */
data class SearchSuggestion(
    val text: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val type: SuggestionType,
)

enum class SuggestionType {
    RECENT_SEARCH,
    TAG,
    USER,
    POST,
}

/**
 * Generate search suggestions based on query and available data.
 */
object SearchSuggestionGenerator {
    /**
     * Generate suggestions from recent searches, tags, and users.
     */
    fun generateSuggestions(
        query: String,
        recentSearches: List<String>,
        popularTags: List<String>,
        users: List<String>,
    ): List<SearchSuggestion> {
        val suggestions = mutableListOf<SearchSuggestion>()

        if (query.isBlank()) {
            // Show recent searches when query is empty
            suggestions.addAll(
                recentSearches.take(5).map { search ->
                    SearchSuggestion(
                        text = search,
                        icon = Icons.Filled.History,
                        type = SuggestionType.RECENT_SEARCH,
                    )
                }
            )
        } else {
            val lowerQuery = query.lowercase()

            // Match tags
            suggestions.addAll(
                popularTags
                    .filter { it.lowercase().contains(lowerQuery) }
                    .take(3)
                    .map { tag ->
                        SearchSuggestion(
                            text = "#$tag",
                            subtitle = "Tag",
                            icon = Icons.Filled.Tag,
                            type = SuggestionType.TAG,
                        )
                    }
            )

            // Match users
            suggestions.addAll(
                users
                    .filter { it.lowercase().contains(lowerQuery) }
                    .take(3)
                    .map { user ->
                        SearchSuggestion(
                            text = "@$user",
                            subtitle = "User",
                            icon = Icons.Filled.Person,
                            type = SuggestionType.USER,
                        )
                    }
            )
        }

        return suggestions.take(10)
    }
}
