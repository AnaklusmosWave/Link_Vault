package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FolderEntity
import com.example.data.LinkEntity
import com.example.data.LinkRepository
import com.example.data.TagEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LinkViewModel(private val repository: LinkRepository, private val context: Context) : ViewModel() {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    val useRoundedCorners = MutableStateFlow(prefs.getBoolean("use_rounded_corners", true))
    val showFoldersTab = MutableStateFlow(prefs.getBoolean("show_folders_tab", true))
    val showTagsTab = MutableStateFlow(prefs.getBoolean("show_tags_tab", true))

    fun setRoundedCorners(enabled: Boolean) {
        useRoundedCorners.value = enabled
        prefs.edit().putBoolean("use_rounded_corners", enabled).apply()
    }

    fun setShowFoldersTab(enabled: Boolean) {
        showFoldersTab.value = enabled
        prefs.edit().putBoolean("show_folders_tab", enabled).apply()
    }

    fun setShowTagsTab(enabled: Boolean) {
        showTagsTab.value = enabled
        prefs.edit().putBoolean("show_tags_tab", enabled).apply()
    }

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

    // Persistent tag list from database Tag table
    val allTags: StateFlow<List<String>> = repository.allTags
        .map { tagEntities ->
            tagEntities.map { it.name }.distinct().sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initialize default data if DB is empty or ensure '無分類' is present once on app startup
        viewModelScope.launch {
            val list = repository.allFolders.first()
            if (list.isEmpty()) {
                setupInitialData()
            } else {
                if (list.none { it.name == "無分類" }) {
                    repository.insertFolder(FolderEntity(name = "無分類", isLocked = false))
                }
            }
        }
    }

    private suspend fun setupInitialData() {
        // Pre-insert default tags so they are always in the database
        val defaultTagsList = listOf(
            "coding", "android", "dev", "compose", "learn",
            "ui", "design", "icon", "money", "finance", "stock"
        )
        defaultTagsList.forEach { tagName ->
            repository.insertTag(TagEntity(name = tagName))
        }

        // Create pre-set folders
        val uncategorizedId = repository.insertFolder(
            FolderEntity(name = "無分類", isLocked = false)
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
            // Save tags to avoid deletion
            tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { tag ->
                    repository.insertTag(TagEntity(name = tag))
                }
        }
    }

    fun updateLink(link: LinkEntity) {
        viewModelScope.launch {
            repository.updateLink(link)
            // Save tags to avoid deletion
            link.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { tag ->
                    repository.insertTag(TagEntity(name = tag))
                }
        }
    }

    fun deleteLink(link: LinkEntity) {
        viewModelScope.launch {
            repository.deleteLink(link)
        }
    }

    fun generateTestData() {
        viewModelScope.launch {
            repository.clearAllData()

            // Pre-seed diverse popular tags
            val tagList = listOf(
                "AI", "Android", "Design", "Finance", "News", "Shopping", "Tech", "Work", "Life", "Idea"
            )
            tagList.forEach { tag ->
                repository.insertTag(TagEntity(name = tag))
            }

            // Create custom folders
            val uncategorizedId = repository.insertFolder(FolderEntity(name = "無分類", isLocked = false))
            val techId = repository.insertFolder(FolderEntity(name = "💻 技術研討", isLocked = false))
            val designId = repository.insertFolder(FolderEntity(name = "🎨 設計靈感", isLocked = false))
            val lifeId = repository.insertFolder(FolderEntity(name = "🏠 日常生活", isLocked = false))
            val secretFinanceId = repository.insertFolder(FolderEntity(
                name = "🔒 機密財經",
                isLocked = true,
                lockType = "PIN",
                lockValue = "9999"
            ))

            // Insert matching links with high quality notes and descriptive tags
            repository.insertLink(LinkEntity(
                folderId = techId,
                title = "Google AI Studio",
                url = "https://ai.google.dev/aistudio",
                note = "★ AI 助理及原型設計入口",
                tags = "AI,Tech"
            ))
            repository.insertLink(LinkEntity(
                folderId = techId,
                title = "Kotlin Coding Lang",
                url = "https://kotlinlang.org",
                note = "⏳ 電腦程式語言官方網站",
                tags = "Android,Tech"
            ))
            repository.insertLink(LinkEntity(
                folderId = designId,
                title = "Material 3 Guideline",
                url = "https://m3.material.io",
                note = "💡 最齊全的 UI 排版色彩規格",
                tags = "Design,Tech"
            ))
            repository.insertLink(LinkEntity(
                folderId = designId,
                title = "Dribbble Design Hub",
                url = "https://dribbble.com",
                note = "🎨 優質網頁及行動端靈感集散地",
                tags = "Design,Idea"
            ))
            repository.insertLink(LinkEntity(
                folderId = lifeId,
                title = "Yahoo 奇摩新聞",
                url = "https://tw.news.yahoo.com",
                note = "🏠 每日焦點時事快報",
                tags = "News,Life"
            ))
            repository.insertLink(LinkEntity(
                folderId = lifeId,
                title = "PChome 24h 購物",
                url = "https://shopping.pchome.com.tw",
                note = "🛍️ 買 3C 及日用品專用口袋",
                tags = "Shopping,Life"
            ))
            repository.insertLink(LinkEntity(
                folderId = secretFinanceId,
                title = "Yahoo 股市個股",
                url = "https://tw.stock.yahoo.com",
                note = "📈 個人投資追蹤",
                tags = "Finance"
            ))
            repository.insertLink(LinkEntity(
                folderId = uncategorizedId,
                title = "GitHub Portfolios",
                url = "https://github.com",
                note = "💼 個人履歷與專案雲端備份",
                tags = "Work,Tech"
            ))

            selectFolder(0L) // Refresh back to root folder list
        }
    }

    fun insertTag(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val trimmed = name.trim()
                repository.insertTag(TagEntity(name = trimmed))
            }
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
            return LinkViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
