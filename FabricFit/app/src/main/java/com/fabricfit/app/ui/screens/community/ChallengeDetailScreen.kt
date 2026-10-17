package com.fabricfit.app.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fabricfit.app.R
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.ChallengeEntry
import com.fabricfit.app.domain.model.StyleChallenge
import com.fabricfit.app.ui.components.ShimmerGrid
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.theme.HeritageGold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// ViewModel
class ChallengeDetailViewModel(
    private val container: AppContainer,
    private val challengeId: String,
) : ViewModel() {
    private val _challenge = MutableStateFlow<StyleChallenge?>(null)
    val challenge: StateFlow<StyleChallenge?> = _challenge.asStateFlow()

    private val _entries = MutableStateFlow<List<ChallengeEntry>>(emptyList())
    val entries: StateFlow<List<ChallengeEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadChallenge()
        loadEntries()
    }

    private fun loadChallenge() {
        viewModelScope.launch {
            _challenge.value = container.community.getChallenge(challengeId)
            _isLoading.value = false
        }
    }

    private fun loadEntries() {
        viewModelScope.launch {
            container.community.observeChallengeEntries(challengeId).collect { entries ->
                _entries.value = entries
            }
        }
    }

    fun joinChallenge() {
        viewModelScope.launch {
            container.community.joinChallenge(challengeId)
            _challenge.value = container.community.getChallenge(challengeId)
        }
    }

    fun voteForEntry(entryId: String) {
        viewModelScope.launch {
            container.community.voteForEntry(entryId)
        }
    }

    fun submitEntry(galleryItemId: String) {
        viewModelScope.launch {
            container.community.submitChallengeEntry(challengeId, galleryItemId)
        }
    }
}

// Screen
@Composable
fun ChallengeDetailScreen(nav: NavHostController, padding: PaddingValues, challengeId: String) {
    val vm: ChallengeDetailViewModel = appViewModel { c, ctx -> ChallengeDetailViewModel(c, challengeId) }
    val challenge by vm.challenge.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

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
                "Style Challenge",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }

        if (isLoading) {
            ShimmerGrid(
                columns = 1,
                rows = 3,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            )
        } else if (challenge == null) {
            Text(
                "Challenge not found",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // Banner image
                item {
                    if (challenge!!.bannerUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(challenge!!.bannerUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = challenge!!.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(80.dp),
                            )
                        }
                    }
                }

                // Challenge info
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            challenge!!.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(Modifier.height(8.dp))

                        // Countdown timer
                        val daysLeft = calculateDaysLeft(challenge!!.endDate)
                        if (daysLeft > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HeritageGold.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    stringResource(R.string.challenge_ends, "$daysLeft days left"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HeritageGold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    "Challenge ended",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            challenge!!.description,
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Spacer(Modifier.height(16.dp))

                        // Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatBox(
                                count = challenge!!.participantCount,
                                label = "Participants",
                            )
                            StatBox(
                                count = entries.size,
                                label = "Entries",
                            )
                            StatBox(
                                count = challenge!!.prizes.size,
                                label = "Prizes",
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Join/Submit button
                        if (challenge!!.isActive) {
                            if (challenge!!.isParticipating) {
                                Button(
                                    onClick = { nav.navigate(Routes.CREATE_POST) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Text(stringResource(R.string.challenge_submit))
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { vm.joinChallenge() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                ) {
                                    Text(stringResource(R.string.challenge_join))
                                }
                            }
                        }
                    }
                }

                // Rules section
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "Rules & Requirements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(Modifier.height(8.dp))

                        if (challenge!!.requiredFabricTypes.isNotEmpty()) {
                            Text(
                                "Required Fabrics:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            challenge!!.requiredFabricTypes.forEach { fabric ->
                                Text(
                                    "• ${fabric.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        if (challenge!!.requiredOccasions.isNotEmpty()) {
                            Text(
                                "Required Occasions:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            challenge!!.requiredOccasions.forEach { occasion ->
                                Text(
                                    "• ${occasion.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        if (challenge!!.prizes.isNotEmpty()) {
                            Text(
                                "Prizes:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            challenge!!.prizes.forEachIndexed { index, prize ->
                                Text(
                                    "${index + 1}. $prize",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }

                // Leaderboard (top 3)
                if (entries.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                "🏆 Leaderboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            Spacer(Modifier.height(8.dp))

                            entries
                                .sortedByDescending { it.voteCount }
                                .take(3)
                                .forEachIndexed { index, entry ->
                                    LeaderboardItem(
                                        rank = index + 1,
                                        entry = entry,
                                        onUserClick = { nav.navigate(Routes.userProfile(entry.userId)) },
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                        }
                    }
                }

                // All entries section
                item {
                    Text(
                        "All Entries (${entries.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                if (entries.isEmpty()) {
                    item {
                        Text(
                            "No entries yet. Be the first to submit!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    items(entries, key = { it.id }) { entry ->
                        EntryCard(
                            entry = entry,
                            onVoteClick = { vm.voteForEntry(entry.id) },
                            onUserClick = { nav.navigate(Routes.userProfile(entry.userId)) },
                            onGalleryClick = { nav.navigate(Routes.galleryDetail(entry.galleryItemId)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LeaderboardItem(
    rank: Int,
    entry: ChallengeEntry,
    onUserClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1) HeritageGold.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "#$rank",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (rank == 1) HeritageGold else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(50.dp),
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            ) {
                if (entry.userAvatar != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(entry.userAvatar)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.clickable { onUserClick() },
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${entry.voteCount} votes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = if (rank == 1) HeritageGold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: ChallengeEntry,
    onVoteClick: () -> Unit,
    onUserClick: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    Card(
        onClick = onGalleryClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            // User row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                ) {
                    if (entry.userAvatar != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(entry.userAvatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.clickable { onUserClick() },
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    entry.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Vote button and count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onVoteClick) {
                    Icon(
                        if (entry.isVoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Vote",
                        tint = if (entry.isVoted) HeritageGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    "${entry.voteCount} votes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun calculateDaysLeft(endDate: Long): Int {
    val now = System.currentTimeMillis()
    val diff = endDate - now
    return if (diff > 0) {
        TimeUnit.MILLISECONDS.toDays(diff).toInt()
    } else {
        0
    }
}
