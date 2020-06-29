package com.pandulapeter.beagle.core.manager

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pandulapeter.beagle.common.configuration.Placement
import com.pandulapeter.beagle.common.contracts.BeagleListItemContract
import com.pandulapeter.beagle.common.contracts.module.Module
import com.pandulapeter.beagle.core.list.CellAdapter
import com.pandulapeter.beagle.core.list.moduleDelegates.AnimationDurationSwitchDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.AppInfoButtonDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.ButtonDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.CheckBoxDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.DeviceInfoDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.DividerDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.ForceCrashButtonDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.HeaderDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.ItemListDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.KeyValueListDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.KeylineOverlaySwitchDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.LabelDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.LogListDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.LongTextDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.MultipleSelectionListDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.PaddingDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.SingleSelectionListDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.SwitchDelegate
import com.pandulapeter.beagle.core.list.moduleDelegates.TextDelegate
import com.pandulapeter.beagle.modules.AnimationDurationSwitchModule
import com.pandulapeter.beagle.modules.AppInfoButtonModule
import com.pandulapeter.beagle.modules.ButtonModule
import com.pandulapeter.beagle.modules.CheckBoxModule
import com.pandulapeter.beagle.modules.DeviceInfoModule
import com.pandulapeter.beagle.modules.DividerModule
import com.pandulapeter.beagle.modules.ForceCrashButtonModule
import com.pandulapeter.beagle.modules.HeaderModule
import com.pandulapeter.beagle.modules.ItemListModule
import com.pandulapeter.beagle.modules.KeyValueListModule
import com.pandulapeter.beagle.modules.KeylineOverlaySwitchModule
import com.pandulapeter.beagle.modules.LabelModule
import com.pandulapeter.beagle.modules.LogListModule
import com.pandulapeter.beagle.modules.LongTextModule
import com.pandulapeter.beagle.modules.MultipleSelectionListModule
import com.pandulapeter.beagle.modules.PaddingModule
import com.pandulapeter.beagle.modules.SingleSelectionListModule
import com.pandulapeter.beagle.modules.SwitchModule
import com.pandulapeter.beagle.modules.TextModule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.reflect.KClass

internal class ListManager {

    private val cellAdapter = CellAdapter()
    private val moduleManagerContext = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    private val modules = mutableListOf<Module<*>>()
    private val moduleDelegates = mutableMapOf(
        AnimationDurationSwitchModule::class to AnimationDurationSwitchDelegate(),
        AppInfoButtonModule::class to AppInfoButtonDelegate(),
        ButtonModule::class to ButtonDelegate(),
        CheckBoxModule::class to CheckBoxDelegate(),
        DeviceInfoModule::class to DeviceInfoDelegate(),
        DividerModule::class to DividerDelegate(),
        ForceCrashButtonModule::class to ForceCrashButtonDelegate(),
        HeaderModule::class to HeaderDelegate(),
        LabelModule::class to LabelDelegate(),
        LogListModule::class to LogListDelegate(),
        LongTextModule::class to LongTextDelegate(),
        ItemListModule::class to ItemListDelegate<BeagleListItemContract>(),
        KeylineOverlaySwitchModule::class to KeylineOverlaySwitchDelegate(),
        KeyValueListModule::class to KeyValueListDelegate(),
        MultipleSelectionListModule::class to MultipleSelectionListDelegate<BeagleListItemContract>(),
        PaddingModule::class to PaddingDelegate(),
        SingleSelectionListModule::class to SingleSelectionListDelegate<BeagleListItemContract>(),
        SwitchModule::class to SwitchDelegate(),
        TextModule::class to TextDelegate()
    )
    private var job: Job? = null

    fun setupRecyclerView(recyclerView: RecyclerView) = recyclerView.run {
        adapter = cellAdapter
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(recyclerView.context)
    }

    fun setModules(newModules: List<Module<*>>) {
        job?.cancel()
        job = GlobalScope.launch { setModulesInternal(newModules) }
    }

    fun addModules(newModules: List<Module<*>>, placement: Placement, lifecycleOwner: LifecycleOwner?) {
        job?.cancel()
        job = GlobalScope.launch { addModulesInternal(newModules, placement, lifecycleOwner) }
    }

    fun removeModules(ids: List<String>) {
        job?.cancel()
        job = GlobalScope.launch { removeModulesInternal(ids) }
    }

    fun refreshCells() {
        job?.cancel()
        job = GlobalScope.launch { refreshCellsInternal() }
    }

    fun contains(id: String) = modules.any { it.id == id }

    //TODO: This might cause concurrency issues. Consider making it a suspend function.
    @Suppress("UNCHECKED_CAST")
    fun <M : Module<*>> findModule(id: String): M? = modules.firstOrNull { it.id == id } as? M?

    //TODO: This might cause concurrency issues. Consider making it a suspend function.
    @Suppress("UNCHECKED_CAST")
    fun <M : Module<M>> findModuleDelegate(type: KClass<out M>) = moduleDelegates[type] as Module.Delegate<M>

    private suspend fun setModulesInternal(newModules: List<Module<*>>) = withContext(moduleManagerContext) {
        modules.clear()
        modules.addAll(newModules.distinctBy { it.id })
        refreshCellsInternal()
    }

    @Suppress("unused")
    private suspend fun addModulesInternal(newModules: List<Module<*>>, placement: Placement, lifecycleOwner: LifecycleOwner?) =
        lifecycleOwner?.lifecycle?.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() {
                job?.cancel()
                job = GlobalScope.launch { addModulesInternal(newModules, placement) }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                job?.cancel()
                job = GlobalScope.launch { removeModules(newModules.map { it.id }) }
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        }) ?: addModulesInternal(newModules, placement)

    private suspend fun addModulesInternal(newModules: List<Module<*>>, placement: Placement) = withContext(moduleManagerContext) {
        modules.apply {
            var newIndex = 0
            newModules.forEach { module ->
                indexOfFirst { it.id == module.id }.also { currentIndex ->
                    if (currentIndex != -1) {
                        removeAt(currentIndex)
                        add(currentIndex, module)
                    } else {
                        when (placement) {
                            Placement.Bottom -> add(module)
                            Placement.Top -> add(newIndex++, module)
                            is Placement.Below -> {
                                indexOfFirst { it.id == placement.id }.also { referencePosition ->
                                    if (referencePosition == -1) {
                                        add(module)
                                    } else {
                                        add(referencePosition + 1 + newIndex++, module)
                                    }
                                }
                            }
                            is Placement.Above -> {
                                indexOfFirst { it.id == placement.id }.also { referencePosition ->
                                    if (referencePosition == -1) {
                                        add(newIndex++, module)
                                    } else {
                                        add(referencePosition, module)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        refreshCellsInternal()
    }

    private suspend fun removeModulesInternal(ids: List<String>) = withContext(moduleManagerContext) {
        modules.removeAll { ids.contains(it.id) }
        refreshCellsInternal()
    }

    //TODO: Throw exception if no handler is found
    private suspend fun refreshCellsInternal() = withContext(moduleManagerContext) {
        cellAdapter.submitList(modules.flatMap { module ->
            (moduleDelegates[module::class] ?: (module.createModuleDelegate().also {
                moduleDelegates[module::class] = it
            })).forceCreateCells(module)
        })
    }
}