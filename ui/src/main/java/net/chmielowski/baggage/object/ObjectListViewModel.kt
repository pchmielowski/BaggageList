package net.chmielowski.baggage.`object`

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
import net.chmielowski.baggage.`object`.ObjectListViewModel.State.Mode
import net.chmielowski.baggage.ui.Database
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class ObjectListViewModel(
    private val observeObjects: ObserveObjects,
    private val insertObject: InsertObject,
    private val setObjectPacked: SetObjectPacked,
    private val deleteObject: DeleteObject,
    private val undoDeleteObject: UndoDeleteObject,
) : ViewModel() {

    private val storeFactory = DefaultStoreFactory

    private val store = storeFactory.create(
        name = "ObjectListStore",
        initialState = State(),
        executorFactory = { Executor() },
        reducer = ReducerImpl(),
        bootstrapper = SimpleBootstrapper(Unit)
    )

    // TODO: Event bus
    private val labels = store.labels
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    //region Callbacks
    fun onEnterEditModeClick() = store.accept(Intent.EnterEditMode)

    fun onNewObjectNameChange(name: String) = store.accept(Intent.SetNewObjectName(name))

    fun onAddingNewItemConfirm() = store.accept(Intent.ConfirmAddingObject)

    fun onExitEditModeClick() = store.accept(Intent.ExitEditMode)

    fun onItemPackedToggle(id: ObjectId, isPacked: Boolean) =
        store.accept(Intent.MarkPacked(id, isPacked))

    fun onDeleteItemClick(id: ObjectId) = store.accept(Intent.Delete(id))

    fun onCancelDeletingClick() = store.accept(Intent.ExitEditMode)

    fun onUndoDeleteClick() = store.accept(Intent.UndoDeleting)
    //endregion

    fun observeModel(): Flow<Model> = store.states

    fun observeLabels(): Flow<Label> = labels

    sealed class Intent {
        object EnterEditMode : Intent()
        object ExitEditMode : Intent()

        data class SetNewObjectName(val name: String) : Intent()
        object ConfirmAddingObject : Intent()

        data class MarkPacked(val id: ObjectId, val isPacked: Boolean) : Intent()

        data class Delete(val id: ObjectId) : Intent()
        object UndoDeleting : Intent()
    }

    private sealed class Result {
        data class StateUpdate(val update: State.() -> State) : Result()
    }

    sealed class Label {
        data class ShowUndoSnackbar(val itemName: String) : Label()
    }

    private data class State(
        val mode: Mode = Mode.Packing,

        val lastDeleted: ObjectId? = null,
        val objectList: List<ObjectDto> = emptyList(),
    ) : Model {

        override val isProgressVisible get() = mode is Mode.Packing

        override val progress: Int
            get() {
                val all = objectList.size
                if (all == 0) {
                    return 0
                }
                val packed = objectList.count { it.isPacked }.toFloat()
                return (packed / all * 100).roundToInt()
            }

        override val isNewObjectViewVisible get() = mode is Mode.Edit

        override val items
            get() = objectList.map {
                ObjectItem(
                    it.id,
                    it.name,
                    it.isPacked,
                    mode is Mode.Edit,
                )
            }

        override val isEditButtonVisible get() = mode is Mode.Packing

        override val isCancelButtonVisible get() = mode is Mode.Edit

        sealed class Mode {

            object Packing : Mode()

            data class Edit(
                val addedObjectName: String = "",
            ) : Mode()
        }
    }

    interface Model {
        val isProgressVisible: Boolean
        val progress: Int

        val isNewObjectViewVisible: Boolean
        val items: List<ObjectItem>
        val isEditButtonVisible: Boolean
        val isCancelButtonVisible: Boolean
    }

    private inner class Executor :
        SuspendExecutor<Intent, Unit, State, Result, Label>(viewModelScope.coroutineContext) {

        override suspend fun executeAction(action: Unit, getState: () -> State) {
            viewModelScope.launch {
                observeObjects()
                    .collectLatest { list ->
                        updateState { copy(objectList = list) }
                    }
            }
        }

        override suspend fun executeIntent(intent: Intent, getState: () -> State) = when (intent) {
            Intent.EnterEditMode -> updateState {
                copy(mode = Mode.Edit())
            }
            is Intent.SetNewObjectName -> updateState {
                if (mode is Mode.Edit) {
                    copy(mode = mode.copy(addedObjectName = intent.name))
                } else {
                    this
                }
            }
            Intent.ConfirmAddingObject -> insertObject((getState().mode as Mode.Edit).addedObjectName)
            is Intent.MarkPacked -> setObjectPacked(intent.id, intent.isPacked)
            Intent.ExitEditMode -> updateState {
                copy(mode = Mode.Packing)
            }
            is Intent.Delete -> {
                updateState { copy(lastDeleted = intent.id) }
                val lastDeletedName = getState().objectList.single { it.id == intent.id }.name
                deleteObject(intent.id)
                publish(Label.ShowUndoSnackbar(lastDeletedName))
            }
            Intent.UndoDeleting -> undoDeleteObject(getState().lastDeleted!!)
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
class ObserveObjects(
    private val database: Database,
    private val context: CoroutineContext = Dispatchers.IO,
) {

    operator fun invoke() = database.objectQueries
        .selectObjects(::ObjectDto)
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
class InsertObject(private val executor: DatabaseExecutor) {

    suspend operator fun invoke(name: String) =
        executor.execute { objectQueries.insertObject(name) }
}

// TODO: Move
class SetObjectPacked(private val executor: DatabaseExecutor) {

    suspend operator fun invoke(id: ObjectId, isPacked: Boolean) =
        executor.execute { objectQueries.setObjectPacked(id, isPacked) }
}

// TODO: Move
class DeleteObject(private val executor: DatabaseExecutor) {

    suspend operator fun invoke(id: ObjectId) =
        executor.execute { objectQueries.deleteObject(id) }
}

// TODO: Move
class UndoDeleteObject(private val executor: DatabaseExecutor) {

    suspend operator fun invoke(id: ObjectId) =
        executor.execute { objectQueries.undoDeleteObject(id) }
}
