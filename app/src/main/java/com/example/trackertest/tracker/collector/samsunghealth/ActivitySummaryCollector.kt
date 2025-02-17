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
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
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
import java.time.ZoneOffset
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

    private val syncPastLimitDays:Long = 0
    private val syncPastLimitMillis:Long = syncPastLimitDays * 24L * 3600L * 1000L
    private var latestDataTimestamp:Long = -1L

    fun getTimeFilter():LocalTimeFilter{
        //Log.d("TAG", "ActivitySummary.getTimeFilter() : latestDataTimestamp=$latestDataTimestamp")
        val currentTimestamp = System.currentTimeMillis()

        //val targetTimestamp = if(latestDataTimestamp == -1L) (currentTimestamp - 30L * 24L * 3600L * 1000L) else latestDataTimestamp
        //Activity summary for 1 month is too large to compute, just gather data for 7 days
        val targetTimestamp = if(latestDataTimestamp == -1L) (currentTimestamp - syncPastLimitMillis) else latestDataTimestamp

        val nowDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(targetTimestamp),
            ZoneId.systemDefault()
        )
        val targetDayStart = LocalDateTime.of(
            nowDateTime.year,
            nowDateTime.month,
            nowDateTime.dayOfMonth,
            0,0
        )
        val targetDayEnd = targetDayStart.plusDays(1)
        val targetDayEndTimestamp = targetDayEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if(targetDayEndTimestamp < currentTimestamp){
            //For some reason, sync not done for some while.
            latestDataTimestamp = targetDayEndTimestamp
        } else {
            //Updating for current day.
            latestDataTimestamp = currentTimestamp
        }
        return LocalTimeFilter.of(
            targetDayStart,
            targetDayEnd
        )
    }
    suspend fun readData(store: HealthDataStore): Entity?{
        val timeFilter = getTimeFilter()
        val activeCaloriesBurnedReq = DataType.ActivitySummaryType
            .TOTAL_ACTIVE_CALORIES_BURNED
            .requestBuilder
            .setLocalTimeFilter(timeFilter)
            .setOrdering(Ordering.DESC)
            .build()
        val activeTimeReq = DataType.ActivitySummaryType
            .TOTAL_ACTIVE_TIME
            .requestBuilder
            .setLocalTimeFilter(timeFilter)
            .setOrdering(Ordering.DESC)
            .build()
        val caloriesBurnedReq = DataType.ActivitySummaryType
            .TOTAL_CALORIES_BURNED
            .requestBuilder
            .setLocalTimeFilter(timeFilter)
            .setOrdering(Ordering.DESC)
            .build()
        val distanceReq = DataType.ActivitySummaryType
            .TOTAL_DISTANCE
            .requestBuilder
            .setLocalTimeFilter(timeFilter)
            .setOrdering(Ordering.DESC)
            .build()
        val startTime:Long = timeFilter.startTime!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime:Long = timeFilter.endTime!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val activeCaloriesBurnedRawDataList = store.aggregateData(activeCaloriesBurnedReq).dataList
        if(activeCaloriesBurnedRawDataList.isEmpty())
            return null
        val activeCaloriesBurnedData:Float? = activeCaloriesBurnedRawDataList.first().value
        val activeTimeData:Long? = store.aggregateData(activeTimeReq).dataList.first().value?.toMillis()
        val caloriesBurnedData:Float? = store.aggregateData(caloriesBurnedReq).dataList.first().value
        val distanceData:Float? = store.aggregateData(distanceReq).dataList.first().value

        Log.d("TAG", "ActivitySummaryCollector : startTime=$startTime, endTime=$endTime")
        return Entity(
            System.currentTimeMillis(),
            startTime,
            endTime,
            activeCaloriesBurnedData?: 0.0f,
            activeTimeData?:0L,
            caloriesBurnedData?:0.0f,
            distanceData?:0.0f
        )
    }
    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            //TODO: store should be passed from outside, not getting here.
            val store = HealthDataService.getStore(context)
            while(isActive){
                val timestamp = System.currentTimeMillis()
                //Log.d("TAG", "ActivitySummaryCollector: $timestamp")

                var isSleep = true
                val readEntity = readData(store)
                if(readEntity != null){
                    listener?.invoke(
                        readEntity
                    )
                    isSleep = readEntity.endTime >= timestamp
                }

                if(isSleep)
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