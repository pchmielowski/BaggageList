package net.chmielowski.baggage.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.SuspendExecutor
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chmielowski.baggage.ui.EquipmentListViewModel.State.NewItemInput.Hidden
import net.chmielowski.baggage.ui.EquipmentListViewModel.State.NewItemInput.Visible
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class EquipmentListViewModel(
    private val observeEquipments: ObserveEquipments,
    private val insertEquipment: InsertEquipment,
    private val setEquipmentPacked: SetEquipmentPacked,
    private val deleteEquipment: DeleteEquipment,
    private val undoDeleteEquipment: UndoDeleteEquipment,
) : ViewModel() {

    private val storeFactory = DefaultStoreFactory

    private val store = storeFactory.create(
        name = "EquipmentListStore",
        initialState = State(),
        executorFactory = { Executor() },
        reducer = ReducerImpl(),
        bootstrapper = SimpleBootstrapper(Unit)
    )

    // TODO: Event bus
    private val labels = store.labels
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    //region Callbacks
    fun onAddItemClick() = store.accept(Intent.AddNew)

    fun onNewItemNameEnter(name: String) = store.accept(Intent.SetNewItemName(name))

    fun onAddingNewItemConfirm() = store.accept(Intent.ConfirmAddingNew)

    fun onCancelAddingClick() = store.accept(Intent.CancelAddingNew)

    fun onItemPackedToggle(id: EquipmentId, isPacked: Boolean) =
        store.accept(Intent.MarkPacked(id, isPacked))

    fun onDeleteClick() = store.accept(Intent.EnterDeletingMode)

    fun onDeleteItemClick(id: EquipmentId) = store.accept(Intent.Delete(id))

    fun onCancelDeletingClick() = store.accept(Intent.ExitDeletingMode)

    fun onUndoDeleteClick() = store.accept(Intent.UndoDeleting)
    //endregion

    fun observeModel(): Flow<Model> = store.states

    fun observeLabels(): Flow<Label> = labels

    // TODO: Rename intents
    sealed class Intent {
        object AddNew : Intent()
        data class SetNewItemName(val name: String) : Intent()
        object ConfirmAddingNew : Intent()
        object CancelAddingNew : Intent()

        data class MarkPacked(val id: EquipmentId, val isPacked: Boolean) : Intent()

        object EnterDeletingMode : Intent()
        object ExitDeletingMode : Intent()
        data class Delete(val id: EquipmentId) : Intent()
        object UndoDeleting : Intent()
    }

    private sealed class Result {
        data class StateUpdate(val update: State.() -> State) : Result()
    }

    sealed class Label {
        data class ShowUndoSnackbar(val itemName: String) : Label()
    }

    private data class State(
        val newItem: NewItemInput = Hidden,
        val isDeleteMode: Boolean = false,
        val lastDeleted: EquipmentId? = null,
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

        override val isCancelDeletingVisible get() = isDeleteMode

        override val isDeleteButtonVisible get() = !isDeleteMode

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
        val isCancelDeletingVisible: Boolean
        val isDeleteButtonVisible: Boolean
    }

    private inner class Executor :
        SuspendExecutor<Intent, Unit, State, Result, Label>(viewModelScope.coroutineContext) {

        override suspend fun executeAction(action: Unit, getState: () -> State) {
            viewModelScope.launch {
                observeEquipments()
                    .collectLatest { list ->
                        updateState { copy(equipmentList = list) }
                    }
            }
        }

        override suspend fun executeIntent(intent: Intent, getState: () -> State) = when (intent) {
            Intent.AddNew -> updateState {
                copy(newItem = Visible(""))
            }
            is Intent.SetNewItemName -> updateState {
                if (newItem is Visible) {
                    copy(newItem = Visible(intent.name))
                } else {
                    this
                }
            }
            Intent.ConfirmAddingNew -> {
                insertEquipment((getState().newItem as Visible).text)
                updateState { copy(newItem = Hidden) }
            }
            Intent.CancelAddingNew -> updateState {
                copy(newItem = Hidden)
            }
            is Intent.MarkPacked -> setEquipmentPacked(intent.id, intent.isPacked)
            Intent.EnterDeletingMode -> updateState {
                copy(isDeleteMode = true)
            }
            Intent.ExitDeletingMode -> updateState {
                copy(isDeleteMode = false)
            }
            is Intent.Delete -> {
                updateState { copy(lastDeleted = intent.id) }
                val lastDeletedName = getState().equipmentList.single { it.id == intent.id }.name
                deleteEquipment(intent.id)
                publish(Label.ShowUndoSnackbar(lastDeletedName))
            }
            Intent.UndoDeleting -> undoDeleteEquipment(getState().lastDeleted!!)
        }

        private fun updateState(update: State.() -> State) =
            dispatch(Result.StateUpdate(update))
    }

    private class ReducerImpl : Reducer<State, Result> {

        override fun State.reduce(result: Result) = when (result) {
            is Result.StateUpdate -> result.update(this)
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

class DatabaseExecutor(
    private val database: Database,
    private val context: CoroutineContext = Dispatchers.IO,
) {

    suspend fun <T> execute(action: Database.() -> T) = withContext(context) {
        database.action()
    }
}

// TODO: Move
class InsertEquipment(private val executor: DatabaseExecutor) {

    suspend operator fun invoke(name: String) =
        executor.execute { equipmentQueries.insertEquimpent(name) }
}

// TODO: Move
class SetEquipmentPacked(private val executor: DatabaseExecutor) {

    suspend operator fun invoke(id: EquipmentId, isPacked: Boolean) =
        executor.execute { equipmentQueries.setEquipmentPacked(id, isPacked) }
}

// TODO: Move
class DeleteEquipment(private val executor: DatabaseExecutor) {

    suspend operator fun invoke(id: EquipmentId) =
        executor.execute { equipmentQueries.deleteEquipment(id) }
}

// TODO: Move
class UndoDeleteEquipment(private val executor: DatabaseExecutor) {

    suspend operator fun invoke(id: EquipmentId) =
        executor.execute { equipmentQueries.undoDeleteEquipment(id) }
}
