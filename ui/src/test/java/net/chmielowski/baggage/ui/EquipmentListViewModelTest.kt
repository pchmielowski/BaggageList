package net.chmielowski.baggage.ui

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import net.chmielowski.baggage.Equipment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// TODO: Move
private class SimpleAdapter<T : Any, S>(
    private val decoder: (S) -> T,
    private val encoder: (T) -> S
) : ColumnAdapter<T, S> {

    override fun decode(databaseValue: S) = decoder(databaseValue)

    override fun encode(value: T) = encoder(value)
}

@Suppress("ClassName")
internal class EquipmentListViewModelTest {

    private val dispatcher = TestCoroutineDispatcher()

    init {
        // TODO: Rule
        Dispatchers.setMain(dispatcher)
        isAssertOnMainThreadEnabled = false
    }

    private fun createTestDatabase(): Database {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        return Database(
            driver,
            Equipment.Adapter(
                SimpleAdapter(::EquipmentId, EquipmentId::value),
            )
        )
    }

    private val viewModel = EquipmentListViewModel(
        createTestDatabase()
    )

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

            @Test
            internal fun `item is present on the list`() = runBlockingTest {
                val model = viewModel.observeModel().first()
                assertThat(model.items)
                    .extracting<String> { it.name }
                    .contains("Socks")
            }
        }
    }
}
