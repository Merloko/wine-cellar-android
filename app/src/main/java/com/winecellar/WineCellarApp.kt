package com.winecellar

import android.app.Application
import com.winecellar.data.WineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WineCellarApp : Application() {

    val repository: WineRepository by lazy { WineRepository.from(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Load the bundled starter cellar on first launch. No-op afterwards.
        appScope.launch { repository.seedIfEmpty(this@WineCellarApp) }
    }
}
