package com.pandulapeter.beagle.views.drawerItems.singleSelectionListItem

import com.pandulapeter.beagle.views.drawerItems.DrawerItemViewModel
import com.pandulapeter.beagleCore.contracts.BeagleListItemContract

internal data class SingleSelectionListItemViewModel<T : BeagleListItemContract>(
    val listModuleId: String,
    val item: T,
    val isSelected: Boolean,
    val onItemSelected: (id: String) -> Unit
) : DrawerItemViewModel {

    override val id = "${listModuleId}_${item.id}"
    val name = item.name

    fun invokeItemSelectedCallback() = onItemSelected(item.id)
}