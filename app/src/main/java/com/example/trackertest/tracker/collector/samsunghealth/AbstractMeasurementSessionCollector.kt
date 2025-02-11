package com.example.trackertest.tracker.collector.samsunghealth

import com.example.trackertest.tracker.collector.core.AbstractCollector
import com.example.trackertest.tracker.collector.core.CollectorConfig
import com.example.trackertest.tracker.collector.core.CollectorState
import com.example.trackertest.tracker.collector.core.DataEntity
import com.example.trackertest.tracker.data.SingletonStorageInterface
import com.example.trackertest.tracker.permission.PermissionManagerInterface

abstract class AbstractMeasurementSessionCollector<
        T: CollectorConfig,
        MK: DataEntity,
        K: DataEntity>
    (
        permissionManager: PermissionManagerInterface,
        configStorage: SingletonStorageInterface<T>,
        stateStorage: SingletonStorageInterface<CollectorState>,
): AbstractCollector<T,K>(
    permissionManager,
    configStorage,
    stateStorage
){
    var metadataListener: ((MK) -> Unit)? = null
}