package com.example.trackertest

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.trackertest.tracker.collector.core.AbstractCollector
import com.example.trackertest.tracker.collector.core.CollectorState
import com.example.trackertest.tracker.collector.samsunghealth.ActiveCaloriesBurnedGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.ActiveTimeGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.ActivitySummaryCollector
import com.example.trackertest.tracker.collector.samsunghealth.BloodOxygenCollector
import com.example.trackertest.tracker.collector.samsunghealth.BloodPressureCollector
import com.example.trackertest.tracker.collector.samsunghealth.BodyCompositionCollector
import com.example.trackertest.tracker.collector.samsunghealth.DeviceCollector
import com.example.trackertest.tracker.collector.samsunghealth.HeartRateCollector
import com.example.trackertest.tracker.collector.samsunghealth.NutritionGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.SkinTemperatureCollector
import com.example.trackertest.tracker.collector.samsunghealth.SleepGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.StepCollector
import com.example.trackertest.tracker.collector.samsunghealth.StepGoalCollector
import com.example.trackertest.tracker.collector.samsunghealth.WaterIntakeCollector
import com.example.trackertest.tracker.collector.samsunghealth.WaterIntakeGoalCollector
import com.example.trackertest.tracker.data.DummySingletonStorage
import com.example.trackertest.tracker.permission.DummyPermissionManager

class SampleTracker(
    val context: Context
) : ViewModel() {
    val acbCollector = NonsessionDataCollectorContainer<ActiveCaloriesBurnedGoalCollector>(
        ActiveCaloriesBurnedGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<ActiveCaloriesBurnedGoalCollector.Config>(
                ActiveCaloriesBurnedGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val atCollector = NonsessionDataCollectorContainer<ActiveTimeGoalCollector>(
        ActiveTimeGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<ActiveTimeGoalCollector.Config>(ActiveTimeGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val asCollector = NonsessionDataCollectorContainer<ActivitySummaryCollector>(
        ActivitySummaryCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<ActivitySummaryCollector.Config>(ActivitySummaryCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val boCollector = SessionDataCollectorContainer<BloodOxygenCollector>(
        BloodOxygenCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<BloodOxygenCollector.Config>(BloodOxygenCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val bpCollector = NonsessionDataCollectorContainer<BloodPressureCollector>(
        BloodPressureCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<BloodPressureCollector.Config>(BloodPressureCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val bodyCompositionCollector = NonsessionDataCollectorContainer<BodyCompositionCollector>(
        BodyCompositionCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<BodyCompositionCollector.Config>(BodyCompositionCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val devCollector = NonsessionDataCollectorContainer<DeviceCollector>(
        DeviceCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<DeviceCollector.Config>(DeviceCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val hrCollector = SessionDataCollectorContainer<HeartRateCollector>(
        HeartRateCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<HeartRateCollector.Config>(HeartRateCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val stCollector = SessionDataCollectorContainer<SkinTemperatureCollector>(
        SkinTemperatureCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<SkinTemperatureCollector.Config>(SkinTemperatureCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val nutCollector = NonsessionDataCollectorContainer<NutritionGoalCollector>(
        NutritionGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<NutritionGoalCollector.Config>(NutritionGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val stepGoalCollector = NonsessionDataCollectorContainer<StepGoalCollector>(
        StepGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<StepGoalCollector.Config>(StepGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val sleepGoalCollector = NonsessionDataCollectorContainer<SleepGoalCollector>(
        SleepGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<SleepGoalCollector.Config>(SleepGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val stepCollector = NonsessionDataCollectorContainer<StepCollector>(
        StepCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<StepCollector.Config>(StepCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val waterCollector = NonsessionDataCollectorContainer<WaterIntakeCollector>(
        WaterIntakeCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<WaterIntakeCollector.Config>(WaterIntakeCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    val waterGoalCollector = NonsessionDataCollectorContainer<WaterIntakeGoalCollector>(
        WaterIntakeGoalCollector(
            context,
            DummyPermissionManager(),
            DummySingletonStorage<WaterIntakeGoalCollector.Config>(WaterIntakeGoalCollector.defaultConfig),
            DummySingletonStorage<CollectorState>(AbstractCollector.defaultState)
        )
    )
    init{
        //For step collector and device collector, need to remove duplicate data
        /*devCollector.collector.listener = {

        }
        stepCollector.collector.listener = {

        }*/
    }

    fun start(){
        acbCollector.start()
        atCollector.start()
        asCollector.start()
        boCollector.start()
        bpCollector.start()
        bodyCompositionCollector.start()
        devCollector.start()
        hrCollector.start()
        stCollector.start()
        nutCollector.start()
        stepGoalCollector.start()
        sleepGoalCollector.start()
        stepCollector.start()
        waterCollector.start()
        waterGoalCollector.start()
    }
    fun stop(){
        acbCollector.stop()
        atCollector.stop()
        asCollector.stop()
        boCollector.stop()
        bpCollector.stop()
        bodyCompositionCollector.stop()
        devCollector.stop()
        hrCollector.stop()
        stCollector.stop()
        nutCollector.stop()
        stepGoalCollector.stop()
        sleepGoalCollector.stop()
        stepCollector.stop()
        waterCollector.stop()
        waterGoalCollector.stop()
    }
}