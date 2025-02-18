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

    private val deviceInfo:MutableMap<String, Entity> = mutableMapOf()
    private suspend fun readMapData(manager: DeviceManager): Map<String,Entity>{
        val rTimestamp = System.currentTimeMillis()
        //What a dirty code from absence of DeviceGroup.ALL
        val mobiles = manager.getDevices(DeviceGroup.MOBILE)
        val watches = manager.getDevices(DeviceGroup.WATCH)
        val bands = manager.getDevices(DeviceGroup.BAND)
        val rings = manager.getDevices(DeviceGroup.RING)
        val accessories = manager.getDevices(DeviceGroup.ACCESSORY)
        val others = manager.getDevices(DeviceGroup.OTHER)
        val devices = mobiles + watches + bands + rings + accessories + others

        val readDeviceInfo:MutableMap<String, Entity> = mutableMapOf()
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
                Log.d("TAG", "DeviceCollector : Device detected(name=$deviceName, type=$deviceType, id=$deviceId)")
            }
            readDeviceInfo[deviceId] = Entity(
                rTimestamp,
                deviceType,
                deviceId,
                deviceManufacturer,
                deviceModel,
                deviceName
            )
        }
        return readDeviceInfo
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
                Log.d("DeviceCollector", "Synced at $timestamp")

                val readEntity = readMapData(deviceManager)
                readEntity.forEach{
                    it->
                    if(!(it.key in deviceInfo)){
                        deviceInfo[it.key] = it.value
                        listener?.invoke(it.value)
                    }
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