package com.example.jpec.pokeguess

import android.app.Application
import android.content.Context
import android.content.res.Configuration

class App : Application() {

    companion object {
        lateinit var localeHelper : LocaleHelper
    }

    override fun attachBaseContext(newBase: Context?) {
        localeHelper = LocaleHelper(newBase)
        super.attachBaseContext(localeHelper.setLocale(newBase))
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        localeHelper.setLocale(this)
    }
}