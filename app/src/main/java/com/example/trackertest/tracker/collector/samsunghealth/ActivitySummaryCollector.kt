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
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeGroup
import com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit
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
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class ActivitySummaryCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
): AbstractCollector<ActivitySummaryCollector.Config, ActivitySummaryCollector.Entity>(permissionManager
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

    private val syncPastLimitDays:Long = 31
    // private val syncPastLimitMillis:Long = syncPastLimitDays * 24L * 3600L * 1000L
    // private var latestDataTimestamp:Long = -1L

    private suspend fun readAllDataByGroup(store:HealthDataStore, since:Long, listener:((DataEntity)->Unit)?):Long{
        val timestamp = System.currentTimeMillis()

        val timeFilter = LocalTimeFilter.since(
            //최근 1일동안의 정보는 계속 업데이트 되어야 하므로 일 단위로 내림
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(since), ZoneId.systemDefault())
                    .truncatedTo(ChronoUnit.DAYS)
        )
        val timeGroup = LocalTimeGroup.of(LocalTimeGroupUnit.DAILY,1)
        val activeCaloriesBurnedReq = DataType.ActivitySummaryType
            .TOTAL_ACTIVE_CALORIES_BURNED
            .requestBuilder
            .setLocalTimeFilterWithGroup(timeFilter, timeGroup)
            .setOrdering(Ordering.DESC)
            .build()
        val activeTimeReq = DataType.ActivitySummaryType
            .TOTAL_ACTIVE_TIME
            .requestBuilder
            .setLocalTimeFilterWithGroup(timeFilter, timeGroup)
            .setOrdering(Ordering.DESC)
            .build()
        val caloriesBurnedReq = DataType.ActivitySummaryType
            .TOTAL_CALORIES_BURNED
            .requestBuilder
            .setLocalTimeFilterWithGroup(timeFilter, timeGroup)
            .setOrdering(Ordering.DESC)
            .build()
        val distanceReq = DataType.ActivitySummaryType
            .TOTAL_DISTANCE
            .requestBuilder
            .setLocalTimeFilterWithGroup(timeFilter, timeGroup)
            .setOrdering(Ordering.DESC)
            .build()

        val resMap:MutableMap<Long, Entity> = mutableMapOf()

        val activeTimeList = store.aggregateData(activeTimeReq).dataList
        val activeCaloriesBurnedList = store.aggregateData(activeCaloriesBurnedReq).dataList
        val caloriesBurnedList = store.aggregateData(caloriesBurnedReq).dataList
        val distanceList = store.aggregateData(distanceReq).dataList

        var maxEndTime:Long = since
        activeTimeList.forEach{
            val startTime = it.startTime.toEpochMilli()
            val endTime = it.endTime.toEpochMilli()
            if(endTime > maxEndTime)
                maxEndTime = endTime
            val activeTime = it.value?.toMillis()?:0L
            if(resMap[startTime] == null){
                resMap[startTime] = Entity(
                    timestamp,
                    startTime,
                    endTime,
                    0.0f,
                    activeTime,
                    0.0f,
                    0.0f
                )
            } else {
                val original = resMap[startTime]!!
                resMap[startTime] = Entity(
                    original.received,
                    original.startTime,
                    original.endTime,
                    original.activeCaloriesBurned,
                    activeTime,
                    original.caloriesBurned,
                    original.distance
                )
            }
        }
        activeCaloriesBurnedList.forEach{
            val startTime = it.startTime.toEpochMilli()
            val endTime = it.endTime.toEpochMilli()
            if(endTime > maxEndTime)
                maxEndTime = endTime
            val activeCaloriesBurned:Float = it.value?:0.0f

            if(resMap[startTime] == null){
                resMap[startTime] = Entity(
                    timestamp,
                    startTime,
                    endTime,
                    activeCaloriesBurned,
                    0L,
                    0.0f,
                    0.0f
                )
            } else {
                val original = resMap[startTime]!!
                resMap[startTime] = Entity(
                    original.received,
                    original.startTime,
                    original.endTime,
                    activeCaloriesBurned,
                    original.activeTime,
                    original.caloriesBurned,
                    original.distance
                )
            }
        }
        caloriesBurnedList.forEach{
            val startTime = it.startTime.toEpochMilli()
            val endTime = it.endTime.toEpochMilli()
            if(endTime > maxEndTime)
                maxEndTime = endTime
            val caloriesBurned = it.value?:0.0f

            if(resMap[startTime] == null){
                resMap[startTime] = Entity(
                    timestamp,
                    startTime,
                    endTime,
                    0.0f,
                    0L,
                    caloriesBurned,
                    0.0f
                )
            } else {
                val original = resMap[startTime]!!
                resMap[startTime] = Entity(
                    original.received,
                    original.startTime,
                    original.endTime,
                    original.activeCaloriesBurned,
                    original.activeTime,
                    caloriesBurned,
                    original.distance
                )
            }
        }
        distanceList.forEach{
            val startTime = it.startTime.toEpochMilli()
            val endTime = it.endTime.toEpochMilli()
            if(endTime > maxEndTime)
                maxEndTime = endTime
            val distance:Float = it.value ?: 0.0f

            if(resMap[startTime] == null){
                resMap[startTime] = Entity(
                    timestamp,
                    startTime,
                    endTime,
                    0.0f,
                    0L,
                    0.0f,
                    distance
                )
            } else {
                val original = resMap[startTime]!!
                resMap[startTime] = Entity(
                    original.received,
                    original.startTime,
                    original.endTime,
                    original.activeCaloriesBurned,
                    original.activeTime,
                    original.caloriesBurned,
                    distance
                )
            }
        }
        Log.d("ActivitySummaryCollector", "${resMap.size} summary data loaded, timeFilter=since(${timeFilter.startTime})")
        resMap.forEach{
            listener?.invoke(it.value)
        }
        return maxEndTime
    }

    private var lastSynced:Long = System.currentTimeMillis() - syncPastLimitDays*24L*3600000L
    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            //TODO: store should be passed from outside, not getting here.
            val store = HealthDataService.getStore(context)
            while(isActive){
                val timestamp = System.currentTimeMillis()

                lastSynced = readAllDataByGroup(store, lastSynced, listener)
                Log.d("ActivitySummaryCollector", "Synced at $timestamp")
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
        val startTime : Long,
        val endTime : Long,
        val activeCaloriesBurned: Float,
        val activeTime : Long,
        val caloriesBurned : Float,
        val distance : Float,
    ) : DataEntity(received)
}