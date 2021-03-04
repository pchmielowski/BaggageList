package net.chmielowski.baggage.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.extensions.coroutines.SuspendExecutor
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.chmielowski.baggage.ui.databinding.ScreenEquipmentListBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class EquipmentListFragment : Fragment(R.layout.screen_equipment_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewModel by viewModel<EquipmentListViewModel>()
        val binding = ScreenEquipmentListBinding.bind(view)
        val adapter = EquipmentAdapter(
            onItemToggled = { id, isToggled -> viewModel.onItemPackedToggle(id, isToggled) },
        )
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.observeModel().collectLatest { model ->
                binding.render(adapter, model)
            }
        }

        binding.bindListeners(viewModel)
    }

    private fun ScreenEquipmentListBinding.render(
        adapter: EquipmentAdapter,
        model: EquipmentListViewModel.Model,
    ) {
        adapter.submitList(model.items)
        addNewInputGroup.isVisible = model.isInputVisible
        addNew.isVisible = model.isAddNewVisible
    }

    private fun ScreenEquipmentListBinding.bindListeners(viewModel: EquipmentListViewModel) {
        addNew.setOnClickListener {
            viewModel.onAddItemClick()
        }
        newItemName.doOnTextChanged { text ->
            viewModel.onNewItemNameEnter(text)
        }
        confirmAdding.setOnClickListener {
            viewModel.onAddingNewItemConfirm()
        }
        cancel.setOnClickListener {
            viewModel.onCancelAddingClick()
        }
    }
}

// TODO: Move
fun TextView.doOnTextChanged(action: (text: String) -> Unit) =
    doOnTextChanged { text, start, before, count ->
        action(text!!.toString())
    }

// TODO: Move
class EquipmentListViewModel(private val database: Database) : ViewModel() {

    private val storeFactory = DefaultStoreFactory

    private val store = storeFactory.create(
        name = "EquipmentListStore",
        initialState = State(),
        executorFactory = { Executor() },
        reducer = ReducerImpl(),
    )

    init {
        val mapToList = observeEquipmentList()
        viewModelScope.launch {
            mapToList
                .collectLatest { list ->
                    store.accept(Intent.ListUpdate(list))
                }
        }
    }

    // TODO: Class
    private fun observeEquipmentList() = database.equipmentQueries
        .selectEquipments(::EquipmentDto)
        .asFlow()
        .mapToList(viewModelScope.coroutineContext)

    fun onAddItemClick() = store.accept(Intent.AddNew)

    fun onNewItemNameEnter(name: String) {
        store.accept(Intent.NewItemNameEnter(name))
    }

    fun onAddingNewItemConfirm() = store.accept(Intent.AddingItemConfirm)

    fun onCancelAddingClick() = store.accept(Intent.AddingNewCancel)

    fun onItemPackedToggle(id: EquipmentId, isPacked: Boolean) =
        store.accept(Intent.ItemPacked(id, isPacked))

    fun observeModel(): Flow<Model> = store.states

    fun observeLabels() = store.labels

    sealed class Intent {
        data class ListUpdate(val list: List<EquipmentDto>) : Intent()

        object AddNew : Intent()
        data class NewItemNameEnter(val name: String) : Intent()
        object AddingItemConfirm : Intent()
        object AddingNewCancel : Intent()

        data class ItemPacked(val id: EquipmentId, val isPacked: Boolean) : Intent()
    }

    private sealed class Result {
        class NewState(val state: State) : Result()
    }

    sealed class Label

    private data class State(
        val newItemName: String = "",
        val isAddingNew: Boolean = false,

        val equipmentList: List<EquipmentDto> = emptyList(),
    ) : Model {

        override val isInputVisible get() = isAddingNew

        override val isAddNewVisible get() = !isAddingNew

        override val items get() = equipmentList.map { EquipmentItem(it.id, it.name, it.isPacked) }
    }

    interface Model {
        val isInputVisible: Boolean
        val isAddNewVisible: Boolean
        val items: List<EquipmentItem>
    }

    private inner class Executor :
        SuspendExecutor<Intent, Nothing, State, Result, Label>(viewModelScope.coroutineContext) {

        override suspend fun executeIntent(intent: Intent, getState: () -> State) = when (intent) {
            is Intent.ListUpdate -> dispatch(Result.NewState(getState().copy(equipmentList = intent.list)))
            Intent.AddNew -> dispatch(Result.NewState(getState().copy(isAddingNew = true)))
            is Intent.NewItemNameEnter -> dispatch(Result.NewState(getState().copy(newItemName = intent.name)))
            Intent.AddingItemConfirm -> {
                // TODO: Class
                database.equipmentQueries.insertEquimpent(getState().newItemName)
                dispatch(
                    Result.NewState(
                        getState().copy(
                            isAddingNew = false,
                        )
                    )
                )
            }
            Intent.AddingNewCancel -> dispatch(Result.NewState(getState().copy(isAddingNew = false)))
            is Intent.ItemPacked -> {
                // TODO: Class
                database.equipmentQueries.setEquipmentPacked(intent.id, intent.isPacked)
            }
        }
    }

    private class ReducerImpl : Reducer<State, Result> {

        override fun State.reduce(result: Result): State {
            return when (result) {
                is Result.NewState -> result.state
            }
        }
    }
}

data class EquipmentDto(
    val id: EquipmentId,
    val name: String,
    val isPacked: Boolean,
)
