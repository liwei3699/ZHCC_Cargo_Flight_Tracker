package com.zzcargo.flightboard

import android.app.Application
import android.content.Context
import com.zzcargo.flightboard.data.cache.SnapshotCache
import com.zzcargo.flightboard.data.remote.ZzcargoApi
import com.zzcargo.flightboard.data.repository.DefaultFlightRepository
import com.zzcargo.flightboard.data.repository.FlightRepository
import com.zzcargo.flightboard.export.ExportManager
import com.zzcargo.flightboard.export.FlightExporter
import java.io.File

class FlightApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val cache = SnapshotCache(File(context.filesDir, "flight_snapshot.json"))
    private val api = ZzcargoApi()

    val repository: FlightRepository = DefaultFlightRepository(api, cache)
    val exporter: FlightExporter = ExportManager(context.applicationContext)
}
