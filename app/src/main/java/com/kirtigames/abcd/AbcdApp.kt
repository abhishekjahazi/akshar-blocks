package com.kirtigames.abcd

import android.app.Application

class AbcdApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Load letters before any screen needs them (also after Android restarts the app).
        Content.load(assets)
        Art.load(assets)
        Voice.load(assets)
    }
}
