package net.chmielowski.baggage.ui

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("ClassName")
internal class EquipmentListViewModelTest {

    private val dispatcher = TestCoroutineDispatcher()

    init {
        Dispatchers.setMain(dispatcher)
        isAssertOnMainThreadEnabled = false
    }

    private val viewModel = EquipmentListViewModel()

    @Nested
    inner class `on Add Item clicked` {

        init {
            viewModel.onAddItemClick()
        }

        @Test
        internal fun `input is displayed`() = runBlockingTest(dispatcher) {
            val model = viewModel.observeModel().first()
            assertThat(model)
                .matches { it.isInputVisible }
        }

        @Nested
        inner class `on item name entered and confirmed adding` {

            init {
                viewModel.onNewItemNameEnter("Socks")
                viewModel.onAddingNewItemConfirm()
            }

            @Test
            internal fun `input is not displayed`() = runBlockingTest {
                val model = viewModel.observeModel().first()
                assertThat(model)
                    .matches { !it.isInputVisible }
            }
        }
    }
}
