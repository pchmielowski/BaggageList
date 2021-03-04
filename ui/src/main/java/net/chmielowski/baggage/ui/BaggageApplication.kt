package net.chmielowski.baggage.ui

import android.app.Application
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import net.chmielowski.baggage.Equipment
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

@Suppress("unused")
class BaggageApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(
                module {
                    androidContext(this@BaggageApplication)
                    single {
                        createDatabase(
                            AndroidSqliteDriver(Database.Schema, androidContext(), "main.db")
                        )
                    }
                    factory { ObserveEquipments(get()) }
                    factory { InsertEquipment(get()) }
                    factory { SetEquipmentPacked(get()) }
                    viewModel { EquipmentListViewModel(get(), get(), get()) }
                }
            )
        }
    }
}

fun createDatabase(driver: SqlDriver) =
    Database(
        driver,
        Equipment.Adapter(
            SimpleAdapter(::EquipmentId, EquipmentId::value),
        )
    )

private class SimpleAdapter<T : Any, S>(
    private val decoder: (S) -> T,
    private val encoder: (T) -> S
) : ColumnAdapter<T, S> {

    override fun decode(databaseValue: S) = decoder(databaseValue)

    override fun encode(value: T) = encoder(value)
}
