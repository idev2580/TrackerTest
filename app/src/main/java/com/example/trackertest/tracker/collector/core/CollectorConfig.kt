package com.example.trackertest.tracker.collector.core

interface CollectorConfig {
    fun copy(property: String, setValue: String): CollectorConfig
}