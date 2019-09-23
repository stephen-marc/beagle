package com.pandulapeter.debugMenu.views.items.singleSelectionListItem

import com.pandulapeter.debugMenu.views.items.DrawerItemViewModel
import com.pandulapeter.debugMenuCore.configuration.modules.ListModule

internal data class SingleSelectionListItemViewModel<T : ListModule.Item>(
    val listModuleId: String,
    val item: T,
    val isSelected: Boolean,
    val onItemSelected: (id: String) -> Unit
) : DrawerItemViewModel {

    override val id = "${listModuleId}_${item.id}"
    override val shouldUsePayloads = true
    val name = item.name

    fun invokeItemSelectedCallback() = onItemSelected(item.id)
}