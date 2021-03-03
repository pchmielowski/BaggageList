package net.chmielowski.baggage.ui

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EquipmentListViewModelTest {

    private val dispatcher = TestCoroutineDispatcher()

    init {
        Dispatchers.setMain(dispatcher)
        isAssertOnMainThreadEnabled = false
    }

    private val viewModel = EquipmentListViewModel()

    @Test
    internal fun `on Add Item clicked, input is displayed`() = runBlockingTest(dispatcher) {
        viewModel.onAddItemClick()

        val model = viewModel.observeModel().first()
        assertThat(model)
            .matches { it.isInputVisible }
    }
}
