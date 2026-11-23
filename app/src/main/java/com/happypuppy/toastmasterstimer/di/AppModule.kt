package com.happypuppy.toastmasterstimer.di

import androidx.room.Room
import com.happypuppy.toastmasterstimer.data.database.repository.AppDatabase
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimerViewModel
import com.happypuppy.toastmasterstimer.ui.screens.TimerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Room database: only allow destructive fallback from legacy schema v1; v2+ requires explicit Migration.
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
            .build()
    }

    single { get<AppDatabase>().speakerDao() }
    single { SpeakerRepository(get()) }

    viewModelOf(::SpeechTimerViewModel)
    viewModel { TimerViewModel() }

    single { DataStoreManager(androidContext()) }
}
