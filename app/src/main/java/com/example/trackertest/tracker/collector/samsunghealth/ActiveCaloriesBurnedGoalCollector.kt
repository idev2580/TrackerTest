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
import com.samsung.android.sdk.health.data.request.DataType
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

class ActiveCaloriesBurnedGoalCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
): AbstractCollector<ActiveCaloriesBurnedGoalCollector.Config, ActiveCaloriesBurnedGoalCollector.Entity>(permissionManager
    , configStorage, stateStorage){

    companion object{
        val defaultConfig = Config(
            TimeUnit.SECONDS.toMillis(60)
        )
        val defaultGoal:Float = 500.0f
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

    var latestGoalSetTime:Long = -1
    var latestGoal:Float = -2.0f

    suspend fun readGoal(store: HealthDataStore): Entity?{
        val rTimestamp = System.currentTimeMillis()
        val req = DataType.ActiveCaloriesBurnedGoalType
            .LAST.requestBuilder
            .setOrdering(Ordering.DESC)
            .setLocalDateFilter(
                LocalDateFilter.since(
                    Instant.ofEpochMilli(
                        if(latestGoalSetTime != -1L) latestGoalSetTime else 0
                    )
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                )
            ).build()
        val resList = store.aggregateData(req).dataList
        if(resList.isNotEmpty()){
            val goal:Float? = resList.first().value?.toFloat()
            if(goal != null){
                val recordGoal = if (goal == defaultGoal) -1.0f else goal
                if(recordGoal != latestGoal){
                    Log.d("ActiveCaloriesBurnedGoalCollector", "latestGoalSetTime=$latestGoalSetTime, recordGoal=$recordGoal, latestGoal=$latestGoal")
                    latestGoalSetTime = System.currentTimeMillis()
                    latestGoal = recordGoal

                    return Entity(
                        rTimestamp,
                        recordGoal,
                        latestGoalSetTime
                    )
                }
            }
        }
        return null
    }
    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            //TODO: Should get latest goal's setTime
            //TODO: store should be passed from outside, not getting here.
            val store = HealthDataService.getStore(context)
            while(isActive){
                val timestamp = System.currentTimeMillis()
                Log.d("ActiveCaloriesBurnedGoalCollector", "Synced at $timestamp")

                val readEntity = readGoal(store)
                if(readEntity != null){
                    listener?.invoke(
                        readEntity
                    )
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
        val activeCaloriesBurned: Float,
        val goalSetTime: Long,
    ) : DataEntity(received)
}