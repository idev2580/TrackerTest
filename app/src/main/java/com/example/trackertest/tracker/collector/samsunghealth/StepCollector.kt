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
import com.samsung.android.sdk.health.data.error.PlatformInternalException
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.LocalDateFilter
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
            TimeUnit.SECONDS.toMillis(60)
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

    private val syncPastLimitDays:Long = 64
    private val syncUnitTimeMinutes:Long = 10
    private val syncUnitTimeMillis:Long = syncUnitTimeMinutes * 60000

    private suspend fun readAllDataByGroup(store:HealthDataStore, since:Long, listener:((DataEntity)->Unit)?):Long{
        val timestamp = System.currentTimeMillis()

        //최근 걸음 정보를 불러와야 하기 때문에 분 단위로 내림한다.
        //(워치 데이터는 실시간으로 휴대폰에 전송되지 않기 때문에 최소 1시간 여유를 두는 것)
        val fromTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(since), ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.MINUTES).minusHours(1)
        val req = DataType.StepsType
            .TOTAL
            .requestBuilder
            .setLocalTimeFilterWithGroup(
                LocalTimeFilter.since(
                    fromTime
                ),
                LocalTimeGroup.of(LocalTimeGroupUnit.MINUTELY, 10)
            )
            .setOrdering(Ordering.DESC)
            .build()
        val resList = store.aggregateData(req).dataList
        Log.d("StepCollector", "readAllDataByGroup() : ${resList.size} step data loaded, timeFilter=since(${fromTime})")
        var maxTime:Long = since
        resList.forEach{ it->
            listener?.invoke(
                Entity(
                    timestamp,
                    it.value?:0L,
                    it.startTime.toEpochMilli(),
                    it.endTime.toEpochMilli()
                )
            )
            if(it.endTime.toEpochMilli() > maxTime)
                maxTime = it.endTime.toEpochMilli()
        }
        return maxTime
    }

    private var lastSynced:Long = System.currentTimeMillis() - syncPastLimitDays*24L*3600L*1000L
    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            //TODO: store should be passed from outside, not getting here.
            val store = HealthDataService.getStore(context)
            val latestGoalSetTime:Long = -1;
            while(isActive){
                val timestamp = System.currentTimeMillis()

                //TODO : Insert only if this entity's timeslot is new. Else, do update instead of insertion.
                //lastSynced = readAllData(store, lastSynced, listener)
                try {
                    lastSynced = readAllDataByGroup(store, lastSynced, listener)
                    Log.d("StepCollector", "Synced at : $timestamp")
                } catch (e: PlatformInternalException) {
                    //Do nothing
                    Log.e("StepCollector", "Sync Error at : $timestamp")
                    Log.e("StepCollector", Log.getStackTraceString(e))
                }

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