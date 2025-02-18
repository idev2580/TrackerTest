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
import com.samsung.android.sdk.health.data.data.ChangeType
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

class BodyCompositionCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
): AbstractCollector<BodyCompositionCollector.Config, BodyCompositionCollector.Entity>(permissionManager
    , configStorage, stateStorage){

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

    private var lastSyncTimestamp:Long = -1
    private suspend fun readAllData(store:HealthDataStore):List<Entity>{
        val rTimestamp = System.currentTimeMillis()
        val timeFilter = InstantTimeFilter.since(Instant.ofEpochMilli(lastSyncTimestamp + 1))
        val req = DataTypes.BODY_COMPOSITION
            .changedDataRequestBuilder
            .setChangeTimeFilter(timeFilter)
            .build()
        val dataList = store.readChanges(req).dataList
        Log.d("BodyComposition", "To-sync data count=${dataList.size}, lastSyncTimestam$lastSyncTimestamp")

        val entityList:MutableList<Entity> = mutableListOf()
        var maxTimestamp:Long = lastSyncTimestamp
        dataList.forEach{
            if(it.changeType == ChangeType.UPSERT){
                //Update maxTimestamp
                if(it.changeTime.toEpochMilli() > maxTimestamp)
                    maxTimestamp = it.changeTime.toEpochMilli()

                val uid:String = it.upsertDataPoint.uid
                val timestamp:Long = it.upsertDataPoint.startTime.toEpochMilli()
                val bodyFatRatio:Float? = it.upsertDataPoint.getValue(DataType.BodyCompositionType.BODY_FAT)
                val weight:Float? = it.upsertDataPoint.getValue(DataType.BodyCompositionType.WEIGHT)
                val height:Float? = it.upsertDataPoint.getValue(DataType.BodyCompositionType.HEIGHT)
                val skeletalMuscleRatio:Float? = it.upsertDataPoint.getValue(DataType.BodyCompositionType.SKELETAL_MUSCLE)
                val totalBodyWater:Float? = it.upsertDataPoint.getValue(DataType.BodyCompositionType.TOTAL_BODY_WATER)
                val muscleMass:Float? = it.upsertDataPoint.getValue(DataType.BodyCompositionType.MUSCLE_MASS)
                //Actually, basalMetabolicRate should be derived from above values, but Samsung didn't opened their formula for basal metabolic rate.
                //Katch-McArdle formula shows the closest result to Samsung Health's one.
                val basalMetabolicRate:Float? = it.upsertDataPoint.getValue(DataType.BodyCompositionType.BASAL_METABOLIC_RATE)?.toFloat()

                val appId:String? = it.upsertDataPoint.dataSource?.appId
                val mDeviceId:String? = it.upsertDataPoint.dataSource?.deviceId

                entityList.add(
                    Entity(
                        rTimestamp,
                        uid,
                        timestamp,
                        bodyFatRatio?:Float.NaN,
                        weight?:Float.NaN,
                        height?:Float.NaN,
                        skeletalMuscleRatio?:Float.NaN,
                        totalBodyWater?:Float.NaN,
                        muscleMass?:Float.NaN,
                        basalMetabolicRate?:Float.NaN,
                        appId?:"UNKNOWN",
                        mDeviceId?:"UNKNOWN"
                    )
                )
            }
        }

        //Update lastSyncTimestamp
        lastSyncTimestamp = maxTimestamp
        return entityList
    }
    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            val store = HealthDataService.getStore(context)
            while(isActive){
                val timestamp = System.currentTimeMillis()
                Log.d("TAG", "BodyCompositionCollector: $timestamp")

                val readEntities = readAllData(store)
                readEntities.forEach{
                    listener?.invoke(it)
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

    data class Entity(
        override val received: Long,
        val uid : String,
        val timestamp : Long,
        val bodyFatRatio : Float,
        val weight : Float,
        val height : Float,
        val skeletalMuscleRatio : Float,
        val totalBodyWater : Float,
        val muscleMass : Float,
        val basalMetabolicRate : Float, //This should be calculated by above values, but samsung didn't share what formula they used.

        val appId:String,
        val mDeviceId:String,
    ) : DataEntity(received)
}