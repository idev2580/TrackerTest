package com.example.trackertest.tracker.collector.samsunghealth

import android.content.Context
import android.util.Log
import com.example.trackertest.tracker.collector.core.AbstractCollector
import com.example.trackertest.tracker.collector.core.Availability
import com.example.trackertest.tracker.collector.core.CollectorConfig
import com.example.trackertest.tracker.collector.core.CollectorState
import com.example.trackertest.tracker.collector.core.DataEntity
import com.example.trackertest.tracker.data.SingletonStorageInterface
import com.example.trackertest.tracker.permission.PermissionManagerInterface
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class BloodOxygenCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
): AbstractMeasurementSessionCollector<
        BloodOxygenCollector.Config,
        BloodOxygenCollector.MetadataEntity,
        BloodOxygenCollector.Entity
    >(permissionManager, configStorage, stateStorage){
    companion object{
        val defaultConfig = Config(
            TimeUnit.SECONDS.toMillis(60)
        )
    }
    override val _defaultConfig = defaultConfig

    override val permissions = listOfNotNull<String>(
        //TODO : Permission
    ).toTypedArray()
    override val foregroundServiceTypes: Array<Int> = listOfNotNull<Int>().toTypedArray()

    data class Config(
        val interval: Long,
    ) : CollectorConfig {
        override fun copy(property: String, setValue:String): Config {
            return when(property){
                "interval" -> this.copy(interval = setValue.toLong())
                else -> error("Unknown property $property")
            }
        }
    }

    override fun getConfigClass(): KClass<out CollectorConfig> {
        return com.example.trackertest.tracker.collector.samsunghealth.BodyCompositionCollector.Config::class
    }

    override fun getEntityClass(): KClass<out DataEntity> {
        return Entity::class
    }

    private var job: Job? = null

    var lastSyncTimestamp:Long = -1L
    suspend fun readAllData(store: HealthDataStore):List<Pair<MetadataEntity, List<Entity>>>{
        //TODO
        return listOf()
    }

    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            val store = HealthDataService.getStore(context)
            while(isActive){
                val timestamp = System.currentTimeMillis()
                Log.d("TAG", "BloodOxygenCollector: $timestamp")

                val dataList = readAllData(store)
                dataList.forEach{
                    metadataListener?.invoke(it.first)
                    it.second.forEach{
                        listener?.invoke(it)
                    }
                }
                sleep(configFlow.value.interval)
            }
        }
    }
    override fun stop() {
        Log.d(NAME, "STOP()")
        job?.cancel()
        job = null
        super.stop()
    }

    override fun isAvailable() = Availability(true)

    data class MetadataEntity(
        override val received: Long,
        val uid : String,
        val startTime : Long,
        val endTime : Long,
        val oxygenSaturation: Float,
        val measurementType: String
        //val appId: String,
        //val deviceId: String
    ) : DataEntity(received)

    data class Entity(
        override val received: Long,
        val uid : String,
        val startTime : Long,
        val endTime : Long,
        val min : Float,
        val max : Float,
        val oxygenSaturation : Float
    ) : DataEntity(received)
}