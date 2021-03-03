package net.chmielowski.baggage.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
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

class EquipmentListFragment : Fragment(R.layout.screen_equipment_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val model by viewModels<EquipmentListViewModel>()
        val binding = ScreenEquipmentListBinding.bind(view)
        val adapter = EquipmentAdapter()
        binding.list.adapter = adapter

        adapter.submitList((1..30).map { EquipmentItem(EquipmentId(it.toLong()), "Item $it") })
    }
}

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

    private fun observeEquipmentList() = database.equipmentQueries
        .selectEquipments(::EquipmentDto)
        .asFlow()
        .mapToList()

    fun onAddItemClick() = store.accept(Intent.AddNew)

    fun onNewItemNameEnter(name: String) {
        store.accept(Intent.NewItemNameEnter(name))
    }

    fun onAddingNewItemConfirm() = store.accept(Intent.AddingItemConfirm)

    fun observeModel(): Flow<Model> = store.states

    fun observeLabels() = store.labels

    sealed class Intent {
        data class ListUpdate(val list: List<EquipmentDto>) : Intent()

        object AddNew : Intent()
        data class NewItemNameEnter(val name: String) : Intent()
        object AddingItemConfirm : Intent()
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

        override val items get() = equipmentList.map { EquipmentItem(it.id, it.name) }
    }

    interface Model {
        val isInputVisible: Boolean
        val items: List<EquipmentItem>
    }

    private inner class Executor : SuspendExecutor<Intent, Nothing, State, Result, Label>() {

        override suspend fun executeIntent(intent: Intent, getState: () -> State) = when (intent) {
            is Intent.ListUpdate -> dispatch(Result.NewState(getState().copy(equipmentList = intent.list)))
            Intent.AddNew -> dispatch(Result.NewState(getState().copy(isAddingNew = true)))
            is Intent.NewItemNameEnter -> dispatch(Result.NewState(getState().copy(newItemName = intent.name)))
            Intent.AddingItemConfirm -> {
                database.equipmentQueries.insertEquimpent(getState().newItemName)
                dispatch(
                    Result.NewState(
                        getState().copy(
                            isAddingNew = false,
                        )
                    )
                )
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
)
