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
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class StepCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
) : AbstractCollector<StepCollector.Config, StepCollector.Entity>(permissionManager
    , configStorage, stateStorage) {

    companion object{
        val defaultConfig = Config(
            TimeUnit.SECONDS.toMillis(5)
        )
    }
    override val _defaultConfig = StepCollector.defaultConfig

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

    private val syncPastLimitDays:Long = 0
    private val syncUnitTimeMinutes:Long = 10
    private val syncUnitTimeMillis:Long = syncUnitTimeMinutes * 60000

    fun getTimeFilter(): LocalTimeFilter {
        val now = LocalDateTime.now()
        val sTime = now
            .minusMinutes((now.minute % syncUnitTimeMinutes).toLong())
            .minusSeconds(now.second.toLong())
            .minusNanos(now.nano.toLong())
        val eTime = sTime.plusMinutes(syncUnitTimeMinutes)
        return LocalTimeFilter.of(sTime, eTime)
    }
    fun getTimeFilter(inputTime:Long):LocalTimeFilter {
        val inputStime:LocalDateTime =
            if(inputTime != -1L)
                Instant.ofEpochMilli(inputTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            else
                LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
        val sTime = inputStime
            .minusMinutes((inputStime.minute % syncUnitTimeMinutes).toLong())
            .truncatedTo(ChronoUnit.MINUTES)
            .minusDays(syncPastLimitDays)

            //.minusSeconds(inputStime.second.toLong())
            //.minusNanos(inputStime.nano.toLong())
        val eTime = sTime.plusMinutes(syncUnitTimeMinutes)
        return LocalTimeFilter.of(sTime, eTime)
    }

    suspend fun readData(store: HealthDataStore):Entity?{
        //지금 시간이 포함되어 있는 Timeslot의 Steps 정보를 읽어온다.
        val timeFilter = getTimeFilter()
        val req = DataType.StepsType
            .TOTAL
            .requestBuilder
            .setLocalTimeFilter(timeFilter)
            .setOrdering(Ordering.DESC)
            .build()

        val resList = store.aggregateData(req).dataList

        val startTime = timeFilter.startTime!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = timeFilter.endTime!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        //Log.d("TAG", "StepCollector : $startTime~$endTime")
        if(resList.isNotEmpty()){
            val step:Long? = resList.first().value?.toLong()
            if(step != null){
                Log.d("TAG", "StepCollector : $step steps in $startTime~$endTime")
                return Entity(
                    System.currentTimeMillis(),
                    step,
                    startTime,
                    endTime
                )
            }
        }
        return null
    }
    suspend fun readAllData(store:HealthDataStore, since:Long, listener:((DataEntity)->Unit)?):Long{
        val timestamp = System.currentTimeMillis()
        var loop = true
        var syncTarget = since
        while(loop){
            val timeFilter = getTimeFilter(syncTarget)
            syncTarget = timeFilter.endTime!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            Log.d("StepCollector","readAllData() : syncTarget=$syncTarget, endTime=${timeFilter.endTime}")
            if(timeFilter.endTime!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() >= timestamp)
                loop = false

            val req = DataType.StepsType
                .TOTAL
                .requestBuilder
                .setLocalTimeFilter(timeFilter)
                .setOrdering(Ordering.DESC)
                .build()

            val resList = store.aggregateData(req).dataList

            val startTime = timeFilter.startTime!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTime = timeFilter.endTime!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            //Log.d("TAG", "StepCollector : $startTime~$endTime")
            if(resList.isNotEmpty()){
                val step:Long? = resList.first().value?.toLong()
                if(step != null){
                    Log.d("StepCollector", "$step steps in $startTime~$endTime")
                    listener?.invoke(
                        Entity(
                            System.currentTimeMillis(),
                            step,
                            startTime,
                            endTime
                        )
                    )
                }
            }
        }
        return timestamp
    }

    var lastSynced:Long = -1
    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            //TODO: store should be passed from outside, not getting here.
            val store = HealthDataService.getStore(context)
            val latestGoalSetTime:Long = -1;
            while(isActive){
                val timestamp = System.currentTimeMillis()

                //TODO : Insert only if this entity's timeslot is new. Else, do update instead of insertion.
                lastSynced = readAllData(store, lastSynced, listener)
                Log.d("StepCollector", "Synced at : $timestamp")
                /*val readEntity = readData(store)
                if(readEntity != null){
                    listener?.invoke(
                        readEntity
                    )
                }*/

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
        val startTime: Long,
        val endTime: Long,
    ) : DataEntity(received)
}