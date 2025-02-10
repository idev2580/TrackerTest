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
import com.samsung.android.sdk.health.data.DeviceManager
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.device.DeviceGroup
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

class DeviceCollector(
    val context: Context,
    permissionManager: PermissionManagerInterface,
    configStorage: SingletonStorageInterface<Config>,
    stateStorage: SingletonStorageInterface<CollectorState>,
): AbstractCollector<DeviceCollector.Config, DeviceCollector.Entity>(permissionManager
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

    val readDeviceInfo:MutableMap<String, Entity> = mutableMapOf()
    suspend fun readData(manager: DeviceManager): Entity?{
        val mobileDevices = manager.getDevices(DeviceGroup.MOBILE)
        val devices = manager.getDevices(DeviceGroup.OTHER) + mobileDevices
        devices.forEach{
            device ->
            val deviceId = device.id
            val deviceType = when(device.deviceType){
                DeviceGroup.MOBILE -> "MOBILE"
                DeviceGroup.WATCH -> "WATCH"
                DeviceGroup.RING -> "RING"
                DeviceGroup.BAND -> "BAND"
                DeviceGroup.ACCESSORY -> "ACCESSORY"
                DeviceGroup.OTHER -> "OTHER"
                else -> "UNKNOWN"
            }
            val deviceManufacturer = device.manufacturer ?: ""
            val deviceModel = device.model ?: ""
            val deviceName = device.name ?: ""

            if(!(deviceId in readDeviceInfo)){
                Log.d("TAG", "DeviceCollector : New device detected(name=$deviceName, type=$deviceType, id=$deviceId)")
            }
            readDeviceInfo[deviceId] = Entity(
                System.currentTimeMillis(),
                deviceType,
                deviceId,
                deviceManufacturer,
                deviceModel,
                deviceName
            )
        }
        return null
    }
    override fun start() {
        super.start()
        job = CoroutineScope(Dispatchers.IO).launch {
            //TODO: Should get latest goal's setTime
            //TODO: store should be passed from outside, not getting here.
            val store = HealthDataService.getStore(context)
            val deviceManager = store.getDeviceManager()
            while(isActive){
                val timestamp = System.currentTimeMillis()
                Log.d("TAG", "DeviceCollector: $timestamp")

                val readEntity = readData(deviceManager)
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
        val deviceType: String,
        val id: String,
        val manufacturer: String,
        val model: String,
        val name: String
    ) : DataEntity(received)
}