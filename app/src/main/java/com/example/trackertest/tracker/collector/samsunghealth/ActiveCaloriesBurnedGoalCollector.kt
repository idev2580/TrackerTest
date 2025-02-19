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
import com.samsung.android.sdk.health.data.request.LocalDateGroup
import com.samsung.android.sdk.health.data.request.LocalDateGroupUnit
import com.samsung.android.sdk.health.data.request.Ordering
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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

    private val syncPastLimitDays:Long = 128
    private var latestGoalSetTime:Long = System.currentTimeMillis() - (syncPastLimitDays * 24L * 3600000L)
    private var latestGoal:Float = -2.0f

    private suspend fun readGoal(store: HealthDataStore, listener:((DataEntity)->Unit)?){
        val rTimestamp = System.currentTimeMillis()
        val req = DataType.ActiveCaloriesBurnedGoalType
            .LAST.requestBuilder
            .setOrdering(Ordering.DESC)
            .setLocalDateFilterWithGroup(
                LocalDateFilter.since(
                    Instant.ofEpochMilli(latestGoalSetTime)
                        .atZone(ZoneId.systemDefault())
                        .truncatedTo(ChronoUnit.DAYS)
                        .toLocalDate()
                ),
                LocalDateGroup.of(
                    LocalDateGroupUnit.DAILY,
                    1
                )
            ).build()
        val resList = store.aggregateData(req).dataList
        resList.forEach{
            val readValue:Float = it.value?.toFloat() ?: -2.0f
            val isDefaultGoal:Boolean = (readValue == ActiveCaloriesBurnedGoalCollector.defaultGoal)
            val goalValue:Float = if(isDefaultGoal) -1.0f else readValue

            if(goalValue != latestGoal && latestGoalSetTime <= it.startTime.toEpochMilli()){
                latestGoal = goalValue
                latestGoalSetTime = it.startTime.toEpochMilli()
                listener?.invoke(
                    Entity(
                        rTimestamp,
                        latestGoal,
                        latestGoalSetTime
                    )
                )
                Log.d("ActiveCaloriesBurnedGoalCollector", "${it.startTime}~${it.endTime}, $goalValue")
            }
        }
        Log.d("ActiveCaloriesBurnedGoalCollector", "latestGoalSetTime = ${Instant.ofEpochMilli(latestGoalSetTime).atZone(ZoneId.systemDefault()).toLocalDateTime()}")
    }
    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            //TODO: Should get latest goal's setTime
            //TODO: store should be passed from outside, not getting here.
            val store = HealthDataService.getStore(context)
            while(isActive){
                val timestamp = System.currentTimeMillis()
                readGoal(store, listener)
                Log.d("ActiveCaloriesBurnedGoalCollector", "Synced at $timestamp")
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