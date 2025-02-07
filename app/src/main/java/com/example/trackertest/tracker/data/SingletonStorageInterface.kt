package com.example.trackertest.tracker.data

import kotlinx.coroutines.flow.StateFlow

interface SingletonStorageInterface<T> {
    val stateFlow: StateFlow<T>
    fun get(): T
    fun set(value: T)
}