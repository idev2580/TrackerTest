package com.example.trackertest

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.trackertest.tracker.collector.core.AbstractCollector
import com.example.trackertest.tracker.collector.core.DataEntity

class NonsessionDataCollectorContainer<T:AbstractCollector<*,*>>(val collector:T) : ViewModel() {
    val dataStorage:MutableList<DataEntity> = mutableStateListOf()
    init{
        collector.listener = { dataStorage.add(it) }
    }
    fun start() {
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