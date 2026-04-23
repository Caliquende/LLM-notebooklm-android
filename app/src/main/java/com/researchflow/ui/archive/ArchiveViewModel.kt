package com.researchflow.ui.archive

import androidx.lifecycle.ViewModel
import com.researchflow.data.local.entity.ThreadEntity
import com.researchflow.data.repository.ResearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    repository: ResearchRepository
) : ViewModel() {
    val threads: Flow<List<ThreadEntity>> = repository.getAllThreads()
}
