package com.kirtigames.abcd

import android.app.Application

class AbcdApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Load letters before any screen needs them (also after Android restarts the app).
        Content.load(assets)
        Art.load(assets)
        // Only the Voice Studio build also plays takes recorded on this phone.
        Voice.load(assets, if (resources.getBoolean(R.bool.studio_build)) getExternalFilesDir("voice") else null)
    }
}
