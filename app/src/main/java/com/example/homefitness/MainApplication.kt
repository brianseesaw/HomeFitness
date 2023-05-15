package com.example.homefitness

import android.app.Application
import com.example.homefitness.di.Modules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin{
            androidLogger()
            androidContext(this@MainApplication)
            modules(
                Modules.databaseModule,
                Modules.daosModule,
                Modules.repositoryModule,
                Modules.classifier,
                Modules.alertPlayer,
                Modules.viewModels
            )
        }
    }
}