package com.fabricfit.app.ui.screens.community

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fabricfit.app.R
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.ui.components.AspectRatioSelector
import com.fabricfit.app.ui.components.RichTextEditor
import com.fabricfit.app.ui.components.rememberMarkwon
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.util.ImageEditor
import com.fabricfit.app.ui.util.rememberImageEditorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel
class CreatePostViewModel(private val container: AppContainer) : ViewModel() {
    private val _fabrics = MutableStateFlow<List<Fabric>>(emptyList())
    val fabrics: StateFlow<List<Fabric>> = _fabrics.asStateFlow()

    private val _styles = MutableStateFlow<List<DressStyle>>(emptyList())
    val styles: StateFlow<List<DressStyle>> = _styles.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    init {
        loadFabrics()
        loadStyles()
    }

    private fun loadFabrics() {
        viewModelScope.launch {
            _fabrics.value = container.fabricAnalysis.getAll()
        }
    }

    private fun loadStyles() {
        viewModelScope.launch {
            _styles.value = container.dressStyles.all()
        }
    }

    suspend fun createPost(
        imageUri: String,
        title: String,
        description: String,
        tags: List<String>,
        fabricIds: List<Long>,
        styleId: String?,
    ): Boolean {
        _isSubmitting.value = true
        return try {
            container.community.createGalleryItem(
                imageUrl = imageUri,
                title = title,
                description = description,
                tags = tags,
                fabricIds = fabricIds,
                styleId = styleId,
            )
            true
        } catch (e: Exception) {
            false
        } finally {
            _isSubmitting.value = false
        }
    }
}

// Screen
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: CreatePostViewModel = appViewModel { c, ctx -> CreatePostViewModel(c) }
    val fabrics by vm.fabrics.collectAsStateWithLifecycle()
    val styles by vm.styles.collectAsStateWithLifecycle()
    val isSubmitting by vm.isSubmitting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Form state
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var newTag by remember { mutableStateOf("") }
    var selectedFabricIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var selectedStyleId by remember { mutableStateOf<String?>(null) }

    // Image pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // In production, save bitmap to file and get URI
        // For now, we'll just use gallery picker
    }

    // Image editing state
    val imageEditorState = rememberImageEditorState()
    var showAspectRatioSelector by remember { mutableStateOf(false) }
    
    // Rich text editor
    val markwon = rememberMarkwon()

    // uCrop launcher
    val uCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = ImageEditor.parseResult(result.resultCode, result.data)
        if (uri != null) {
            imageUri = uri
        }
        imageEditorState.cancelEditing()
    }

    // Dropdown states
    var fabricMenuExpanded by remember { mutableStateOf(false) }
    var styleMenuExpanded by remember { mutableStateOf(false) }

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
                "Create Post",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    if (imageUri != null && title.isNotBlank()) {
                        scope.launch {
                            val success = vm.createPost(
                                imageUri = imageUri.toString(),
                                title = title,
                                description = description,
                                tags = tags,
                                fabricIds = selectedFabricIds,
                                styleId = selectedStyleId,
                            )
                            if (success) {
                                nav.popBackStack()
                            }
                        }
                    }
                },
                enabled = imageUri != null && title.isNotBlank() && !isSubmitting,
            ) {
                Text(if (isSubmitting) "Posting..." else "Post")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Image picker
            item {
                Column {
                    Text(
                        "Photo *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))

                    if (imageUri == null) {
                        Card(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Tap to add photo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { cameraLauncher.launch(null) },
                                    ) {
                                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Camera")
                                    }
                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                    ) {
                                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Gallery")
                                    }
                                }
                            }
                        }
                    } else {
                        Column {
                            Box {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.5f)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                                // Edit button
                                IconButton(
                                    onClick = {
                                        imageUri?.let { uri ->
                                            imageEditorState.setOriginalUri(uri)
                                            showAspectRatioSelector = true
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.action_edit),
                                            modifier = Modifier.padding(8.dp),
                                        )
                                    }
                                }
                                // Remove button
                                IconButton(
                                    onClick = { 
                                        imageUri = null
                                        showAspectRatioSelector = false
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove photo",
                                            modifier = Modifier.padding(8.dp),
                                        )
                                    }
                                }
                            }

                            // Aspect ratio selector
                            if (showAspectRatioSelector) {
                                Spacer(Modifier.height(12.dp))
                                AspectRatioSelector(
                                    selectedRatio = imageEditorState.selectedAspectRatio,
                                    onRatioSelected = { ratio ->
                                        imageEditorState.setAspectRatio(ratio)
                                        // Launch uCrop with selected aspect ratio
                                        imageEditorState.originalUri?.let { uri ->
                                            val activity = context as? Activity
                                            if (activity != null) {
                                                ImageEditor.launchEditor(
                                                    activity = activity,
                                                    sourceUri = uri,
                                                    aspectRatio = ratio,
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Title
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    placeholder = { Text("Give your post a title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Description with rich text formatting
            item {
                RichTextEditor(
                    value = description,
                    onValueChange = { description = it },
                    markwon = markwon,
                    label = stringResource(R.string.description_label),
                    placeholder = stringResource(R.string.description_placeholder),
                    minLines = 5,
                    maxLines = 10,
                )
            }

            // Tags
            item {
                Column {
                    Text(
                        "Tags",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newTag,
                            onValueChange = { newTag = it },
                            placeholder = { Text("Add tag") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newTag.isNotBlank() && !tags.contains(newTag.trim())) {
                                    tags = tags + newTag.trim()
                                    newTag = ""
                                }
                            },
                            enabled = newTag.isNotBlank(),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add tag")
                        }
                    }

                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { tags = tags - tag },
                                    label = { Text("#$tag") },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Fabric linker
            item {
                Column {
                    Text(
                        "Fabrics Used",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = fabricMenuExpanded,
                        onExpandedChange = { fabricMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = if (selectedFabricIds.isEmpty()) {
                                ""
                            } else {
                                "${selectedFabricIds.size} fabric(s) selected"
                            },
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select fabrics from your wardrobe") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fabricMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = fabricMenuExpanded,
                            onDismissRequest = { fabricMenuExpanded = false },
                        ) {
                            fabrics.forEach { fabric ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                fabric.name ?: fabric.category.name,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (selectedFabricIds.contains(fabric.id)) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedFabricIds = if (selectedFabricIds.contains(fabric.id)) {
                                            selectedFabricIds - fabric.id
                                        } else {
                                            selectedFabricIds + fabric.id
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Style linker
            item {
                Column {
                    Text(
                        "Style Used",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = styleMenuExpanded,
                        onExpandedChange = { styleMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = styles.find { it.id == selectedStyleId }?.let { 
                                context.getString(it.titleKey) 
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select a style") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = styleMenuExpanded,
                            onDismissRequest = { styleMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedStyleId = null
                                    styleMenuExpanded = false
                                },
                            )
                            styles.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(context.getString(style.titleKey)) },
                                    onClick = {
                                        selectedStyleId = style.id
                                        styleMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Preview
            item {
                Column {
                    Text(
                        "Preview",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (imageUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            if (title.isNotBlank()) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (description.isNotBlank()) {
                                Text(
                                    description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                )
                            }
                            if (tags.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    tags.joinToString(" ") { "#$it" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
