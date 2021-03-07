package net.chmielowski.baggage.`object`

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import net.chmielowski.baggage.`object`.ObjectListViewModel.Label.ShowUndoSnackbar
import net.chmielowski.baggage.ui.Database
import net.chmielowski.baggage.ui.createDatabase
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
internal class ObjectListViewModelTest {

    private val dispatcher = TestCoroutineDispatcher()

    init {
        // TODO: Rule
        Dispatchers.setMain(dispatcher)
        isAssertOnMainThreadEnabled = false
    }

    private val database = createTestDatabase()
        .apply { addDummyItem() }

    private val executor = DatabaseExecutor(database, dispatcher)

    private val viewModel = ObjectListViewModel(
        ObserveObjects(database, dispatcher),
        InsertObject(executor),
        SetObjectPacked(executor),
        DeleteObject(executor),
        UndoDeleteObject(executor),
    )

    private val dummyItemId = database.objectQueries.selectObjects()
        .executeAsList()
        .single { it.name == "Pants" }
        .id

    @Nested
    inner class `on EDIT clicked` {

        init {
            viewModel.onEnterEditModeClick()
        }

        @Test
        internal fun `new object view is visible`() = runBlockingTest(dispatcher) {
            assertThat(currentModel())
                .matches { it.isNewObjectViewVisible }
        }

        @Nested
        inner class `on CANCEL click` {

            init {
                viewModel.onExitEditModeClick()
            }

            @Test
            internal fun `new object view is not visible`() {
                runBlockingTest {
                    assertThat(currentModel())
                        .matches { !it.isNewObjectViewVisible }
                }
            }
        }

        @Nested
        inner class `on item name entered and confirmed adding` {

            init {
                // TODO: Count
                viewModel.onNewObjectNameChange("Socks")
                viewModel.onAddingNewItemConfirm()
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

        init {
            viewModel.onItemPackedToggle(dummyItemId, isPacked = true)
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
                viewModel.onItemPackedToggle(dummyItemId, isPacked = false)
            }

            @Test
            internal fun `it is not checked`() = runBlockingTest {
                assertThat(currentModel().items.single { it.name == "Pants" })
                    .matches { !it.isChecked }
            }
        }
    }

    @Nested
    inner class `on Delete icon clicked` {

        init {
            viewModel.onDeleteClick()
        }

        @Test
        internal fun `delete buttons are visible on each item`() = runBlockingTest {
            assertThat(currentModel().items)
                .allMatch { it.isDeleteVisible }
        }

        @Test
        internal fun `cancel deleting button is visible`() = runBlockingTest {
            assertThat(currentModel())
                .matches { it.isCancelDeletingVisible }
        }

        @Nested
        inner class `on delete item clicked` {

            init {
                viewModel.onDeleteItemClick(dummyItemId)
            }

            @Test
            internal fun `item is no longer present on the list`() = runBlockingTest {
                assertThat(currentModel().items)
                    .noneMatch { it.id == dummyItemId }
            }

            @Test
            internal fun `Undo message is displayed`() = runBlockingTest {
                assertThat(lastLabel())
                    .isEqualTo(ShowUndoSnackbar("Pants"))
            }

            @Nested
            inner class `on Undo click` {

                init {
                    viewModel.onUndoDeleteClick()
                }

                @Test
                internal fun `item is again present on the list`() = runBlockingTest {
                    assertThat(currentModel().items)
                        .anyMatch { it.id == dummyItemId }
                }
            }
        }

        @Nested
        inner class `on Cancel icon clicked` {

            init {
                viewModel.onCancelDeletingClick()
            }

            @Test
            internal fun `delete buttons are not visible on any item`() = runBlockingTest {
                assertThat(currentModel().items)
                    .allMatch { !it.isDeleteVisible }
            }

            @Test
            internal fun `delete button is visible`() = runBlockingTest {
                assertThat(currentModel())
                    .matches { it.isDeleteButtonVisible }
            }
        }
    }

    private suspend fun currentModel() = viewModel.observeModel().first()

    private suspend fun lastLabel() = viewModel.observeLabels().first()
}

private fun Database.addDummyItem() = objectQueries.insertObject("Pants")
