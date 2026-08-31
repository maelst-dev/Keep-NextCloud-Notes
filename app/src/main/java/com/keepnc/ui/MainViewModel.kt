package com.keepnc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepnc.data.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for [MainActivity].
 * Exposes the list of categories to populate the Navigation Drawer dynamically.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    repository: NotesRepository
) : ViewModel() {

    /**
     * Live list of distinct note categories from Room.
     * The drawer updates automatically whenever notes are added/edited/deleted.
     */
    val categories: StateFlow<List<String>> = repository.getCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
