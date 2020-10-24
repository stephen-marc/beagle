package com.pandulapeter.beagle.core.list.delegates

import com.pandulapeter.beagle.BeagleCore
import com.pandulapeter.beagle.common.configuration.toText
import com.pandulapeter.beagle.common.contracts.module.Cell
import com.pandulapeter.beagle.core.list.cells.ExpandedItemTextCell
import com.pandulapeter.beagle.core.list.delegates.shared.ExpandableModuleDelegate
import com.pandulapeter.beagle.core.util.extension.append
import com.pandulapeter.beagle.modules.LifecycleLogListModule

internal class LifecycleLogListDelegate : ExpandableModuleDelegate<LifecycleLogListModule> {

    override fun canExpand(module: LifecycleLogListModule) = BeagleCore.implementation.getLifecycleLogEntries(module.eventTypes).isNotEmpty()

    override fun MutableList<Cell<*>>.addItems(module: LifecycleLogListModule) {
        addAll(BeagleCore.implementation.getLifecycleLogEntries(module.eventTypes).take(module.maxItemCount).map { entry ->
            ExpandedItemTextCell(
                id = "${module.id}_${entry.id}",
                text = (module.timestampFormatter?.let { formatter ->
                    "[".append(formatter(entry.timestamp)).append("] ").append(entry.getFormattedTitle(module.shouldDisplayFullNames))
                } ?: entry.getFormattedTitle(module.shouldDisplayFullNames)).toText(),
                isEnabled = true,
                shouldEllipsize = false,
                onItemSelected = null
            )
        })
    }
}