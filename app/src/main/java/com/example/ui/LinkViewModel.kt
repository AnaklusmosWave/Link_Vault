package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FolderEntity
import com.example.data.LinkEntity
import com.example.data.LinkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LinkViewModel(private val repository: LinkRepository) : ViewModel() {

    val folders: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLinks: StateFlow<List<LinkEntity>> = repository.allLinks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current filters
    val selectedFolderId = MutableStateFlow<Long>(0) // 0 means "All" / "全部"
    val searchQuery = MutableStateFlow("")
    val selectedTag = MutableStateFlow<String?>(null)

    // Set of unlocked folder IDs for the current session
    val unlockedFolderIds = MutableStateFlow<Set<Long>>(emptySet())

    // Filtered links shown in the UI
    val filteredLinks: StateFlow<List<LinkEntity>> = combine(
        allLinks,
        selectedFolderId,
        searchQuery,
        selectedTag
    ) { links, folderId, query, tag ->
        var result = links

        // Filter by Folder
        if (folderId != 0L) {
            result = result.filter { it.folderId == folderId }
        }

        // Filter by Tag
        if (tag != null) {
            result = result.filter { link ->
                val linkTags = link.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                linkTags.contains(tag)
            }
        }

        // Filter by Search Query
        if (query.isNotEmpty()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.url.lowercase().contains(q) ||
                it.note.lowercase().contains(q) ||
                it.tags.lowercase().contains(q)
            }
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic tag list computed from ALL links or current visible links
    val allTags: StateFlow<List<String>> = allLinks
        .combine(selectedFolderId) { links, folderId ->
            val applicableLinks = if (folderId != 0L) {
                links.filter { it.folderId == folderId }
            } else {
                links
            }
            applicableLinks.flatMap { link ->
                link.tags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }.distinct().sorted()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initialize default data if DB is empty
        viewModelScope.launch {
            repository.allFolders.collect { list ->
                if (list.isEmpty()) {
                    setupInitialData()
                }
            }
        }
    }

    private suspend fun setupInitialData() {
        // Create pre-set folders
        val uncategorizedId = repository.insertFolder(
            FolderEntity(name = "📂 無分類", isLocked = false)
        )
        val workId = repository.insertFolder(
            FolderEntity(name = "🏢 工作專案", isLocked = false)
        )
        val learnId = repository.insertFolder(
            FolderEntity(name = "📖 閱讀學習", isLocked = false)
        )
        val entertainmentId = repository.insertFolder(
            FolderEntity(name = "🎨 趣味休閒", isLocked = false)
        )
        val financeId = repository.insertFolder(
            FolderEntity(
                name = "💰 金融財務 (已加密)",
                isLocked = true,
                lockType = "PIN",
                lockValue = "1234"
            )
        )

        // Insert some standard default links
        repository.insertLink(
            LinkEntity(
                folderId = workId,
                title = "Android Developer Main",
                url = "https://developer.android.com",
                note = "★ 重要官方開發文件",
                tags = "coding,android,dev"
            )
        )
        repository.insertLink(
            LinkEntity(
                folderId = learnId,
                title = "Jetpack Compose Guides",
                url = "https://developer.android.com/compose",
                note = "💡 最新的UI框架教學",
                tags = "compose,learn,ui"
            )
        )
        repository.insertLink(
            LinkEntity(
                folderId = entertainmentId,
                title = "Material Design Icons",
                url = "https://fonts.google.com/icons",
                note = "🎨 豐富的前端向量圖示庫",
                tags = "design,icon"
            )
        )
        repository.insertLink(
            LinkEntity(
                folderId = financeId,
                title = "Google Finance Stock Search",
                url = "https://www.google.com/finance",
                note = "📈 每週美股個股追蹤",
                tags = "money,finance,stock"
            )
        )
    }

    // --- Actions ---

    fun selectTag(tag: String?) {
        selectedTag.value = tag
    }

    fun selectFolder(folderId: Long) {
        selectedFolderId.value = folderId
        selectedTag.value = null // reset tag view on folder switch
    }

    fun unlockFolder(folderId: Long) {
        unlockedFolderIds.value = unlockedFolderIds.value + folderId
    }

    fun lockFolder(folderId: Long) {
        unlockedFolderIds.value = unlockedFolderIds.value - folderId
    }

    fun lockAllFolders() {
        unlockedFolderIds.value = emptySet()
    }

    fun addFolder(name: String, isLocked: Boolean, lockType: String, lockValue: String) {
        viewModelScope.launch {
            repository.insertFolder(
                FolderEntity(
                    name = name,
                    isLocked = isLocked,
                    lockType = lockType,
                    lockValue = lockValue
                )
            )
        }
    }

    fun updateFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder)
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            if (selectedFolderId.value == folder.id) {
                selectedFolderId.value = 0L // reset selection
            }
            repository.deleteFolder(folder)
        }
    }

    fun addLink(folderId: Long, title: String, url: String, note: String, tags: String) {
        viewModelScope.launch {
            repository.insertLink(
                LinkEntity(
                    folderId = folderId,
                    title = title,
                    url = url,
                    note = note,
                    tags = tags
                )
            )
        }
    }

    fun updateLink(link: LinkEntity) {
        viewModelScope.launch {
            repository.updateLink(link)
        }
    }

    fun deleteLink(link: LinkEntity) {
        viewModelScope.launch {
            repository.deleteLink(link)
        }
    }
}

// Factory provider
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LinkViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            val repository = LinkRepository(db.linkDao())
            @Suppress("UNCHECKED_CAST")
            return LinkViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
