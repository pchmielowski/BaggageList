package net.chmielowski.baggage.ui

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EquipmentListViewModelTest {

    @BeforeEach
    internal fun setUp() {
        Dispatchers.setMain(TestCoroutineDispatcher())
        isAssertOnMainThreadEnabled = false
    }

    @Test
    internal fun `basic test`() {
        val model = EquipmentListViewModel()
    }
}
