package com.example.trackertest.tracker.collector.samsunghealth

import android.content.Context
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
import com.samsung.android.sdk.health.data.data.entries.OxygenSaturation
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
        return Config::class
    }

    override fun getEntityClass(): KClass<out DataEntity> {
        return Entity::class
    }

    override fun getMetadataEntityClass(): KClass<out DataEntity> {
        return MetadataEntity::class
    }

    private var job: Job? = null

    private var lastSyncTimestamp:Long = -1L
    private suspend fun readAllData(store: HealthDataStore):List<Pair<MetadataEntity, List<Entity>>>{
        val rTimestamp = System.currentTimeMillis()
        val timeFilter = InstantTimeFilter.since(Instant.ofEpochMilli(lastSyncTimestamp + 1))
        val req = DataTypes.BLOOD_OXYGEN
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
                val appId:String? = it.upsertDataPoint.dataSource?.appId
                val mDeviceId:String? = it.upsertDataPoint.dataSource?.deviceId

                val oxygenSaturation:Float? = it.upsertDataPoint.getValue(DataType.BloodOxygenType.OXYGEN_SATURATION)
                val max:Float? = it.upsertDataPoint.getValue(DataType.BloodOxygenType.MAX_OXYGEN_SATURATION)
                val min:Float? = it.upsertDataPoint.getValue(DataType.BloodOxygenType.MIN_OXYGEN_SATURATION)

                val entityList:MutableList<Entity> = mutableListOf()
                val measurements:List<OxygenSaturation>? = it.upsertDataPoint.getValue(DataType.BloodOxygenType.SERIES_DATA)
                measurements?.forEach{
                    val m_uid:String = uid
                    val m_startTime:Long = it.startTime.toEpochMilli()
                    val m_endTime:Long = it.endTime.toEpochMilli()
                    val m_min:Float = it.min
                    val m_max:Float = it.max
                    val m_oxygenSaturation:Float = it.oxygenSaturation

                    entityList.add(Entity(
                        rTimestamp,
                        m_uid,
                        m_startTime,
                        m_endTime,
                        m_min,
                        m_max,
                        m_oxygenSaturation
                    ))
                }

                retList.add(
                    Pair(
                        MetadataEntity(
                            rTimestamp,
                            uid,
                            startTime,
                            endTime?:startTime,
                            oxygenSaturation ?:Float.NaN,
                            appId?:"UNKNOWN",
                            mDeviceId?:"UNKNOWN"
                        ), entityList
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

                val dataList = readAllData(store)
                dataList.forEach{
                    var itemLogMsg:String = "Metadata=${it.first}, dataCount=${it.second.size}"
                    metadataListener?.invoke(it.first)
                    it.second.forEach{
                        listener?.invoke(it)
                        //itemLogMsg = "$itemLogMsg\n\tData=${it}"
                    }
                    //Log.d("TAG", "BloodOxygenCollector :\n$itemLogMsg")
                }
                Log.d("BloodOxygenCollector", "Synced at $timestamp")
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
        val appId: String,
        val mDeviceId: String
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