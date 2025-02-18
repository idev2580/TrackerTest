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

class StepGoalCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
) : AbstractCollector<StepGoalCollector.Config, StepGoalCollector.Entity>(permissionManager
    , configStorage, stateStorage) {

    companion object{
        val defaultConfig = Config(
            TimeUnit.SECONDS.toMillis(60)
        )
        val defaultGoal:Long = 6000
    }
    override val _defaultConfig = StepGoalCollector.defaultConfig

    override val permissions = listOfNotNull<String>(
        //TODO : Permission
    ).toTypedArray()
    override val foregroundServiceTypes: Array<Int> = listOfNotNull<Int>().toTypedArray()

    data class Config(
        val interval: Long,
    ) : CollectorConfig {
        override fun copy(property: String, setValue:String):Config {
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

    //At now, skip reading latestGoal data and just use member variable to track them
    private var latestGoalSetTime:Long = -1
    private var latestGoal:Long = -2    //-1 is default goal. To track even default goal at first, it should not be -1.

    private suspend fun readGoal(store: HealthDataStore):Entity?{
        val rTimestamp = System.currentTimeMillis()
        val req = DataType.StepsGoalType
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
            val goal:Long = resList.first().value!!.toLong()
            val isDefaultGoal = (goal == StepGoalCollector.defaultGoal)
            val recordGoal:Long = if(isDefaultGoal) -1 else goal

            if(latestGoal != recordGoal) {
                latestGoalSetTime = System.currentTimeMillis()
                latestGoal = recordGoal

                Log.d(
                    "StepGoalCollector",
                    "latestGoalSetTime=$latestGoalSetTime, goal=$recordGoal, isDefaultGoal=$isDefaultGoal"
                )
                return Entity(
                    rTimestamp,
                    recordGoal,
                    latestGoalSetTime
                )
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

                val readEntity = readGoal(store)
                if(readEntity != null){
                    listener?.invoke(
                        readEntity
                    )
                }
                Log.d("StepGoalCollector", "Synced at $timestamp")
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
        val steps: Long,
        val goalSetTime: Long,
    ) : DataEntity(received)
}