package com.happypuppy.toastmasterstimer.di

import androidx.room.Room
import com.happypuppy.toastmasterstimer.data.database.repository.AppDatabase
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

// Create a Koin module
val appModule = module {
    // Room database with migration strategy
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration() // For simplicity in development
            .build()
    }

    // DAO
    single { get<AppDatabase>().speakerDao() }

    // Repository
    single { SpeakerRepository(get()) }

    // ViewModel
    viewModelOf(::SpeechTimerViewModel)

    single { DataStoreManager(androidContext()) }
}