package net.chmielowski.baggage.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.extensions.coroutines.SuspendExecutor
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.chmielowski.baggage.ui.EquipmentListViewModel.State.NewItemInput.Hidden
import net.chmielowski.baggage.ui.EquipmentListViewModel.State.NewItemInput.Visible
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class EquipmentListViewModel(
    private val observeEquipments: ObserveEquipments,
    private val insertEquipment: InsertEquipment,
    private val setEquipmentPacked: SetEquipmentPacked,
) : ViewModel() {

    private val storeFactory = DefaultStoreFactory

    private val store = storeFactory.create(
        name = "EquipmentListStore",
        initialState = State(),
        executorFactory = { Executor() },
        reducer = ReducerImpl(),
    )

    init {
        viewModelScope.launch {
            observeEquipments()
                .collectLatest { list ->
                    store.accept(Intent.ListUpdate(list))
                }
        }
    }

    fun onAddItemClick() = store.accept(Intent.AddNew)

    fun onNewItemNameEnter(name: String) {
        store.accept(Intent.NewItemNameEnter(name))
    }

    fun onAddingNewItemConfirm() = store.accept(Intent.AddingItemConfirm)

    fun onCancelAddingClick() = store.accept(Intent.AddingNewCancel)

    fun onItemPackedToggle(id: EquipmentId, isPacked: Boolean) =
        store.accept(Intent.ItemPacked(id, isPacked))

    fun onDeleteClick() = store.accept(Intent.DeleteClick)

    fun observeModel(): Flow<Model> = store.states

    sealed class Intent {
        data class ListUpdate(val list: List<EquipmentDto>) : Intent()

        object AddNew : Intent()
        data class NewItemNameEnter(val name: String) : Intent()
        object AddingItemConfirm : Intent()
        object AddingNewCancel : Intent()
        object DeleteClick : Intent()

        data class ItemPacked(val id: EquipmentId, val isPacked: Boolean) : Intent()
    }

    private sealed class Result {
        class NewState(val state: State) : Result()
    }

    sealed class Label

    private data class State(
        val newItem: NewItemInput = Hidden,
        val isDeleteMode: Boolean = false,
        val equipmentList: List<EquipmentDto> = emptyList(),
    ) : Model {

        override val progress: Int
            get() {
                val all = equipmentList.size
                if (all == 0) {
                    return 0
                }
                val packed = equipmentList.count { it.isPacked }.toFloat()
                return (packed / all * 100).roundToInt()
            }

        override val isInputVisible get() = newItem is Visible

        override val isAddNewVisible get() = newItem is Hidden

        override val items
            get() = equipmentList.map {
                EquipmentItem(
                    it.id,
                    it.name,
                    it.isPacked,
                    isDeleteMode,
                )
            }

        sealed class NewItemInput {
            object Hidden : NewItemInput()
            data class Visible(val text: String) : NewItemInput()
        }
    }

    interface Model {
        val progress: Int
        val isInputVisible: Boolean
        val isAddNewVisible: Boolean
        val items: List<EquipmentItem>
    }

    private inner class Executor :
        SuspendExecutor<Intent, Nothing, State, Result, Label>(viewModelScope.coroutineContext) {

        override suspend fun executeIntent(intent: Intent, getState: () -> State) = when (intent) {
            is Intent.ListUpdate -> dispatchState(getState) {
                copy(equipmentList = intent.list)
            }
            Intent.AddNew -> dispatchState(getState) {
                copy(newItem = Visible(""))
            }
            is Intent.NewItemNameEnter -> dispatchState(getState) {
                copy(newItem = Visible(intent.name))
            }
            Intent.AddingItemConfirm -> {
                insertEquipment((getState().newItem as Visible).text)
                dispatchState(getState) { copy(newItem = Hidden) }
            }
            Intent.AddingNewCancel -> dispatchState(getState) {
                copy(newItem = Hidden)
            }
            is Intent.ItemPacked -> setEquipmentPacked(intent.id, intent.isPacked)
            Intent.DeleteClick -> dispatchState(getState) {
                copy(isDeleteMode = true)
            }
        }

        private fun dispatchState(getState: () -> State, modifyState: State.() -> State) =
            dispatch(Result.NewState(getState().modifyState()))
    }

    private class ReducerImpl : Reducer<State, Result> {

        override fun State.reduce(result: Result): State {
            return when (result) {
                is Result.NewState -> result.state
            }
        }
    }
}

// TODO: Move
class ObserveEquipments(
    private val database: Database,
    private val context: CoroutineContext = Dispatchers.IO,
) {

    operator fun invoke() = database.equipmentQueries
        .selectEquipments(::EquipmentDto)
        .asFlow()
        .mapToList(context)
}

// TODO: Move
class InsertEquipment(private val database: Database) {

    // TODO: Dispatcher
    operator fun invoke(name: String) {
        database.equipmentQueries.insertEquimpent(name)
    }
}

// TODO: Move
class SetEquipmentPacked(private val database: Database) {

    // TODO: Dispatcher
    operator fun invoke(id: EquipmentId, isPacked: Boolean) {
        database.equipmentQueries.setEquipmentPacked(id, isPacked)
    }
}
