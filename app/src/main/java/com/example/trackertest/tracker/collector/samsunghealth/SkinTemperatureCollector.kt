package com.example.trackertest.tracker.collector.samsunghealth

import android.content.Context
import android.health.connect.datatypes.SkinTemperatureRecord
import android.util.Log
import com.example.trackertest.tracker.collector.core.Availability
import com.example.trackertest.tracker.collector.core.CollectorConfig
import com.example.trackertest.tracker.collector.core.CollectorState
import com.example.trackertest.tracker.collector.core.DataEntity
import com.example.trackertest.tracker.data.SingletonStorageInterface
import com.example.trackertest.tracker.permission.PermissionManagerInterface
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.ChangeType
import com.samsung.android.sdk.health.data.data.entries.SkinTemperature
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class SkinTemperatureCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
): AbstractMeasurementSessionCollector<
        SkinTemperatureCollector.Config,
        SkinTemperatureCollector.MetadataEntity,
        SkinTemperatureCollector.Entity>(permissionManager, configStorage, stateStorage) {
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
        return Config::class
    }

    override fun getEntityClass(): KClass<out DataEntity> {
        return Entity::class
    }
    private var job: Job? = null

    var lastSyncTimestamp:Long = -1L
    suspend fun readAllData(store: HealthDataStore):List<Pair<MetadataEntity, List<Entity>>>{
        val timeFilter = InstantTimeFilter.since(Instant.ofEpochMilli(lastSyncTimestamp + 1))
        val req = DataTypes.SKIN_TEMPERATURE
            .changedDataRequestBuilder
            .setChangeTimeFilter(timeFilter)
            .build()

        lastSyncTimestamp = System.currentTimeMillis()
        val dataList = store.readChanges(req).dataList

        val retList:MutableList<Pair<MetadataEntity, List<Entity>>> = mutableListOf()
        dataList.forEach{
            if(it.changeType == ChangeType.UPSERT){
                val uid:String = it.upsertDataPoint.uid
                val startTime:Long = it.upsertDataPoint.startTime.toEpochMilli()
                val endTime:Long? = it.upsertDataPoint.endTime?.toEpochMilli()
                val skinTemperature:Float? = it.upsertDataPoint.getValue(DataType.SkinTemperatureType.SKIN_TEMPERATURE)
                val max:Float? = it.upsertDataPoint.getValue(DataType.SkinTemperatureType.MAX_SKIN_TEMPERATURE)
                val min:Float? = it.upsertDataPoint.getValue(DataType.SkinTemperatureType.MIN_SKIN_TEMPERATURE)

                val entityList:MutableList<Entity> = mutableListOf()
                val measurements:List<SkinTemperature>? = it.upsertDataPoint.getValue(DataType.SkinTemperatureType.SERIES_DATA)
                measurements?.forEach{
                    val m_uid:String = uid
                    val m_startTime:Long = it.startTime.toEpochMilli()
                    val m_endTime:Long = it.endTime.toEpochMilli()
                    val m_min:Float = it.min
                    val m_max:Float = it.max
                    val m_skinTemperature:Float = it.skinTemperature

                    entityList.add(Entity(
                        System.currentTimeMillis(),
                        m_uid,
                        m_startTime,
                        m_endTime,
                        m_min,
                        m_max,
                        m_skinTemperature
                    ))
                }

                retList.add(
                    Pair(
                        MetadataEntity(
                            System.currentTimeMillis(),
                            uid,
                            startTime,
                            endTime?:startTime,
                            skinTemperature?:Float.NaN
                        ),
                        entityList
                    )
                )
            }
        }
        return retList
    }

    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            val store = HealthDataService.getStore(context)
            while(isActive){
                val timestamp = System.currentTimeMillis()
                Log.d("TAG", "SkinTemperatureCollector: $timestamp")

                val dataList = readAllData(store)
                dataList.forEach{
                    var itemLogMsg:String = "Metadata=${it.first}, dataCount=${it.second.size}"
                    metadataListener?.invoke(it.first)
                    it.second.forEach{
                        listener?.invoke(it)
                        //itemLogMsg = "$itemLogMsg\n\tData=${it}"
                    }
                    Log.d("TAG", "SkinTemperatureCollector :\n$itemLogMsg")
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
        val skinTemperature: Float
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
        val skinTemperature : Float
    ) : DataEntity(received)
}