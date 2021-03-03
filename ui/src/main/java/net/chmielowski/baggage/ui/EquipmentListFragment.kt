package net.chmielowski.baggage.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.extensions.coroutines.SuspendExecutor
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.flow.Flow
import net.chmielowski.baggage.ui.databinding.ScreenEquipmentListBinding

class EquipmentListFragment : Fragment(R.layout.screen_equipment_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val model by viewModels<EquipmentListViewModel>()
        val binding = ScreenEquipmentListBinding.bind(view)
        val adapter = EquipmentAdapter()
        binding.list.adapter = adapter

        adapter.submitList((1..30).map { EquipmentItem(EquipmentId(it), "Item $it") })
    }
}

class EquipmentListViewModel : ViewModel() {

    private val storeFactory = DefaultStoreFactory

    private val store = storeFactory.create(
        name = "EquipmentListStore",
        initialState = State(),
        executorFactory = EquipmentListViewModel::Executor,
        reducer = ReducerImpl(),
    )

    fun onAddItemClick() {
    }

    fun observeModel(): Flow<Model> = store.states

    fun observeLabels() = store.labels

    sealed class Intent

    private sealed class Result

    sealed class Label

    private data class State(
        val equipmentList: List<EquipmentDto> = emptyList(),
    ) : Model {

        override val isInputVisible get() = true
    }

    interface Model {
        val isInputVisible: Boolean
    }

    private class Executor : SuspendExecutor<Intent, Nothing, State, Result, Label>()

    private class ReducerImpl : Reducer<State, Result> {

        override fun State.reduce(result: Result): State {
            return this
        }
    }
}

data class EquipmentDto(
    val id: EquipmentId,
    val name: String,
)
