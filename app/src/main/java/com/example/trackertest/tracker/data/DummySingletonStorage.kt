package com.example.trackertest.tracker.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DummySingletonStorage<T>(
    initVal:T
):SingletonStorageInterface<T> {
    private val _stateFlow = MutableStateFlow(initVal)
    override val stateFlow: StateFlow<T> get() = _stateFlow
    var data:T = initVal
    override fun get(): T{
        return data
    }
    override fun set(value: T){
        data = value
    }
}