@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FolderEntity
import com.example.data.LinkEntity

enum class MainTab {
    RECENT,
    FOLDERS,
    TAGS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LinkViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val links by viewModel.filteredLinks.collectAsStateWithLifecycle()
    val allLinks by viewModel.allLinks.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()

    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val unlockedFolderIds by viewModel.unlockedFolderIds.collectAsStateWithLifecycle()
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
    val showFoldersTab by viewModel.showFoldersTab.collectAsStateWithLifecycle()
    val showTagsTab by viewModel.showTagsTab.collectAsStateWithLifecycle()

    // Active bottom navigation tab selection
    var currentTab by remember { mutableStateOf(MainTab.RECENT) }

    LaunchedEffect(showFoldersTab, showTagsTab, currentTab) {
        if (!showFoldersTab && currentTab == MainTab.FOLDERS) {
            currentTab = MainTab.RECENT
        }
        if (!showTagsTab && currentTab == MainTab.TAGS) {
            currentTab = MainTab.RECENT
        }
    }

    // Auto-locking when leaving folders
    var previousFolderId by remember { mutableStateOf(selectedFolderId) }
    LaunchedEffect(selectedFolderId) {
        if (previousFolderId != selectedFolderId) {
            if (previousFolderId != 0L) {
                viewModel.lockFolder(previousFolderId)
            }
            previousFolderId = selectedFolderId
        }
    }

    // Auto-locking when switching tab pages
    LaunchedEffect(currentTab) {
        viewModel.lockAllFolders()
    }

    // Dialog trigger states
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<FolderEntity?>(null) }
    var linkToEdit by remember { mutableStateOf<LinkEntity?>(null) }

    // Lock authorization screen triggers
    var folderIdToUnlock by remember { mutableStateOf<Long?>(null) }
    var folderNameToUnlock by remember { mutableStateOf("") }
    var folderLockTypeToUnlock by remember { mutableStateOf("") }
    var folderLockValToUnlock by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header app brand and dynamic active tab indicator text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dynamic minimal branding cube matching the main app icon
                        val brandingShape = if (useRoundedCorners) RoundedCornerShape(10.dp) else RoundedCornerShape(0.dp)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, brandingShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "LinkVault Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(-45f)
                            )
                        }

                        Column {
                            Text(
                                text = "LinkVault",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Reset Session Locks with notification setup styled minimally
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (unlockedFolderIds.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.unlockedFolderIds.value = emptySet()
                                    Toast.makeText(context, "所有資料夾已重新上鎖 🔒", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock all folders",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // 1. RECENT Tab
                NavigationBarItem(
                    selected = currentTab == MainTab.RECENT,
                    onClick = { currentTab = MainTab.RECENT },
                    icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Recent") },
                    label = { Text("最近新增", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )

                // 2. FOLDERS Tab
                if (showFoldersTab) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.FOLDERS,
                        onClick = { currentTab = MainTab.FOLDERS },
                        icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = "Folders") },
                        label = { Text("資料夾", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // 3. ADD LINK Action Button
                NavigationBarItem(
                    selected = false,
                    onClick = { showAddLinkDialog = true },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add link",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = { Text("新增連結", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )

                // 4. TAGS Tab
                if (showTagsTab) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.TAGS,
                        onClick = { currentTab = MainTab.TAGS },
                        icon = { Icon(imageVector = Icons.Default.Label, contentDescription = "Tags") },
                        label = { Text("標籤", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // 5. SETTINGS Tab
                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { currentTab = MainTab.SETTINGS },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("設定", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.RECENT -> {
                    RecentTabContent(
                        allLinks = allLinks,
                        folders = folders,
                        context = context,
                        clipboardManager = clipboardManager,
                        viewModel = viewModel,
                        onEditLink = { target -> linkToEdit = target },
                        onDeleteLink = { target ->
                            viewModel.deleteLink(target)
                            Toast.makeText(context, "連結已刪除🗑️", Toast.LENGTH_SHORT).show()
                        },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { q -> viewModel.searchQuery.value = q },
                        onAddLinkClick = { showAddLinkDialog = true }
                    )
                }
                MainTab.FOLDERS -> {
                    FoldersTabContent(
                        folders = folders,
                        links = links,
                        tags = tags,
                        selectedFolderId = selectedFolderId,
                        searchQuery = searchQuery,
                        selectedTag = selectedTag,
                        unlockedFolderIds = unlockedFolderIds,
                        context = context,
                        clipboardManager = clipboardManager,
                        viewModel = viewModel,
                        onAddFolderDialog = { showAddFolderDialog = true },
                        onEditFolder = { target -> folderToEdit = target },
                        onEditLink = { target -> linkToEdit = target },
                        onDeleteLink = { target ->
                            viewModel.deleteLink(target)
                            Toast.makeText(context, "連結已刪除🗑️", Toast.LENGTH_SHORT).show()
                        },
                        onUnlockRequest = { id, name, type, value ->
                            folderIdToUnlock = id
                            folderNameToUnlock = name
                            folderLockTypeToUnlock = type
                            folderLockValToUnlock = value
                        },
                        onSearchQueryChange = { q -> viewModel.searchQuery.value = q },
                        onAddLinkClick = { showAddLinkDialog = true }
                    )
                }
                MainTab.TAGS -> {
                    TagsTabContent(
                        folders = folders,
                        allLinks = allLinks,
                        context = context,
                        clipboardManager = clipboardManager,
                        viewModel = viewModel,
                        unlockedFolderIds = unlockedFolderIds,
                        onUnlockRequest = { id, name, type, value ->
                            folderIdToUnlock = id
                            folderNameToUnlock = name
                            folderLockTypeToUnlock = type
                            folderLockValToUnlock = value
                        },
                        onEditLink = { target -> linkToEdit = target },
                        onDeleteLink = { target ->
                            viewModel.deleteLink(target)
                            Toast.makeText(context, "連結已刪除🗑️", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsTabContent(
                        viewModel = viewModel,
                        unlockedFolderIds = unlockedFolderIds,
                        onLockAllFolders = {
                            viewModel.unlockedFolderIds.value = emptySet()
                            Toast.makeText(context, "所有加密資料夾已重新進入安全上鎖狀態 🔒", Toast.LENGTH_SHORT).show()
                        },
                        onGenerateTestData = {
                            viewModel.generateTestData()
                            Toast.makeText(context, "已成功生成開發者測試用完整資料集！✨", Toast.LENGTH_SHORT).show()
                        },
                        context = context
                    )
                }
            }
        }
    }

    // --- Overlay Control Dialogs ---

    // 1. PIN / Pattern Authorization Dialog
    if (folderIdToUnlock != null) {
        LockUnlockDialog(
            folderName = folderNameToUnlock,
            lockType = folderLockTypeToUnlock,
            lockValue = folderLockValToUnlock,
            onDismiss = {
                folderIdToUnlock = null
            },
            onUnlockSuccess = {
                val fId = folderIdToUnlock
                if (fId != null) {
                    viewModel.unlockFolder(fId)
                    viewModel.selectFolder(fId)
                }
                folderIdToUnlock = null
            }
        )
    }

    // 2. Add / Edit Link Dialog
    if (showAddLinkDialog || linkToEdit != null) {
        AddEditLinkDialog(
            folders = folders,
            linkToEdit = linkToEdit,
            allTags = tags,
            defaultFolderId = selectedFolderId,
            onDismiss = {
                showAddLinkDialog = false
                linkToEdit = null
            },
            onSave = { folderId, title, url, note, tagsStr ->
                if (linkToEdit != null) {
                    val target = linkToEdit!!.copy(
                        folderId = folderId,
                        title = title,
                        url = url,
                        note = note,
                        tags = tagsStr
                    )
                    viewModel.updateLink(target)
                    Toast.makeText(context, "連結更新完成！✏️", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addLink(folderId, title, url, note, tagsStr)
                    Toast.makeText(context, "連結儲存成功！🎉", Toast.LENGTH_SHORT).show()
                }
                showAddLinkDialog = false
                linkToEdit = null
            }
        )
    }

    // 3. Create Folder Dialog
    if (showAddFolderDialog) {
        AddEditFolderDialog(
            folderToEdit = null,
            onDismiss = { showAddFolderDialog = false },
            onSave = { name, isLocked, lockType, lockValue ->
                viewModel.addFolder(name, isLocked, lockType, lockValue)
                Toast.makeText(context, "建立新資料夾：$name", Toast.LENGTH_SHORT).show()
                showAddFolderDialog = false
            }
        )
    }

    // 4. Edit/Update/Delete Folder Dialog (triggered by long-pressing folder capsule)
    if (folderToEdit != null) {
        AddEditFolderDialog(
            folderToEdit = folderToEdit,
            onDismiss = { folderToEdit = null },
            onSave = { name, isLocked, lockType, lockValue ->
                val updated = folderToEdit!!.copy(
                    name = name,
                    isLocked = isLocked,
                    lockType = lockType,
                    lockValue = lockValue
                )
                viewModel.updateFolder(updated)
                Toast.makeText(context, "資料夾已更新", Toast.LENGTH_SHORT).show()
                folderToEdit = null
            },
            onDelete = {
                viewModel.deleteFolder(folderToEdit!!)
                Toast.makeText(context, "資料夾與其連結皆已清除", Toast.LENGTH_SHORT).show()
                folderToEdit = null
            }
        )
    }
}

/**
 * Custom Folder Capsule supporting click & long click edit operations
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCapsule(
    name: String,
    isSelected: Boolean,
    isLocked: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    val borderStrokeModifier = if (isSelected) {
        Modifier
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(containerColor)
            .then(borderStrokeModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLocked) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = "Lock indicator",
                tint = if (isSelected) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(15.dp)
                    .padding(end = 4.dp)
            )
        } else {
            Text(
                text = "📂",
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Text(
            text = name,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/**
 * Elegant item card displaying complete details of saved hyperlink
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinkItemCard(
    link: LinkEntity,
    folderName: String,
    onOpenLink: (LinkEntity) -> Unit,
    onCopyLink: (LinkEntity) -> Unit,
    onEditLink: (LinkEntity) -> Unit,
    onDeleteLink: (LinkEntity) -> Unit,
    useRoundedCorners: Boolean = true
) {
    val cardShape = if (useRoundedCorners) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp)
    val badgeShape = if (useRoundedCorners) RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("link_card_${link.id}")
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Brand information Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Main Name (名稱)
                    Text(
                        text = link.title.ifEmpty { "未命名連結" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // URL in Clean Monospace typography
                    Text(
                        text = link.url,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary, // Clean digital blue (Primary)
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onOpenLink(link) }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Control actions placed in top-right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Open Web Link (no background, no text, styled like the others)
                    IconButton(
                        onClick = { onOpenLink(link) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Browse",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy URL
                    IconButton(
                        onClick = { onCopyLink(link) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy link",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Edit
                    IconButton(
                        onClick = { onEditLink(link) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit linkDetails",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = { onDeleteLink(link) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete link",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Dynamic Tags sub-row
            val tagList = link.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tagList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                // Clean wrapping list of tags
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tagList.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(badgeShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Optional custom bottom note row below subtle divider
            if (link.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "NOTE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = link.note,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
            }

            // Divider before actions list
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Bottom controls row showing only folder label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder placement location tag
                val folderBadgeShape = if (useRoundedCorners) RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp)
                Box(
                    modifier = Modifier
                        .clip(folderBadgeShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = folderName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/**
 * Locked Folder Screen display inside list area
 */
@Composable
fun LockedFolderMessage(
    folderName: String,
    onUnlockClicked: () -> Unit,
    onBackClicked: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Folder locked",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "此資料夾密碼保護中",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "您必須先進行安全認證，才能存取「$folderName」內的連結。",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onUnlockClicked,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("unlock_folder_button")
        ) {
            Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Unlock icon")
            Spacer(modifier = Modifier.width(6.dp))
            Text("安全密碼解鎖", fontWeight = FontWeight.Bold)
        }

        if (onBackClicked != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBackClicked,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("back_from_locked_folder_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(6.dp))
                Text("返回上一層", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Friendly Empty State Composable
 */
@Composable
fun EmptyLinksState(
    isSearchResult: Boolean,
    onCreateClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearchResult) Icons.Default.Search else Icons.Default.Link,
                contentDescription = "Empty list",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSearchResult) "找不到符合的連結" else "尚未儲存連結軌跡",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isSearchResult) "請更換關鍵字或選擇其他標籤再試一次" else "立即新增您的第一個超連結，安全收藏、快速開啟！",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (!isSearchResult) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onCreateClicked,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add now")
                Spacer(modifier = Modifier.width(4.dp))
                Text("新增第一個連結", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Dialog for adding or editing a hyperlink, featuring Name, URL, Category and SELECTABLE NOTE TEMPLATE.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLinkDialog(
    folders: List<FolderEntity>,
    linkToEdit: LinkEntity?,
    allTags: List<String>,
    defaultFolderId: Long = 0L,
    onDismiss: () -> Unit,
    onSave: (folderId: Long, title: String, url: String, note: String, tags: String) -> Unit
) {
    var title by remember { mutableStateOf(linkToEdit?.title ?: "") }
    var url by remember { mutableStateOf(linkToEdit?.url ?: "") }
    var note by remember { mutableStateOf(linkToEdit?.note ?: "") }
    var tagsStr by remember { mutableStateOf(linkToEdit?.tags ?: "") }

    var selectedFolderId by remember {
        mutableStateOf(
            linkToEdit?.folderId ?: if (defaultFolderId != 0L) defaultFolderId else (folders.firstOrNull()?.id ?: 0L)
        )
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val currentTags = remember(tagsStr) {
        tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    val onTagToggle: (String) -> Unit = { tag ->
        val updated = if (currentTags.contains(tag)) {
            currentTags - tag
        } else {
            currentTags + tag
        }
        tagsStr = updated.joinToString(", ")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (linkToEdit != null) "編輯連結收藏" else "安全加入新連結",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("連結名稱 (必填)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // URL Input
                item {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("超連結 URL (必填，例如 https://...)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Folders Category Picker
                item {
                    Text(
                        text = "歸屬資料夾：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val activeFolder = folders.find { it.id == selectedFolderId }
                    val activeFolderName = activeFolder?.name ?: "⚠️ 請先建立資料夾"

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = activeFolderName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            folders.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(f.name) },
                                    onClick = {
                                        selectedFolderId = f.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Custom Note Field
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("自訂備註說明 (選填)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Modern Tag Selection & Input
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "分類標籤：",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (allTags.isNotEmpty()) {
                            Text(
                                text = "快速點選現有標籤 (可複選)：",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                allTags.forEach { tag ->
                                    val isSelected = currentTags.contains(tag)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onTagToggle(tag) },
                                        label = { Text(tag) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tagsStr,
                            onValueChange = { tagsStr = it },
                            label = { Text("自訂或編輯標籤 (以半形逗點區隔)") },
                            placeholder = { Text("例如: 工具, 學習") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    item {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Final Panel Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("取消")
                        }

                        Button(
                            onClick = {
                                if (title.trim().isEmpty()) {
                                    errorMessage = "請填寫連結名稱！"
                                    return@Button
                                }
                                if (url.trim().isEmpty()) {
                                    errorMessage = "請填寫連結的 URL 網址！"
                                    return@Button
                                }
                                if (selectedFolderId == 0L) {
                                    errorMessage = "歸屬資料夾無效，請先建立資料夾"
                                    return@Button
                                }
                                errorMessage = ""
                                onSave(selectedFolderId, title.trim(), url.trim(), note.trim(), tagsStr.trim())
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("儲存連結")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to Create or Edit folders. Includes locking toggles (PIN code and Pattern lock configuration)
 */
@Composable
fun AddEditFolderDialog(
    folderToEdit: FolderEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, isLocked: Boolean, lockType: String, lockValue: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(folderToEdit?.name ?: "") }
    var isLocked by remember { mutableStateOf(folderToEdit?.isLocked ?: false) }
    var lockType by remember { mutableStateOf(folderToEdit?.lockType ?: "PIN") }
    var lockValue by remember { mutableStateOf(folderToEdit?.lockValue ?: "") }

    var showSetupLocker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (folderToEdit != null) "編輯資料夾設定" else "建立新分類資料夾",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("資料夾名稱 (例如: 📖 熱門閱讀)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Safe locker switches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔒 資料夾密碼鎖定",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isLocked) "已開啟 (類型: ${if (lockType == "PIN") "數字鎖" else "9點圖形鎖"})" else "未鎖定內容",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Switch(
                        checked = isLocked,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showSetupLocker = true
                            } else {
                                isLocked = false
                                lockValue = ""
                            }
                        }
                    )
                }

                // Locker details prompt
                if (isLocked && lockValue.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Configured indicator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "加密鑰匙已完成註冊！(${if (lockType == "PIN") "數字暗號" else "圖形連線"})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Drawer control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (folderToEdit != null && onDelete != null) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("刪除")
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("關閉")
                    }

                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                errorMessage = "請填寫資料夾名稱！"
                                return@Button
                            }
                            if (isLocked && lockValue.isEmpty()) {
                                errorMessage = "請先設定上鎖的密碼或圖形軌跡"
                                return@Button
                            }
                            errorMessage = ""
                            onSave(name.trim(), isLocked, lockType, lockValue)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("確認儲存")
                    }
                }
            }
        }
    }

    if (showSetupLocker) {
        LockSetupDialog(
            onDismiss = {
                if (lockValue.isEmpty()) {
                    isLocked = false
                }
                showSetupLocker = false
            },
            onLockConfigured = { type, value ->
                lockType = type
                lockValue = value
                isLocked = true
                showSetupLocker = false
            }
        )
    }
}

/**
 * Native utility support package functions for browser launchers
 */
private fun openBrowser(context: Context, url: String) {
    var rawUrl = url.trim()
    if (rawUrl.isEmpty()) return
    if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
        rawUrl = "https://$rawUrl"
    }
    try {
        val uri = android.net.Uri.parse(rawUrl)
        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        context.startActivity(browserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "無效或不支援的 URL 連結格式", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun RecentTabContent(
    allLinks: List<LinkEntity>,
    folders: List<FolderEntity>,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    viewModel: LinkViewModel,
    onEditLink: (LinkEntity) -> Unit,
    onDeleteLink: (LinkEntity) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddLinkClick: () -> Unit
) {
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()

    // 1. Get non-locked folders set
    val nonLockedFolderIds = remember(folders) {
        folders.filter { !it.isLocked }.map { it.id }.toSet()
    }

    // 2. Filter links locally for "RECENT (recent added, non-locked, matching search query)"
    val recentLinks = remember(allLinks, nonLockedFolderIds, searchQuery) {
        val q = searchQuery.trim().lowercase()
        allLinks.filter { link ->
            nonLockedFolderIds.contains(link.folderId) && (
                q.isEmpty() ||
                link.title.lowercase().contains(q) ||
                link.url.lowercase().contains(q) ||
                link.note.lowercase().contains(q) ||
                link.tags.lowercase().contains(q)
            )
        }.sortedByDescending { it.id } // Newest added links on top!
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search pill field specifically for Recent links
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("搜尋最近收藏的連結...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field_recent"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (recentLinks.isEmpty()) {
                EmptyLinksState(
                    isSearchResult = searchQuery.isNotEmpty(),
                    onCreateClicked = onAddLinkClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentLinks, key = { it.id }) { link ->
                        val folderObj = folders.find { it.id == link.folderId }
                        val folderLabel = folderObj?.name ?: "無分類"

                        LinkItemCard(
                            link = link,
                            folderName = folderLabel,
                            onOpenLink = { target -> openBrowser(context, target.url) },
                            onCopyLink = { target ->
                                clipboardManager.setText(AnnotatedString(target.url))
                                Toast.makeText(context, "已複製 URL 連結 📋", Toast.LENGTH_SHORT).show()
                            },
                            onEditLink = onEditLink,
                            onDeleteLink = onDeleteLink,
                            useRoundedCorners = useRoundedCorners
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FoldersTabContent(
    folders: List<FolderEntity>,
    links: List<LinkEntity>,
    tags: List<String>,
    selectedFolderId: Long,
    searchQuery: String,
    selectedTag: String?,
    unlockedFolderIds: Set<Long>,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    viewModel: LinkViewModel,
    onAddFolderDialog: () -> Unit,
    onEditFolder: (FolderEntity) -> Unit,
    onEditLink: (LinkEntity) -> Unit,
    onDeleteLink: (LinkEntity) -> Unit,
    onUnlockRequest: (folderId: Long, name: String, type: String, value: String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddLinkClick: () -> Unit
) {
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
    val shape16 = if (useRoundedCorners) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp)
    val shape12 = if (useRoundedCorners) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp)
    val shape8 = if (useRoundedCorners) RoundedCornerShape(8.dp) else RoundedCornerShape(0.dp)

    val allDbLinks by viewModel.allLinks.collectAsStateWithLifecycle()
    val folderTags = remember(allDbLinks, selectedFolderId) {
        if (selectedFolderId == 0L) {
            emptyList()
        } else {
            allDbLinks.filter { it.folderId == selectedFolderId }
                .flatMap { link ->
                    link.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }.distinct().sorted()
        }
    }

    if (selectedFolderId == 0L) {
        // --- 1. ROOT VIEW: Displays list of directories like computer file explorer ---
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📂 根目錄 (資料夾列表)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = onAddFolderDialog,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add folder",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新增資料夾", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📁 沒有任何資料夾", color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onAddFolderDialog) {
                            Text("新增第一個資料夾")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(folders) { folder ->
                        val isLocked = folder.isLocked
                        val isUnlocked = unlockedFolderIds.contains(folder.id)
                        val folderLinksCount = viewModel.allLinks.value.count { it.folderId == folder.id }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isLocked && !isUnlocked) {
                                            onUnlockRequest(folder.id, folder.name, folder.lockType, folder.lockValue)
                                        } else {
                                            viewModel.selectFolder(folder.id)
                                        }
                                    },
                                    onLongClick = { onEditFolder(folder) }
                                ),
                            shape = shape16,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Folder visual representation (computer directory icon style)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isLocked && !isUnlocked) "🔒" else "📁",
                                        fontSize = 22.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$folderLinksCount 個項目",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLocked) {
                                        Icon(
                                            imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = "🔒 Secure",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    IconButton(onClick = { onEditFolder(folder) }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Folder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- 2. INSIDE FOLDER VIEW: Drilled down view of the actual folder ---
        val currentFolder = folders.find { it.id == selectedFolderId }
        val isCurrentFolderLockedAndNotAuth = currentFolder != null &&
                currentFolder.isLocked &&
                !unlockedFolderIds.contains(currentFolder.id)

        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant Breadcrumbs and Back Button Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.selectFolder(0L) }, // Return to root directory
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Computer breadcrumb directory visual
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape8)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "根目錄 ➔ ${currentFolder?.name ?: "無分類"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (!isCurrentFolderLockedAndNotAuth) {
                // Search Bar specifically in folders tab to find folder items
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("搜尋此資料夾內的連結...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("search_field_folder"),
                    shape = shape16,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tag Slider (Only show tags active under current folder)
                if (folderTags.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { viewModel.selectTag(null) },
                                label = { Text("全部標籤") },
                                shape = shape12
                            )
                        }

                        items(folderTags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { viewModel.selectTag(tag) },
                                label = { Text("# $tag") },
                                shape = shape12,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }

            // Main display body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (isCurrentFolderLockedAndNotAuth) {
                    LockedFolderMessage(
                        folderName = currentFolder?.name ?: "已加密資料夾",
                        onUnlockClicked = {
                            onUnlockRequest(
                                currentFolder?.id ?: 0L,
                                currentFolder?.name ?: "已加密資料夾",
                                currentFolder?.lockType ?: "PIN",
                                currentFolder?.lockValue ?: ""
                            )
                        },
                        onBackClicked = { viewModel.selectFolder(0L) }
                    )
                } else if (links.isEmpty()) {
                    EmptyLinksState(
                        isSearchResult = searchQuery.isNotEmpty() || selectedTag != null,
                        onCreateClicked = onAddLinkClick
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("links_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(links, key = { it.id }) { link ->
                            val folderObj = folders.find { it.id == link.folderId }
                            val folderLabel = folderObj?.name ?: "無分類"

                            LinkItemCard(
                                link = link,
                                folderName = folderLabel,
                                onOpenLink = { target -> openBrowser(context, target.url) },
                                onCopyLink = { target ->
                                    clipboardManager.setText(AnnotatedString(target.url))
                                    Toast.makeText(context, "已複製 URL 連結 📋", Toast.LENGTH_SHORT).show()
                                },
                                onEditLink = onEditLink,
                                onDeleteLink = onDeleteLink,
                                useRoundedCorners = useRoundedCorners
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagsTabContent(
    folders: List<FolderEntity>,
    allLinks: List<LinkEntity>,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    viewModel: LinkViewModel,
    unlockedFolderIds: Set<Long>,
    onUnlockRequest: (folderId: Long, name: String, type: String, value: String) -> Unit,
    onEditLink: (LinkEntity) -> Unit,
    onDeleteLink: (LinkEntity) -> Unit
) {
    // Persistent tags collected from ViewModel state flow of the database table
    val allUniqueTags by viewModel.allTags.collectAsStateWithLifecycle()
    val useRoundedCorners by viewModel.useRoundedCorners.collectAsStateWithLifecycle()

    var activeTagSelected by remember { mutableStateOf<String?>(null) }
    var showAddTagDialog by remember { mutableStateOf(false) }

    // Initialize select first tag if nothing selected and tags are available
    LaunchedEffect(allUniqueTags) {
        if (activeTagSelected == null && allUniqueTags.isNotEmpty()) {
            activeTagSelected = allUniqueTags.first()
        }
    }

    val matchingLinks = remember(allLinks, activeTagSelected) {
        if (activeTagSelected == null) emptyList()
        else {
            allLinks.filter { link ->
                val linkTags = link.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                linkTags.contains(activeTagSelected)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header with dynamic Add Tag capability
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "點選要檢索的標籤分類：",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )

            TextButton(
                onClick = { showAddTagDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add tag",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("新增標籤", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (allUniqueTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "目前無任何自訂標籤，點擊右上角新增標籤，或在新增或編輯連結時設定標籤庫🏷️",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            // LazyHorizontalGrid with exactly 3 Rows, horizontally scrollable
            Box(modifier = Modifier.height(110.dp)) {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(allUniqueTags) { tag ->
                        val isSelected = activeTagSelected == tag
                        val shape = if (useRoundedCorners) RoundedCornerShape(10.dp) else RoundedCornerShape(0.dp)
                        Box(
                            modifier = Modifier
                                .clip(shape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                                .clickable { activeTagSelected = tag }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 11.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (matchingLinks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "該標籤下尚未包含任何連結", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(matchingLinks, key = { it.id }) { link ->
                            val folderObj = folders.find { it.id == link.folderId }
                            val isFolderLocked = folderObj?.isLocked == true
                            val isUnlocked = unlockedFolderIds.contains(link.folderId)

                            if (isFolderLocked && !isUnlocked) {
                                val shape = if (useRoundedCorners) RoundedCornerShape(16.dp) else RoundedCornerShape(0.dp)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            folderObj?.let {
                                                onUnlockRequest(it.id, it.name, it.lockType, it.lockValue)
                                            }
                                        }
                                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, shape),
                                    shape = shape,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = "🔒 受保護的加密連結",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "歸屬於密碼資料夾「${folderObj?.name}」，點擊進行解鎖安全存取",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                val folderLabel = folderObj?.name ?: "無分類"
                                LinkItemCard(
                                    link = link,
                                    folderName = folderLabel,
                                    onOpenLink = { target -> openBrowser(context, target.url) },
                                    onCopyLink = { target ->
                                        clipboardManager.setText(AnnotatedString(target.url))
                                        Toast.makeText(context, "已複製 URL 連結 📋", Toast.LENGTH_SHORT).show()
                                    },
                                    onEditLink = onEditLink,
                                    onDeleteLink = onDeleteLink,
                                    useRoundedCorners = useRoundedCorners
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dynamic Add Tag Dialog
    if (showAddTagDialog) {
        var tagNameInput by remember { mutableStateOf("") }
        val shape = if (useRoundedCorners) RoundedCornerShape(20.dp) else RoundedCornerShape(0.dp)
        val buttonShape = if (useRoundedCorners) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp)

        Dialog(onDismissRequest = { showAddTagDialog = false }) {
            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, shape)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "新增標籤",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tagNameInput,
                        onValueChange = { tagNameInput = it },
                        label = { Text("標籤名稱") },
                        placeholder = { Text("例如：工作、私房、AI") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = if (useRoundedCorners) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddTagDialog = false }) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (tagNameInput.isNotBlank()) {
                                    val trimmed = tagNameInput.trim()
                                    viewModel.insertTag(trimmed)
                                    activeTagSelected = trimmed
                                    Toast.makeText(context, "標籤 #$trimmed 已新增", Toast.LENGTH_SHORT).show()
                                }
                                showAddTagDialog = false
                            },
                            shape = buttonShape
                        ) {
                            Text("確認新增")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    viewModel: LinkViewModel,
    unlockedFolderIds: Set<Long>,
    onLockAllFolders: () -> Unit,
    onGenerateTestData: () -> Unit,
    context: Context
) {
    val useRoundedOnApp by viewModel.useRoundedCorners.collectAsStateWithLifecycle()
    val showFoldersSetting by viewModel.showFoldersTab.collectAsStateWithLifecycle()
    val showTagsSetting by viewModel.showTagsTab.collectAsStateWithLifecycle()
    val baseShape = if (useRoundedOnApp) RoundedCornerShape(20.dp) else RoundedCornerShape(0.dp)
 
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Angle shape design style changer (Replaces the private/security protector card!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, baseShape),
                shape = baseShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "📐 頁面編角視覺設計 (Interface Corner Style)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "切換應用程式中所有按鈕、超連結卡片和對話視窗的外觀邊角風格，隨心打造最愛的美感。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setRoundedCorners(true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (useRoundedOnApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (useRoundedOnApp) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("圓滑設計", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.setRoundedCorners(false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!useRoundedOnApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (!useRoundedOnApp) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("直角設計", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1.5. Navigation Tabs Visibility Settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, baseShape),
                shape = baseShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "📋 導覽分頁顯示設定 (Navigation Tab Visibility)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "自訂底部導覽列中「資料夾」與「標籤」分頁的顯示狀態。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setShowFoldersTab(!showFoldersSetting) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "顯示資料夾分頁",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "關閉後將不顯示下方「資料夾」選項",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = showFoldersSetting,
                            onCheckedChange = { viewModel.setShowFoldersTab(it) }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setShowTagsTab(!showTagsSetting) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "顯示標籤分頁",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "關閉後將不顯示下方「標籤」選項",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = showTagsSetting,
                            onCheckedChange = { viewModel.setShowTagsTab(it) }
                        )
                    }
                }
            }
        }

        // 2. Developer Mode section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), baseShape),
                shape = baseShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🛠️ 開發者測試模式 (Developer Mode)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "點擊下方按鈕將清理目前的資料庫，並為您一鍵生成內含多個安全防護、各類標籤與自訂筆記的分類測試資料，以供功能及視覺效果之展示。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onGenerateTestData,
                        shape = if (useRoundedOnApp) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = "Generate mock data")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "一鍵快速生成測試資料",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Technical credit panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, baseShape),
                shape = baseShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ℹ️ LinkVault 系統與美感規格",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "架構引擎", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(text = "Clean Jetpack Compose MVVM", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "本地持型機制", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(text = "SQLite Room DB with KSP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "美感風格專案", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(text = "Clean Minimalism (珊瑚珊瑚極簡橘)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
