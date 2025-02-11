package com.example.trackertest.tracker.collector.samsunghealth

import android.content.Context
import android.util.Log
import com.example.trackertest.tracker.collector.core.AbstractCollector
import com.example.trackertest.tracker.collector.core.Availability
import com.example.trackertest.tracker.collector.core.CollectorConfig
import com.example.trackertest.tracker.collector.core.CollectorState
import com.example.trackertest.tracker.collector.core.DataEntity
import com.example.trackertest.tracker.collector.samsunghealth.BloodPressureCollector.Entity
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
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class WaterIntakeCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
): AbstractCollector<WaterIntakeCollector.Config, WaterIntakeCollector.Entity>(permissionManager
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
        val timeFilter = InstantTimeFilter.since(Instant.ofEpochMilli(lastSyncTimestamp + 1))
        val req = DataTypes.WATER_INTAKE
            .changedDataRequestBuilder
            .setChangeTimeFilter(timeFilter)
            .build()
        val dataList = store.readChanges(req).dataList
        Log.d("TAG", "WaterIntake : To-sync data count=${dataList.size}, lastSyncTimestamp=$lastSyncTimestamp")

        if(dataList.isEmpty()){
            //No data to sync -> All data synced, change lastSyncTimestamp
            lastSyncTimestamp = System.currentTimeMillis()
            return null
        }
        //There are at least one or more data to sync
        // -> Sync them one-by-one. (See conditional sleep call in while loop of start() method)
        val minItem = dataList.reduce{
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
            val amount:Float? = minItem.upsertDataPoint.getValue(DataType.WaterIntakeType.AMOUNT)

            return Entity(
                System.currentTimeMillis(),
                uid,
                timestamp,
                amount?:Float.NaN
            )
        }
        return null
    }
    suspend fun readAllData(store:HealthDataStore):List<Entity>{
        //Sync all data from samsung health data SDK at once, using list.
        //For better performance and battery time, this should be used instead of above one.
        val timeFilter = InstantTimeFilter.since(Instant.ofEpochMilli(lastSyncTimestamp + 1))
        val req = DataTypes.WATER_INTAKE
            .changedDataRequestBuilder
            .setChangeTimeFilter(timeFilter)
            .build()
        val dataList = store.readChanges(req).dataList
        Log.d("TAG", "WaterIntake : To-sync data count=${dataList.size}, lastSyncTimestamp=$lastSyncTimestamp")

        val entityList:MutableList<Entity> = mutableListOf()
        var maxTimestamp:Long = lastSyncTimestamp
        dataList.forEach{
            if(it.changeType == ChangeType.UPSERT){
                //Update maxTimestamp
                if(it.changeTime.toEpochMilli() > maxTimestamp)
                    maxTimestamp = it.changeTime.toEpochMilli()

                val uid:String = it.upsertDataPoint.uid
                val timestamp:Long = it.upsertDataPoint.startTime.toEpochMilli()
                val amount:Float? = it.upsertDataPoint.getValue(DataType.WaterIntakeType.AMOUNT)
                entityList.add(
                    Entity(
                        System.currentTimeMillis(),
                        uid,
                        timestamp,
                        amount?:Float.NaN
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
            //TODO: Should get latest goal's setTime
            //TODO: store should be passed from outside, not getting here.
            val store = HealthDataService.getStore(context)
            while(isActive){
                val timestamp = System.currentTimeMillis()
                Log.d("TAG", "WaterIntakeCollector: $timestamp")

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
        val amount : Float,
    ) : DataEntity(received)
}