package com.example.trackertest.tracker.collector.core

import com.example.trackertest.tracker.TrackerUtil

open class DataEntity(
    open val received: Long,
    open val synced: Long = -1,
    open val deviceId: String = TrackerUtil.getDeviceId(),
    open val deviceModel: String = TrackerUtil.getDeviceModel(),
    open val app: String = TrackerUtil.getApp() + ":" + TrackerUtil.getAppVersion(),
    open val androidVersion: String = TrackerUtil.getOSVersion()
)