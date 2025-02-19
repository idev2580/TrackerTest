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

class SleepGoalCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
) : AbstractCollector<SleepGoalCollector.Config, SleepGoalCollector.Entity>(permissionManager
    , configStorage, stateStorage) {

    companion object{
        val defaultConfig = Config(
            TimeUnit.SECONDS.toMillis(60)
        )
    }
    override val _defaultConfig = SleepGoalCollector.defaultConfig

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

    private val syncPastLimitDays:Long = 128
    private var latestGoalSetTime:Long = System.currentTimeMillis() - (syncPastLimitDays * 24L * 3600000L)
    private var latestBedTimeGoal:Long = -2
    private var latestWakeUpTimeGoal:Long = -2

    private suspend fun readGoal(store: HealthDataStore, listener:((DataEntity)->Unit)?){
        val rTimestamp = System.currentTimeMillis()
        val timeFilter = LocalDateFilter.since(
            Instant.ofEpochMilli(
                latestGoalSetTime
            )
                .atZone(ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.DAYS)
                .toLocalDate()
        )
        val timeGroup = LocalDateGroup.of(
            LocalDateGroupUnit.DAILY,
            1
        )
        val breq = DataType.SleepGoalType
            .LAST_BED_TIME.requestBuilder
            .setOrdering(Ordering.ASC)
            .setLocalDateFilterWithGroup(timeFilter, timeGroup).build()
        val bresArray = store.aggregateData(breq).dataList.toTypedArray()

        val wreq = DataType.SleepGoalType
            .LAST_WAKE_UP_TIME.requestBuilder
            .setOrdering(Ordering.ASC)
            .setLocalDateFilterWithGroup(timeFilter, timeGroup).build()
        val wresArray = store.aggregateData(wreq).dataList.toTypedArray()

        if(bresArray.size == wresArray.size){
            for(i in 0 until bresArray.size){
                val readBedtime:Long = bresArray[i].value?.toSecondOfDay()?.toLong() ?:-2L
                val readWakeupTime:Long = wresArray[i].value?.toSecondOfDay()?.toLong() ?: -2L

                if((readBedtime != latestBedTimeGoal || readWakeupTime != latestWakeUpTimeGoal)
                    && latestGoalSetTime <= bresArray[i].startTime.toEpochMilli()){

                    latestBedTimeGoal = readBedtime
                    latestWakeUpTimeGoal = readWakeupTime
                    latestGoalSetTime = bresArray[i].startTime.toEpochMilli()
                    listener?.invoke(
                        Entity(
                            rTimestamp,
                            latestBedTimeGoal,
                            latestWakeUpTimeGoal,
                            latestGoalSetTime
                        )
                    )
                    Log.d(
                        "SleepGoalCollector",
                        "${bresArray[i].startTime}~${bresArray[i].endTime}, bedTimeGoal=$latestBedTimeGoal, wakeUpTimeGoal=$latestWakeUpTimeGoal"
                    )
                }
            }
            Log.d("SleepGoalCollector", "latestGoalSetTime = ${Instant.ofEpochMilli(latestGoalSetTime).atZone(ZoneId.systemDefault()).toLocalDateTime()}")
        } else {
            Log.e("SleepGoalCollector", "BedTime and WakeUpTime has different numbers. Cannot load SleepGoal.")
        }
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
                Log.d("SleepGoalCollector", "Synced at $timestamp")
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
        val bedTime:Long,
        val wakeUpTime:Long,
        val goalSetTime: Long,
    ) : DataEntity(received)
}