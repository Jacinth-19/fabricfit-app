package com.fabricfit.app.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.fabricfit.app.domain.model.Comment
import com.fabricfit.app.domain.model.GalleryItem
import com.fabricfit.app.ui.components.ShimmerGrid
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.theme.HeritageGold
import com.fabricfit.app.ui.util.shareImagePath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel
class GalleryDetailViewModel(
    private val container: AppContainer,
    private val itemId: String,
) : ViewModel() {
    private val _item = MutableStateFlow<GalleryItem?>(null)
    val item: StateFlow<GalleryItem?> = _item.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadItem()
        loadComments()
    }

    private fun loadItem() {
        viewModelScope.launch {
            _item.value = container.community.getGalleryItem(itemId)
            _isLoading.value = false
        }
    }

    private fun loadComments() {
        viewModelScope.launch {
            container.community.observeComments(itemId).collect { comments ->
                _comments.value = comments
            }
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            container.community.toggleLike(itemId)
            _item.value = container.community.getGalleryItem(itemId)
        }
    }

    fun addComment(text: String) {
        viewModelScope.launch {
            container.community.addComment(itemId, text)
        }
    }
}

// Screen
@Composable
fun GalleryDetailScreen(nav: NavHostController, padding: PaddingValues, itemId: String) {
    val vm: GalleryDetailViewModel = appViewModel { c, ctx -> GalleryDetailViewModel(c, itemId) }
    val item by vm.item.collectAsStateWithLifecycle()
    val comments by vm.comments.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                stringResource(R.string.gallery_detail),
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
        } else if (item == null) {
            Text(
                "Item not found",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // Full image
                item {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item!!.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item!!.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }

                // User info and actions
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // User row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate(Routes.userProfile(item!!.userId)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(48.dp),
                            ) {
                                if (item!!.userAvatar != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(item!!.userAvatar)
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
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item!!.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    item!!.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { vm.toggleLike() }) {
                                Icon(
                                    if (item!!.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = stringResource(R.string.action_like),
                                    tint = if (item!!.isLiked) HeritageGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                stringResource(R.string.likes_count, item!!.likeCount),
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            Spacer(Modifier.width(24.dp))

                            Icon(
                                Icons.Filled.Comment,
                                contentDescription = stringResource(R.string.action_comment),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.comments_count, item!!.commentCount),
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            Spacer(Modifier.weight(1f))

                            IconButton(onClick = {
                                context.shareImagePath(item!!.imageUrl, item!!.title)
                            }) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = stringResource(R.string.action_share),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Description
                        if (item!!.description.isNotBlank()) {
                            Text(
                                item!!.description,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        // Tags
                        if (item!!.tags.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item!!.tags.take(5).forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            "#$tag",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // Divider
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        ) {}
                    }
                }

                // Comments section
                item {
                    Text(
                        stringResource(R.string.comments_count, comments.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                if (comments.isEmpty()) {
                    item {
                        Text(
                            "No comments yet. Be the first to comment!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        CommentItem(comment = comment)
                    }
                }
            }

            // Add comment bar at bottom
            CommentInput(onSubmit = { vm.addComment(it) })
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(36.dp),
        ) {
            if (comment.userAvatar != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(comment.userAvatar)
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
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                comment.username,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                comment.text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CommentInput(onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.add_comment)) },
                modifier = Modifier.weight(1f),
                maxLines = 3,
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSubmit(text.trim())
                        text = ""
                    }
                },
                enabled = text.isNotBlank(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.post_comment),
                    tint = if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
