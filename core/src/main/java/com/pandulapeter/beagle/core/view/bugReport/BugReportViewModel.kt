package com.pandulapeter.beagle.core.view.bugReport

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pandulapeter.beagle.BeagleCore
import com.pandulapeter.beagle.core.util.extension.getScreenCapturesFolder
import com.pandulapeter.beagle.core.view.bugReport.list.BugReportListItem
import com.pandulapeter.beagle.core.view.bugReport.list.DescriptionViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.GalleryViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.HeaderViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.LogItemViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.NetworkLogItemViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.SendButtonViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.ShowMoreLogsViewHolder
import com.pandulapeter.beagle.core.view.bugReport.list.ShowMoreNetworkLogsViewHolder
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

internal class BugReportViewModel(
    private val context: Context,
    private val shouldShowGallerySection: Boolean,
    private val shouldShowNetworkLogsSection: Boolean,
    private val logLabelSectionsToShow: List<String?>,
    descriptionTemplate: String
) : ViewModel() {

    private val _items = MutableLiveData(emptyList<BugReportListItem>())
    val items: LiveData<List<BugReportListItem>> = _items
    private val _shouldShowLoadingIndicator = MutableLiveData(true)
    val shouldShowLoadingIndicator: LiveData<Boolean> = _shouldShowLoadingIndicator

    private var mediaFiles = emptyList<File>()
    private var selectedMediaFileIds = emptyList<String>()

    private val allNetworkLogEntries = BeagleCore.implementation.getNetworkLogEntries()
    private var lastNetworkLogIndex = LOG_INDEX_INCREMENT - 1
    private var selectedNetworkLogIds = emptyList<String>()
    private fun getNetworkLogEntries() = allNetworkLogEntries.take(lastNetworkLogIndex)
    private fun areThereMoreNetworkLogEntries() = allNetworkLogEntries.size > getNetworkLogEntries().size

    private val allLogEntries = logLabelSectionsToShow.map { label -> label to BeagleCore.implementation.getLogEntries(label) }.toMap()
    private val lastLogIndex = logLabelSectionsToShow.map { label -> label to LOG_INDEX_INCREMENT - 1 }.toMap().toMutableMap()
    private val selectedLogIds = logLabelSectionsToShow.map { label -> label to emptyList<String>() }.toMap().toMutableMap()
    private fun getLogEntries(label: String?) = allLogEntries[label]?.take(lastLogIndex[label] ?: 0).orEmpty()
    private fun areThereMoreLogEntries(label: String?) = allLogEntries[label]?.size ?: 0 > getLogEntries(label).size

    private var description: CharSequence = descriptionTemplate
        set(value) {
            if (field != value) {
                //TODO: Selection is lost
                val shouldRefresh = (field.trim().isEmpty() && value.trim().isNotEmpty()) || (field.trim().isNotEmpty() && value.trim().isEmpty())
                field = value
                if (shouldRefresh) {
                    viewModelScope.launch(listManagerContext) { refreshContent() }
                }
            }
        }

    private val listManagerContext = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    private var isSendButtonEnabled = true
        set(value) {
            field = value
            viewModelScope.launch(listManagerContext) { refreshContent() }
        }
    private val isDataValid
        get() = selectedMediaFileIds.isNotEmpty() ||
                selectedNetworkLogIds.isNotEmpty() ||
                selectedLogIds.keys.any { label -> selectedLogIds[label]?.isNotEmpty() == true } ||
                description.trim().isNotEmpty()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(listManagerContext) {
            _shouldShowLoadingIndicator.postValue(true)
            mediaFiles = context.getScreenCapturesFolder().listFiles().orEmpty().toList().sortedByDescending { it.lastModified() }
            selectedMediaFileIds = selectedMediaFileIds.filter { id -> mediaFiles.any { it.name == id } }
            refreshContent()
        }
    }

    fun onMediaFileLongTapped(fileName: String) = onMediaFileSelectionChanged(fileName)

    fun onNetworkLogLongTapped(id: String) = onNetworkLogSelectionChanged(id)

    fun onShowMoreNetworkLogsTapped() {
        viewModelScope.launch(listManagerContext) {
            lastNetworkLogIndex += LOG_INDEX_INCREMENT
            refreshContent()
        }
    }

    fun onLogLongTapped(id: String, label: String?) = onLogSelectionChanged(id, label)

    fun onShowMoreLogsTapped(label: String?) {
        viewModelScope.launch(listManagerContext) {
            lastLogIndex[label] = (lastLogIndex[label] ?: 0) + LOG_INDEX_INCREMENT
            refreshContent()
        }
    }

    fun onDescriptionChanged(newValue: CharSequence) {
        description = newValue
    }

    fun onSendButtonPressed() {
        if (isSendButtonEnabled) {
            isSendButtonEnabled = false
            //TODO: Generate and share zip file
            //TODO: Attach device info
            isSendButtonEnabled = true
        }
    }

    private fun onMediaFileSelectionChanged(id: String) {
        viewModelScope.launch(listManagerContext) {
            selectedMediaFileIds = if (selectedMediaFileIds.contains(id)) {
                selectedMediaFileIds.filterNot { it == id }
            } else {
                (selectedMediaFileIds + id)
            }.distinct()
            refreshContent()
        }
    }

    private fun onNetworkLogSelectionChanged(id: String) {
        viewModelScope.launch(listManagerContext) {
            selectedNetworkLogIds = if (selectedNetworkLogIds.contains(id)) {
                selectedNetworkLogIds.filterNot { it == id }
            } else {
                (selectedNetworkLogIds + id)
            }.distinct()
            refreshContent()
        }
    }

    //TODO: Does not work when label = null
    private fun onLogSelectionChanged(id: String, label: String?) {
        viewModelScope.launch(listManagerContext) {
            selectedLogIds[label] = if (selectedLogIds[label]?.contains(id) == true) {
                selectedLogIds[label].orEmpty().filterNot { it == id }
            } else {
                (selectedLogIds[label].orEmpty() + id)
            }.distinct()
            refreshContent()
        }
    }

    private suspend fun refreshContent() = withContext(listManagerContext) {
        _items.postValue(mutableListOf<BugReportListItem>().apply {
            if (shouldShowGallerySection && mediaFiles.isNotEmpty()) {
                add(
                    HeaderViewHolder.UiModel(
                        id = "headerGallery",
                        text = BeagleCore.implementation.appearance.bugReportTexts.gallerySectionTitle(selectedMediaFileIds.size)
                    )
                )
                add(GalleryViewHolder.UiModel(mediaFiles.map { it.name to it.lastModified() }, selectedMediaFileIds))
            }
            getNetworkLogEntries().let { networkLogEntries ->
                if (shouldShowNetworkLogsSection && networkLogEntries.isNotEmpty()) {
                    add(
                        HeaderViewHolder.UiModel(
                            id = "headerNetworkLogs",
                            text = BeagleCore.implementation.appearance.bugReportTexts.networkLogsSectionTitle(selectedNetworkLogIds.size)
                        )
                    )
                    addAll(networkLogEntries.map { entry ->
                        NetworkLogItemViewHolder.UiModel(
                            entry = entry,
                            isSelected = selectedNetworkLogIds.contains(entry.id)
                        )
                    })
                    if (areThereMoreNetworkLogEntries()) {
                        add(ShowMoreNetworkLogsViewHolder.UiModel())
                    }
                }
            }
            logLabelSectionsToShow.distinct().forEach { label ->
                getLogEntries(label).let { logEntries ->
                    if (logEntries.isNotEmpty()) {
                        add(
                            HeaderViewHolder.UiModel(
                                id = "headerLogs_$label",
                                text = BeagleCore.implementation.appearance.bugReportTexts.logsSectionTitle(label, selectedLogIds[label]?.size ?: 0)
                            )
                        )
                        addAll(logEntries.map { entry ->
                            LogItemViewHolder.UiModel(
                                entry = entry,
                                isSelected = selectedLogIds[label].orEmpty().contains(entry.id)
                            )
                        })
                        if (areThereMoreLogEntries(label)) {
                            add(ShowMoreLogsViewHolder.UiModel(label))
                        }
                    }
                }
            }
            add(
                HeaderViewHolder.UiModel(
                    id = "headerDescription",
                    text = BeagleCore.implementation.appearance.bugReportTexts.descriptionSectionTitle
                )
            )
            add(DescriptionViewHolder.UiModel(description))
            add(SendButtonViewHolder.UiModel(isSendButtonEnabled && isDataValid))
        })
        _shouldShowLoadingIndicator.postValue(false)
    }

    companion object {
        private const val LOG_INDEX_INCREMENT = 5
    }
}