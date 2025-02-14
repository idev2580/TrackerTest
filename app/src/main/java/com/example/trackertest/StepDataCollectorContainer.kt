package com.example.trackertest

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.example.trackertest.tracker.collector.core.AbstractCollector
import com.example.trackertest.tracker.collector.core.DataEntity
import com.example.trackertest.tracker.collector.samsunghealth.StepCollector

class StepDataCollectorContainer(val collector: StepCollector){
    val dataStorage:MutableMap<Long, DataEntity> = mutableStateMapOf()
    init{
        collector.listener = { it ->
            val item = it as StepCollector.Entity
            //Log.d("TAG", "StepDataCollectorContainer : key=${item.startTime}, steps=${item.steps}")
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
            Log.w("TAG", "NonsessionDataCollectorContainer : Collector for ${collector.getEntityClass()}'s stop() made error")
            return Unit
        }
    }
}