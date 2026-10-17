package com.fabricfit.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.domain.model.CommunityNotificationPreferences

/**
 * Screen for managing notification preferences.
 */
@Composable
fun NotificationPreferencesScreen(nav: NavHostController, padding: PaddingValues) {
    var preferences by remember { mutableStateOf(CommunityNotificationPreferences()) }

    Column(Modifier.fillMaxSize().padding(padding)) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                stringResource(R.string.notification_preferences_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }

        // Preferences list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.notification_category_social),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(8.dp))

            NotificationPreferenceItem(
                title = stringResource(R.string.notification_pref_likes),
                description = stringResource(R.string.notification_pref_likes_desc),
                checked = preferences.likesEnabled,
                onCheckedChange = { preferences = preferences.copy(likesEnabled = it) },
            )

            NotificationPreferenceItem(
                title = stringResource(R.string.notification_pref_comments),
                description = stringResource(R.string.notification_pref_comments_desc),
                checked = preferences.commentsEnabled,
                onCheckedChange = { preferences = preferences.copy(commentsEnabled = it) },
            )

            NotificationPreferenceItem(
                title = stringResource(R.string.notification_pref_follows),
                description = stringResource(R.string.notification_pref_follows_desc),
                checked = preferences.followsEnabled,
                onCheckedChange = { preferences = preferences.copy(followsEnabled = it) },
            )

            NotificationPreferenceItem(
                title = stringResource(R.string.notification_pref_mentions),
                description = stringResource(R.string.notification_pref_mentions_desc),
                checked = preferences.mentionsEnabled,
                onCheckedChange = { preferences = preferences.copy(mentionsEnabled = it) },
            )

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.notification_category_community),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(8.dp))

            NotificationPreferenceItem(
                title = stringResource(R.string.notification_pref_challenges),
                description = stringResource(R.string.notification_pref_challenges_desc),
                checked = preferences.challengesEnabled,
                onCheckedChange = { preferences = preferences.copy(challengesEnabled = it) },
            )

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.notification_category_other),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(8.dp))

            NotificationPreferenceItem(
                title = stringResource(R.string.notification_pref_email),
                description = stringResource(R.string.notification_pref_email_desc),
                checked = preferences.emailNotifications,
                onCheckedChange = { preferences = preferences.copy(emailNotifications = it) },
            )

            Spacer(Modifier.height(32.dp))

            Text(
                stringResource(R.string.notification_preferences_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationPreferenceItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
