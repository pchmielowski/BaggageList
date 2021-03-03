package net.chmielowski.baggage.ui

import android.app.Application
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

@Suppress("unused")
class BaggageApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            module {
                viewModel { EquipmentListViewModel(get()) }
            }
        }
    }
}
