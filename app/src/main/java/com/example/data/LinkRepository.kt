package com.example.data

import kotlinx.coroutines.flow.Flow

class LinkRepository(private val linkDao: LinkDao) {

    val allFolders: Flow<List<FolderEntity>> = linkDao.getAllFolders()
    val allLinks: Flow<List<LinkEntity>> = linkDao.getAllLinks()

    fun getLinksByFolder(folderId: Long): Flow<List<LinkEntity>> =
        linkDao.getLinksByFolder(folderId)

    suspend fun insertFolder(folder: FolderEntity): Long =
        linkDao.insertFolder(folder)

    suspend fun updateFolder(folder: FolderEntity) =
        linkDao.updateFolder(folder)

    suspend fun deleteFolder(folder: FolderEntity) {
        // Automatically purge any nested links inside this folder
        linkDao.deleteLinksByFolder(folder.id)
        linkDao.deleteFolder(folder)
    }

    suspend fun insertLink(link: LinkEntity): Long =
        linkDao.insertLink(link)

    suspend fun updateLink(link: LinkEntity) =
        linkDao.updateLink(link)

    suspend fun deleteLink(link: LinkEntity) =
        linkDao.deleteLink(link)
}
