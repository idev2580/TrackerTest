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
import com.samsung.android.sdk.health.data.request.LocalDateFilter
import com.samsung.android.sdk.health.data.request.Ordering
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class BloodPressureCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
): AbstractCollector<BloodPressureCollector.Config, BloodPressureCollector.Entity>(permissionManager
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

    var lastSyncTimestamp:Long = -1
    suspend fun readData(store: HealthDataStore): Entity?{
        //Why changedDataRequestBuilder?
        //-> In samsung health, the user can insert data for 'past'.
        //   If there is trigger-like API, then we can sync using that, but there is no similar thing in Samsung Health data SDK
        //   (It's very strange because its predecessor(Samsung Health SDK for Android) has trigger-like API)
        //   So, to synchronize, we need to use 'When the data inserted' as filter rather than 'When the data represent'
        val timeFilter = InstantTimeFilter.since(Instant.ofEpochMilli(lastSyncTimestamp + 1))
        val req = DataTypes.BLOOD_PRESSURE
            .changedDataRequestBuilder
            .setChangeTimeFilter(timeFilter)
            .build()
        val bloodPressureList = store.readChanges(req).dataList
        Log.d("TAG", "BloodPressure : To-sync data count=${bloodPressureList.size}, lastSyncTimestamp=$lastSyncTimestamp")

        if(bloodPressureList.isEmpty()){
            //No data to sync -> All data synced, change lastSyncTimestamp
            lastSyncTimestamp = System.currentTimeMillis()
            return null
        }

        //There are at least one or more data to sync
        // -> Sync them one-by-one. (See conditional sleep call in while loop of start() method)
        val minItem = bloodPressureList.reduce{
            minItem, item ->
                if (item.changeTime.toEpochMilli() < minItem.changeTime.toEpochMilli())
                    item
                else
                    minItem
        }
        lastSyncTimestamp = minItem.changeTime.toEpochMilli()
        if(minItem.changeType == ChangeType.UPSERT){
            val uid:String = minItem.upsertDataPoint.uid
            val timestamp:Long = minItem.upsertDataPoint.startTime.toEpochMilli()
            val diastolic:Float? = minItem.upsertDataPoint.getValue(DataType.BloodPressureType.DIASTOLIC)
            val systolic:Float? = minItem.upsertDataPoint.getValue(DataType.BloodPressureType.SYSTOLIC)
            val pulseRate:Int? = minItem.upsertDataPoint.getValue(DataType.BloodPressureType.PULSE_RATE)
            val medicationTaken:Boolean? = minItem.upsertDataPoint.getValue(DataType.BloodPressureType.MEDICATION_TAKEN)

            val appId:String? = minItem.upsertDataPoint.dataSource?.appId
            val mDeviceId:String? = minItem.upsertDataPoint.dataSource?.deviceId

            return Entity(
                System.currentTimeMillis(),
                uid,
                timestamp,
                diastolic?:Float.NaN,
                systolic?:Float.NaN,
                pulseRate?:-1,
                medicationTaken?:false,
                appId?:"UNKNOWN",
                mDeviceId?:"UNKNOWN"
            )
        }
        return null
    }
    suspend fun readAllData(store:HealthDataStore):List<Entity>{
        //Sync all data from samsung health data SDK at once, using list.
        //For better performance and battery time, this should be used instead of above one.
        val timeFilter = InstantTimeFilter.since(Instant.ofEpochMilli(lastSyncTimestamp + 1))
        val req = DataTypes.BLOOD_PRESSURE
            .changedDataRequestBuilder
            .setChangeTimeFilter(timeFilter)
            .build()
        val dataList = store.readChanges(req).dataList
        Log.d("TAG", "BloodPressure : To-sync data count=${dataList.size}, lastSyncTimestamp=$lastSyncTimestamp")

        val entityList:MutableList<Entity> = mutableListOf()
        var maxTimestamp:Long = lastSyncTimestamp
        dataList.forEach{
            if(it.changeType == ChangeType.UPSERT){
                //Update maxTimestamp
                if(it.changeTime.toEpochMilli() > maxTimestamp)
                    maxTimestamp = it.changeTime.toEpochMilli()

                val uid:String = it.upsertDataPoint.uid
                val timestamp:Long = it.upsertDataPoint.startTime.toEpochMilli()
                val diastolic:Float? = it.upsertDataPoint.getValue(DataType.BloodPressureType.DIASTOLIC)
                val systolic:Float? = it.upsertDataPoint.getValue(DataType.BloodPressureType.SYSTOLIC)
                val pulseRate:Int? = it.upsertDataPoint.getValue(DataType.BloodPressureType.PULSE_RATE)
                val medicationTaken:Boolean? = it.upsertDataPoint.getValue(DataType.BloodPressureType.MEDICATION_TAKEN)

                val appId:String? = it.upsertDataPoint.dataSource?.appId
                val mDeviceId:String? = it.upsertDataPoint.dataSource?.deviceId

                entityList.add(
                    Entity(
                        System.currentTimeMillis(),
                        uid,
                        timestamp,
                        diastolic?:Float.NaN,
                        systolic?:Float.NaN,
                        pulseRate?:-1,
                        medicationTaken?:false,
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
            //TODO: store should be passed from outside, not getting here.
            val store = HealthDataService.getStore(context)
            while(isActive){
                val timestamp = System.currentTimeMillis()
                Log.d("TAG", "BloodPressureCollector: $timestamp")

                val readEntities = readAllData(store)
                readEntities.forEach{
                    listener?.invoke(it)
                }
                sleep(configFlow.value.interval)
                /*val readEntity = readData(store)
                if(readEntity != null){
                    listener?.invoke(
                        readEntity
                    )
                }
                if(lastSyncTimestamp >= timestamp)
                    sleep(configFlow.value.interval)*/
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
        val diastolic : Float,
        val systolic : Float,
        val pulseRate : Int,
        val medicationTaken: Boolean,
        val appId:String,
        val mDeviceId:String,
    ) : DataEntity(received)
}