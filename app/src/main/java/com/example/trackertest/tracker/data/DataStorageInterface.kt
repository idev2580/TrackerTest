package com.example.trackertest.tracker.data

import com.example.trackertest.tracker.collector.core.DataEntity
import kotlinx.coroutines.flow.StateFlow

interface DataStorageInterface<T: DataEntity> {
    val statFlow: StateFlow<Pair<Long, Long>> /*timestamp & number*/

    fun insert(data: T)
    fun getUnsynced(): List<T>
    fun updateSyncStatus(ids: List<String>, timestamp: Long)
}