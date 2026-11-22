package com.happypuppy.toastmasterstimer

import android.app.Application
import android.content.pm.ApplicationInfo
import com.happypuppy.toastmasterstimer.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        startKoin {
            androidContext(this@MainApp)
            if (isDebuggable) {
                androidLogger(Level.INFO)
            }
            modules(appModule)
        }
    }
}
