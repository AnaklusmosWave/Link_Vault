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

    // Active bottom navigation tab selection
    var currentTab by remember { mutableStateOf(MainTab.RECENT) }

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
                        // Dynamic minimal branding cube
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "L",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
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
                NavigationBarItem(
                    selected = currentTab == MainTab.FOLDERS,
                    onClick = { currentTab = MainTab.FOLDERS },
                    icon = { Icon(imageVector = Icons.Default.Folder, contentDescription = "Folders") },
                    label = { Text("資料夾", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )

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
                NavigationBarItem(
                    selected = currentTab == MainTab.TAGS,
                    onClick = { currentTab = MainTab.TAGS },
                    icon = { Icon(imageVector = Icons.Default.Label, contentDescription = "Tags") },
                    label = { Text("標籤", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )

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
                        onSearchQueryChange = { q -> viewModel.searchQuery.value = q }
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
                        onSearchQueryChange = { q -> viewModel.searchQuery.value = q }
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
                        unlockedFolderIds = unlockedFolderIds,
                        onLockAllFolders = {
                            viewModel.unlockedFolderIds.value = emptySet()
                            Toast.makeText(context, "所有加密資料夾已重新進入安全上鎖狀態 🔒", Toast.LENGTH_SHORT).show()
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
    onDeleteLink: (LinkEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("link_card_${link.id}")
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
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
            // Header Top line: Metadata tags / Selectable Note highlight Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary tag marker or icon placeholder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🏷️ CATEGORY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Selectable Note Template Badge
                if (link.note.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = link.note,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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

                // Beautiful round decorative symbol badge on the right matching HTML mockup cards
                val iconEmoji = remember(link.title, link.tags, link.note) {
                    val combined = "${link.title} ${link.tags} ${link.note}".lowercase()
                    when {
                        combined.contains("design") || combined.contains("figma") || combined.contains("ui") || combined.contains("ux") || combined.contains("設計") || combined.contains("美學") -> "🎨"
                        combined.contains("dev") || combined.contains("code") || combined.contains("git") || combined.contains("github") || combined.contains("開發") || combined.contains("程式") -> "💻"
                        combined.contains("work") || combined.contains("office") || combined.contains("job") || combined.contains("工作") || combined.contains("任務") -> "💼"
                        combined.contains("study") || combined.contains("learn") || combined.contains("school") || combined.contains("學習") || combined.contains("讀") || combined.contains("教學") -> "🎓"
                        combined.contains("shop") || combined.contains("buy") || combined.contains("購物") || combined.contains("買") -> "🛍️"
                        combined.contains("video") || combined.contains("movie") || combined.contains("youtube") || combined.contains("影") || combined.contains("片") -> "🎬"
                        combined.contains("music") || combined.contains("song") || combined.contains("音") || combined.contains("歌") -> "🎵"
                        combined.contains("security") || combined.contains("private") || combined.contains("lock") || combined.contains("密") -> "🔒"
                        else -> "🔗"
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconEmoji, fontSize = 16.sp)
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
                                .clip(RoundedCornerShape(8.dp))
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

            // Bottom controls row: folder name label (left) & utility actions buttons (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder placement location tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
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

                // Control actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                            modifier = Modifier.size(14.dp)
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
                            modifier = Modifier.size(14.dp)
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
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Open Web Link
                    Button(
                        onClick = { onOpenLink(link) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Browse",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("開啟", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
    onUnlockClicked: () -> Unit
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
    onDismiss: () -> Unit,
    onSave: (folderId: Long, title: String, url: String, note: String, tags: String) -> Unit
) {
    var title by remember { mutableStateOf(linkToEdit?.title ?: "") }
    var url by remember { mutableStateOf(linkToEdit?.url ?: "") }
    var note by remember { mutableStateOf(linkToEdit?.note ?: "") }
    var tagsStr by remember { mutableStateOf(linkToEdit?.tags ?: "") }

    var selectedFolderId by remember {
        mutableStateOf(linkToEdit?.folderId ?: folders.firstOrNull()?.id ?: 0L)
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Predefined selectable note options
    val notePresets = listOf(
        "★ 重要珍藏",
        "⏳ 待讀教學",
        "💡 靈感筆記",
        "💼 工作任務",
        "🛍️ 購物口袋",
        "🏠 個人生活",
        "✨ 優質推薦"
    )

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

                // Selectable Note Field (選擇式註記)
                item {
                    Text(
                        text = "選擇式備註註記：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Flow of optional notes
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        notePresets.forEach { preset ->
                            val isSelected = note == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        // Auto-paste or clear
                                        note = if (isSelected) "" else preset
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("自訂備註說明...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Tags input
                item {
                    OutlinedTextField(
                        value = tagsStr,
                        onValueChange = { tagsStr = it },
                        label = { Text("標籤分類 (以半形逗點區分，如: 旅遊,工具)") },
                        placeholder = { Text("開發,學習,書籤") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
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
    onSearchQueryChange: (String) -> Unit
) {
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

        Text(
            text = "LATEST ACTIVITY 軌跡",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )
        Text(
            text = "最近新增收藏 (不含已加密安全內容)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 4.dp)
        )

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
                    onCreateClicked = {}
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentLinks, key = { it.id }) { link ->
                        val folderObj = folders.find { it.id == link.folderId }
                        val folderLabel = folderObj?.name ?: "未分類"

                        LinkItemCard(
                            link = link,
                            folderName = folderLabel,
                            onOpenLink = { target -> openBrowser(context, target.url) },
                            onCopyLink = { target ->
                                clipboardManager.setText(AnnotatedString(target.url))
                                Toast.makeText(context, "已複製 URL 連結 📋", Toast.LENGTH_SHORT).show()
                            },
                            onEditLink = onEditLink,
                            onDeleteLink = onDeleteLink
                        )
                    }
                }
            }
        }
    }
}

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
    onSearchQueryChange: (String) -> Unit
) {
    val currentFolder = folders.find { it.id == selectedFolderId }
    val isCurrentFolderLockedAndNotAuth = currentFolder != null &&
            currentFolder.isLocked &&
            !unlockedFolderIds.contains(currentFolder.id)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COLLECTIONS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            TextButton(
                onClick = onAddFolderDialog,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add folder",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("新增分類", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
        }

        // Slider of folders
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val isSelected = selectedFolderId == 0L
                FolderCapsule(
                    name = "📦 全部連結",
                    isSelected = isSelected,
                    isLocked = false,
                    isUnlocked = true,
                    onClick = { viewModel.selectFolder(0L) },
                    onLongClick = {}
                )
            }

            items(folders) { folder ->
                val isSelected = selectedFolderId == folder.id
                val isLocked = folder.isLocked
                val isUnlocked = unlockedFolderIds.contains(folder.id)

                FolderCapsule(
                    name = folder.name,
                    isSelected = isSelected,
                    isLocked = isLocked,
                    isUnlocked = isUnlocked,
                    onClick = {
                        if (isLocked && !isUnlocked) {
                            onUnlockRequest(folder.id, folder.name, folder.lockType, folder.lockValue)
                        } else {
                            viewModel.selectFolder(folder.id)
                        }
                    },
                    onLongClick = { onEditFolder(folder) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            shape = RoundedCornerShape(16.dp),
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
        if (tags.isNotEmpty()) {
            Text(
                text = "TAG FILTERS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

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
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(tags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { viewModel.selectTag(tag) },
                        label = { Text("# $tag") },
                        shape = RoundedCornerShape(12.dp),
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
                    }
                )
            } else if (links.isEmpty()) {
                EmptyLinksState(
                    isSearchResult = searchQuery.isNotEmpty() || selectedTag != null,
                    onCreateClicked = {}
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("links_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "COLLECTION ENTRIES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    items(links, key = { it.id }) { link ->
                        val folderObj = folders.find { it.id == link.folderId }
                        val folderLabel = folderObj?.name ?: "未分類"

                        LinkItemCard(
                            link = link,
                            folderName = folderLabel,
                            onOpenLink = { target -> openBrowser(context, target.url) },
                            onCopyLink = { target ->
                                clipboardManager.setText(AnnotatedString(target.url))
                                Toast.makeText(context, "已複製 URL 連結 📋", Toast.LENGTH_SHORT).show()
                            },
                            onEditLink = onEditLink,
                            onDeleteLink = onDeleteLink
                        )
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
    // Gather all unique tags from all links
    val allUniqueTags = remember(allLinks) {
        allLinks.flatMap { link ->
            link.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }.distinct().sorted()
    }

    var activeTagSelected by remember { mutableStateOf<String?>(null) }

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

        Text(
            text = "TAG MATRIX 標籤庫",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )
        Text(
            text = "多維度標籤聚合檢索",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (allUniqueTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "目前無任何自訂標籤，可以在新增或編輯連結時設定標籤庫🏷️",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            // FlowRow of Tags
            Text(
                text = "點選要檢索的標籤分類：",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                allUniqueTags.forEach { tag ->
                    val isSelected = activeTagSelected == tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable { activeTagSelected = tag }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

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
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            folderObj?.let {
                                                onUnlockRequest(it.id, it.name, it.lockType, it.lockValue)
                                            }
                                        }
                                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
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
                                val folderLabel = folderObj?.name ?: "未分類"
                                LinkItemCard(
                                    link = link,
                                    folderName = folderLabel,
                                    onOpenLink = { target -> openBrowser(context, target.url) },
                                    onCopyLink = { target ->
                                        clipboardManager.setText(AnnotatedString(target.url))
                                        Toast.makeText(context, "已複製 URL 連結 📋", Toast.LENGTH_SHORT).show()
                                    },
                                    onEditLink = onEditLink,
                                    onDeleteLink = onDeleteLink
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    unlockedFolderIds: Set<Long>,
    onLockAllFolders: () -> Unit,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "SYSTEM SETTINGS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )
            Text(
                text = "LinkVault 系統設定與美學說明",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // 2. Security reset controller
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🔐 安全隱私保護設定",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "您可以在【資料夾】頁籤上，長按任何歸屬分類資料夾，自訂四位數密碼(PIN)或是極簡圖形鎖扣。受鎖定保護的資料夾連結，在進行安全解鎖驗證之前，均不會暴露於【最近新增】及【標籤】檢索視窗中，高規格防護，保障個人隱私軌跡。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (unlockedFolderIds.isEmpty()) {
                                Toast.makeText(context, "所有加密資料夾目前皆處於安全上鎖狀態！🔒", Toast.LENGTH_SHORT).show()
                            } else {
                                onLockAllFolders()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock all")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (unlockedFolderIds.isNotEmpty()) "立刻一鍵重新上鎖 ${unlockedFolderIds.size} 個資料夾" else "所有資料夾目前皆處於安全上鎖狀態 🔒",
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
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
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
