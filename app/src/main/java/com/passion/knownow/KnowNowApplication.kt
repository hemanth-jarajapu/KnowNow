package com.passion.knownow

import android.app.Application

class KnowNowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.passion.knownow.data.ServiceLocator.init(this)
    }
}