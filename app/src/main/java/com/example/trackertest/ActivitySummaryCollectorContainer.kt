package com.example.trackertest

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.example.trackertest.tracker.collector.core.DataEntity
import com.example.trackertest.tracker.collector.samsunghealth.ActivitySummaryCollector

class ActivitySummaryCollectorContainer(val collector: ActivitySummaryCollector) {
    val dataStorage:MutableMap<Long, DataEntity> = mutableStateMapOf()
    init{
        collector.listener = { it ->
            val item = it as ActivitySummaryCollector.Entity
            dataStorage[item.startTime] = item
        }
    }
    fun start() {
        return collector.start()
    }
    fun stop(){
        try {
            return collector.stop()
        } catch (e:Exception){
            Log.w("ActivitySummaryCollectorContainer", "Collector for ${collector.getEntityClass()}'s stop() made error")
            return Unit
        }
    }
}