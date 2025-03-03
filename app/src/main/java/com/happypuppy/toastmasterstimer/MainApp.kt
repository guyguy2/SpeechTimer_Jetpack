package com.happypuppy.toastmasterstimer

import android.app.Application
import com.happypuppy.toastmasterstimer.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level


class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MainApp)
            androidLogger(Level.INFO)
            modules(appModule)
        }
    }
}

