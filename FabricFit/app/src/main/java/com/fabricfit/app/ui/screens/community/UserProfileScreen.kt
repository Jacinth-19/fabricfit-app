package com.fabricfit.app.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.fabricfit.app.domain.model.BlockedUser
import com.fabricfit.app.domain.model.CommunityUser
import com.fabricfit.app.domain.model.GalleryItem
import com.fabricfit.app.ui.components.EnhancedEmptyState
import com.fabricfit.app.ui.components.ShimmerGrid
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel
class UserProfileViewModel(
    private val container: AppContainer,
    private val userId: String,
) : ViewModel() {
    private val _user = MutableStateFlow<CommunityUser?>(null)
    val user: StateFlow<CommunityUser?> = _user.asStateFlow()

    private val _galleryItems = MutableStateFlow<List<GalleryItem>>(emptyList())
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()

    init {
        loadUser()
        loadGallery()
        checkIfBlocked()
    }

    private fun checkIfBlocked() {
        viewModelScope.launch {
            _isBlocked.value = container.blockedUsers.isUserBlocked(userId)
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            _user.value = container.community.getUser(userId)
            _isLoading.value = false
        }
    }

    private fun loadGallery() {
        viewModelScope.launch {
            container.community.observeGalleryByUser(userId).collect { items ->
                _galleryItems.value = items
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val currentUser = _user.value ?: return@launch
            if (currentUser.isFollowing) {
                container.community.unfollowUser(userId)
            } else {
                container.community.followUser(userId)
            }
            _user.value = container.community.getUser(userId)
        }
    }

    fun toggleBlock() {
        viewModelScope.launch {
            val currentUser = _user.value ?: return@launch
            if (_isBlocked.value) {
                // Unblock
                container.blockedUsers.unblockUser(userId)
                _isBlocked.value = false
            } else {
                // Block
                container.blockedUsers.blockUser(
                    com.fabricfit.app.domain.model.BlockedUser(
                        userId = userId,
                        username = currentUser.username,
                        displayName = currentUser.displayName,
                        avatarUrl = currentUser.avatarUrl,
                    )
                )
                _isBlocked.value = true
            }
        }
    }
}

// Screen
@Composable
fun UserProfileScreen(nav: NavHostController, padding: PaddingValues, userId: String) {
    val vm: UserProfileViewModel = appViewModel { c, ctx -> UserProfileViewModel(c, userId) }
    val user by vm.user.collectAsStateWithLifecycle()
    val galleryItems by vm.galleryItems.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val isBlocked by vm.isBlocked.collectAsStateWithLifecycle()
    
    var showMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

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
                user?.username ?: stringResource(R.string.user_profile),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            
            // More options menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                if (isBlocked) stringResource(R.string.action_unblock) 
                                else stringResource(R.string.action_block)
                            ) 
                        },
                        onClick = {
                            showMenu = false
                            if (isBlocked) {
                                vm.toggleBlock()
                            } else {
                                showBlockDialog = true
                            }
                        },
                    )
                }
            }
        }
        
        // Block confirmation dialog
        if (showBlockDialog && user != null) {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title = { Text(stringResource(R.string.block_confirm_title)) },
                text = { Text(stringResource(R.string.block_confirm_message, user!!.displayName)) },
                confirmButton = {
                    Button(onClick = {
                        vm.toggleBlock()
                        showBlockDialog = false
                    }) {
                        Text(stringResource(R.string.action_block))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
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
        } else if (user == null) {
            EnhancedEmptyState(
                illustration = R.drawable.illust_empty_activity,
                title = R.string.community_empty_title,
                body = R.string.user_profile_not_found,
                actionLabel = R.string.cd_back,
                onAction = { nav.popBackStack() },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // Profile header spanning all columns
                item(span = { GridItemSpan(3) }) {
                    ProfileHeader(
                        user = user!!,
                        onFollowClick = { vm.toggleFollow() },
                    )
                }

                // Gallery grid
                items(galleryItems, key = { it.id }) { item ->
                    GalleryGridItem(
                        item = item,
                        onClick = { nav.navigate(Routes.galleryDetail(item.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: CommunityUser,
    onFollowClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(120.dp),
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(60.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Display name
        Text(
            user.displayName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        // Username
        Text(
            "@${user.username}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        // Bio
        if (user.bio.isNotBlank()) {
            Text(
                user.bio,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(16.dp))
        }

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(
                count = user.galleryCount,
                label = stringResource(R.string.community_posts),
            )
            StatItem(
                count = user.followerCount,
                label = stringResource(R.string.community_followers),
            )
            StatItem(
                count = user.followingCount,
                label = stringResource(R.string.community_following),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Follow/Unfollow button
        if (user.isFollowing) {
            OutlinedButton(
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
            ) {
                Text(stringResource(R.string.action_unfollow))
            }
        } else {
            Button(
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(stringResource(R.string.action_follow))
            }
        }

        Spacer(Modifier.height(24.dp))

        // Divider
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        ) {}
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GalleryGridItem(
    item: GalleryItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(1.dp)
            .aspectRatio(1f),
        shape = RoundedCornerShape(0.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}
