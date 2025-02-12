package com.example.trackertest

import android.content.Context
import android.util.Log
import com.example.trackertest.tracker.collector.core.DataEntity
import com.example.trackertest.tracker.collector.samsunghealth.AbstractMeasurementSessionCollector

class SessionDataCollectorContainer<
        T: AbstractMeasurementSessionCollector<*, *, *>>(val collector:T){
    val metadataStorage:MutableList<DataEntity> = mutableListOf()
    val dataStorage:MutableList<DataEntity> = mutableListOf()
    init{
        collector.metadataListener = { metadataStorage.add(it) }
        collector.listener = { dataStorage.add(it) }
    }
    fun start(){
        return collector.start()
    }
    fun stop(){
        try {
            return collector.stop()
        } catch (e:Exception){
            Log.w("TAG", "SessionDataCollectorContainer : Collector for ${collector.getEntityClass()}'s stop() made error")
            return Unit
        }
    }
}

