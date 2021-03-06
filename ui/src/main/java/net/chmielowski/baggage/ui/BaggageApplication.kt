package net.chmielowski.baggage.ui

import android.app.Application
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import net.chmielowski.baggage.Object_
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
                    factory { DatabaseExecutor(get()) }

                    factory { ObserveObjects(get()) }
                    factory { InsertObject(get()) }
                    factory { SetObjectPacked(get()) }
                    factory { DeleteObject(get()) }
                    factory { UndoDeleteObject(get()) }

                    viewModel { ObjectListViewModel(get(), get(), get(), get(), get()) }
                }
            )
        }
    }
}

fun createDatabase(driver: SqlDriver) =
    Database(
        driver,
        Object_.Adapter(
            SimpleAdapter(::ObjectId, ObjectId::value),
        )
    )

private class SimpleAdapter<T : Any, S>(
    private val decoder: (S) -> T,
    private val encoder: (T) -> S
) : ColumnAdapter<T, S> {

    override fun decode(databaseValue: S) = decoder(databaseValue)

    override fun encode(value: T) = encoder(value)
}
