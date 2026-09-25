package com.leshoraa.scanorea.features.recentpdfs.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.leshoraa.scanorea.features.recentpdfs.data.PdfThumbnailLoader
import com.leshoraa.scanorea.features.recentpdfs.data.RecentPdfsRepository
import com.leshoraa.scanorea.features.recentpdfs.domain.model.RecentPdf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * UI presentation state for the Results and Recent PDFs feature.
 */
data class RecentPdfsUiState(
    val recentPdfs: List<RecentPdf> = emptyList(),
    val categories: List<String> = emptyList(),
    val pinnedFolders: List<String> = listOf("Favorites", "Work", "Study"),
    val isLoading: Boolean = false
)

/**
 * ViewModel managing document management, categories, pinned folders,
 * and metadata in the Results tab.
 */
class RecentPdfsViewModel(
    private val recentPdfsRepository: RecentPdfsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentPdfsUiState())
    val uiState: StateFlow<RecentPdfsUiState> = _uiState.asStateFlow()

    init {
        loadRecentPdfs()
    }

    fun loadRecentPdfs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val recents = recentPdfsRepository.getRecentPdfs()
            val categories = recentPdfsRepository.getCategories()
            val pinnedFolders = recentPdfsRepository.getPinnedFolders()
            _uiState.update {
                it.copy(
                    recentPdfs = recents,
                    categories = categories,
                    pinnedFolders = pinnedFolders,
                    isLoading = false
                )
            }
        }
    }

    fun deletePdf(file: File) {
        PdfThumbnailLoader.evict(file)
        viewModelScope.launch {
            recentPdfsRepository.deletePdf(file)
            loadRecentPdfs()
        }
    }

    fun toggleFavorite(file: File) {
        viewModelScope.launch {
            recentPdfsRepository.toggleFavorite(file)
            loadRecentPdfs()
        }
    }

    fun updatePdfFolders(file: File, folders: List<String>) {
        viewModelScope.launch {
            recentPdfsRepository.updatePdfFolders(file, folders)
            loadRecentPdfs()
        }
    }

    fun addCategory(category: String) {
        viewModelScope.launch {
            recentPdfsRepository.addCategory(category)
            loadRecentPdfs()
        }
    }

    fun renameCategory(oldCategory: String, newCategory: String) {
        viewModelScope.launch {
            recentPdfsRepository.renameCategory(oldCategory, newCategory)
            loadRecentPdfs()
        }
    }

    fun deleteCategory(category: String) {
        viewModelScope.launch {
            recentPdfsRepository.deleteCategory(category)
            loadRecentPdfs()
        }
    }

    fun togglePinFolder(folder: String) {
        viewModelScope.launch {
            recentPdfsRepository.togglePinFolder(folder)
            loadRecentPdfs()
        }
    }

    fun setPinnedFolders(folders: List<String>) {
        viewModelScope.launch {
            recentPdfsRepository.setPinnedFolders(folders)
            loadRecentPdfs()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = RecentPdfsRepository(context.applicationContext)
                    return RecentPdfsViewModel(repo) as T
                }
            }
    }
}
