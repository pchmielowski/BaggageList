package net.chmielowski.baggage.ui

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// TODO: Move
private fun createTestDatabase(): Database {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    Database.Schema.create(driver)
    return createDatabase(driver)
}

@Suppress("ClassName")
internal class EquipmentListViewModelTest {

    private val dispatcher = TestCoroutineDispatcher()

    init {
        // TODO: Rule
        Dispatchers.setMain(dispatcher)
        isAssertOnMainThreadEnabled = false
    }

    private val database = createTestDatabase()
        .apply { addDummyItem() }

    private val viewModel = EquipmentListViewModel(database)

    @Nested
    inner class `on Add Item clicked` {

        init {
            viewModel.onAddItemClick()
        }

        @Test
        internal fun `input is displayed`() = runBlockingTest(dispatcher) {
            assertThat(currentModel())
                .matches { it.isInputVisible }
        }

        @Nested
        inner class `on cancelled` {

            init {
                viewModel.onCancelAddingClick()
            }

            @Test
            internal fun `input is not visible`() {
                runBlockingTest {
                    assertThat(currentModel())
                        .matches { !it.isInputVisible }
                }
            }
        }

        @Nested
        inner class `on item name entered and confirmed adding` {

            init {
                viewModel.onNewItemNameEnter("Socks")
                viewModel.onAddingNewItemConfirm()
            }

            @Test
            internal fun `input is not displayed`() = runBlockingTest {
                assertThat(currentModel())
                    .matches { !it.isInputVisible }
            }

            @Test
            internal fun `item is present on the list`() = runBlockingTest {
                assertThat(currentModel().items)
                    .extracting<String> { it.name }
                    .contains("Socks")
            }
        }
    }

    @Nested
    inner class `on dummy item checked as packed` {

        private val id = database.equipmentQueries.selectEquipments()
            .executeAsList()
            .single { it.name == "Pants" }
            .id

        init {
            viewModel.onItemPackedToggle(id, isPacked = true)
        }

        @Test
        internal fun `it is checked`() = runBlockingTest {
            assertThat(currentModel().items.single { it.name == "Pants" })
                .matches { it.isChecked }
        }

        @Test
        internal fun `progress is set to 100`() = runBlockingTest {
            assertThat(currentModel())
                .matches { it.progress == 100 }
        }

        @Nested
        inner class `on checked as not packed` {

            init {
                viewModel.onItemPackedToggle(id, isPacked = false)
            }

            @Test
            internal fun `it is not checked`() = runBlockingTest {
                assertThat(currentModel().items.single { it.name == "Pants" })
                    .matches { !it.isChecked }
            }
        }
    }

    private suspend fun currentModel() = viewModel.observeModel().first()
}

private fun Database.addDummyItem() = equipmentQueries.insertEquimpent("Pants")
