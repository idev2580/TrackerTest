package com.example.trackertest

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.example.trackertest.ui.NonsessionDataPanel
import com.example.trackertest.ui.SessionDataPanel

enum class MainScreen(){
    MAIN_SELECTION_SCREEN,
    NONSESSION_DATA,
    ACTIVITY_SUMMARY,
    STEP_DATA,
    BLOOD_OXYGEN,
    HEART_RATE,
    SKIN_TEMPERATURE,
}


@Composable
fun MainScreen(
    activity:MainActivity,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val permManager = SamsungHealthPermissionManager(activity)
    var hasPermission by remember{mutableStateOf(false)}

    permManager.request(arrayOf()){
        res -> if(!res){
            //Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("TAG", "All permissions granted")
            hasPermission = true
        }
    }

    val tracker: SampleTracker by remember { mutableStateOf(SampleTracker(context)) }
    var isStarted by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = "TrackerTest",
            modifier = Modifier.padding(16.dp),
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Button(
            enabled = hasPermission,
            onClick = {
            if (isStarted) {
                tracker.stop()
            } else {
                tracker.start()
            }
            isStarted = !isStarted
        }) {
            Text(text = if (isStarted) "Stop" else "Start")
        }

        NavHost(
            navController = navController,
            startDestination = MainScreen.MAIN_SELECTION_SCREEN.name,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp, 15.dp, 15.dp, 15.dp))
        ){
            composable(route = MainScreen.MAIN_SELECTION_SCREEN.name){
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.NONSESSION_DATA.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="Others")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.ACTIVITY_SUMMARY.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="Activity Summary")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.STEP_DATA.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="Step")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.BLOOD_OXYGEN.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="BloodOxygen")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.HEART_RATE.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="HeartRate")
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                navController.navigate(MainScreen.SKIN_TEMPERATURE.name)
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) {
                            Text(text="SkinTemperature")
                        }
                    }
                }
            }
            composable(route = MainScreen.NONSESSION_DATA.name){
                LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                ) {
                    item {
                        NonsessionDataPanel(
                            "ActiveCaloriesBurnedGoal", tracker.acbCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as ActiveCaloriesBurnedGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("activeCaloriesBurned", item.activeCaloriesBurned.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "ActiveTimeGoal", tracker.atCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as ActiveTimeGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("activeTime", item.activeTime.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "BloodPressure", tracker.bpCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as BloodPressureCollector.Entity
                            val valueMap = mapOf(
                                Pair("uid", item.uid),
                                Pair("appId", item.appId),
                                Pair("deviceId(sHealth)", item.mDeviceId),
                                Pair("timestamp", item.timestamp.toString()),
                                Pair("systolic", item.systolic.toString()),
                                Pair("diastolic", item.diastolic.toString()),
                                Pair("pulseRate", item.pulseRate.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "BodyComposition", tracker.bodyCompositionCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as BodyCompositionCollector.Entity
                            val valueMap = mapOf(
                                Pair("uid", item.uid),
                                Pair("timestamp", item.timestamp.toString()),
                                Pair("bodyFatRatio", item.bodyFatRatio.toString()),
                                Pair("weight", item.weight.toString()),
                                Pair("height", item.height.toString()),
                                Pair("muscleMass", item.muscleMass.toString()),
                                Pair("skeletalMuscleRatio", item.skeletalMuscleRatio.toString()),
                                Pair("totalBodyWater", item.totalBodyWater.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "Device", tracker.devCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as DeviceCollector.Entity
                            val valueMap = mapOf(
                                Pair("id", item.id),
                                Pair("deviceType", item.deviceType),
                                Pair("model", item.model),
                                Pair("name", item.name),
                                Pair("manufacturer", item.manufacturer)
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "NutritionGoal", tracker.nutCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as NutritionGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("calories", item.calories.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "SleepGoal", tracker.sleepGoalCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as SleepGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("wakeUpTime", item.wakeUpTime.toString()),
                                Pair("bedTime", item.bedTime.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "StepGoal", tracker.stepGoalCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as StepGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("steps", item.steps.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "WaterIntake", tracker.waterCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { it ->
                            val item = it as WaterIntakeCollector.Entity
                            val valueMap = mapOf(
                                Pair("uid", item.uid),
                                Pair("timestamp", item.timestamp.toString()),
                                Pair("amount", item.amount.toString())
                            )
                            valueMap
                        }
                    }
                    item {
                        NonsessionDataPanel(
                            "WaterIntakeGoal", tracker.waterGoalCollector.dataStorage,
                            modifier = Modifier
                                .fillMaxWidth(),
                            hasBottomBorder = false
                        ) { it ->
                            val item = it as WaterIntakeGoalCollector.Entity
                            val valueMap = mapOf(
                                Pair("goalSetTime", item.goalSetTime.toString()),
                                Pair("amount", item.amount.toString())
                            )
                            valueMap
                        }
                    }
                }
            }
            composable(route = MainScreen.ACTIVITY_SUMMARY.name){
                NonsessionDataPanel(
                    "ActivitySummary", tracker.asCollector.dataStorage.values.toList(),
                    modifier = Modifier
                        .fillMaxWidth(),
                    hasBottomBorder = false,
                    isLazy = true
                ) { it ->
                    val item = it as ActivitySummaryCollector.Entity
                    val valueMap = mapOf(
                        Pair("startTime",item.startTime.toString()),
                        Pair("endTime",item.endTime.toString()),
                        Pair("activeTime",item.activeTime.toString()),
                        Pair("activeCaloriesBurned",item.activeCaloriesBurned.toString()),
                        Pair("caloriesBurned",item.caloriesBurned.toString()),
                        Pair("distance",item.distance.toString()),
                    )
                    valueMap
                }
            }
            composable(route = MainScreen.STEP_DATA.name){
                NonsessionDataPanel(
                    "Step", tracker.stepCollector.dataStorage.values.toList(),
                    modifier = Modifier
                        .fillMaxWidth(),
                    hasBottomBorder = false,
                    isLazy = true
                ) { it ->
                    val item = it as StepCollector.Entity
                    val valueMap = mapOf(
                        Pair("startTime", item.startTime.toString()),
                        Pair("endTime", item.endTime.toString()),
                        Pair("steps", item.steps.toString())
                    )
                    valueMap
                }
            }
            composable(route = MainScreen.BLOOD_OXYGEN.name){
                SessionDataPanel(
                    "BloodOxygen",
                    tracker.boCollector.metadataStorage,
                    tracker.boCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxSize(),
                    { it ->
                        val item = it as BloodOxygenCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("appId", item.appId),
                            Pair("deviceId(shealth)", item.mDeviceId),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("oxygenSaturation", item.oxygenSaturation.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as BloodOxygenCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("oxygenSaturation", item.oxygenSaturation.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
            composable(route = MainScreen.HEART_RATE.name){
                SessionDataPanel(
                    "HeartRate",
                    tracker.hrCollector.metadataStorage,
                    tracker.hrCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxSize(),
                    { it ->
                        val item = it as HeartRateCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("appId", item.appId),
                            Pair("deviceId(shealth)", item.mDeviceId),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("heartRateSaturation", item.heartRate.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as HeartRateCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("heartRateSaturation", item.heartRate.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
            composable(route = MainScreen.SKIN_TEMPERATURE.name){
                SessionDataPanel(
                    "SkinTemperature",
                    tracker.stCollector.metadataStorage,
                    tracker.stCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxSize(),
                    { it ->
                        val item = it as SkinTemperatureCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("appId", item.appId),
                            Pair("deviceId(shealth)", item.mDeviceId),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("skinTemperature", item.skinTemperature.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as SkinTemperatureCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("skinTemperature", item.skinTemperature.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
        }

        /*LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                NonsessionDataPanel(
                    "ActiveCaloriesBurnedGoal", tracker.acbCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as ActiveCaloriesBurnedGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("activeCaloriesBurned", item.activeCaloriesBurned.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "ActiveTimeGoal", tracker.atCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as ActiveTimeGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("activeTime", item.activeTime.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "BloodPressure", tracker.bpCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as BloodPressureCollector.Entity
                    val valueMap = mapOf(
                        Pair("uid", item.uid),
                        Pair("timestamp", item.timestamp.toString()),
                        Pair("systolic", item.systolic.toString()),
                        Pair("diastolic", item.diastolic.toString()),
                        Pair("pulseRate", item.pulseRate.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "BodyComposition", tracker.bodyCompositionCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as BodyCompositionCollector.Entity
                    val valueMap = mapOf(
                        Pair("uid", item.uid),
                        Pair("timestamp", item.timestamp.toString()),
                        Pair("bodyFatRatio", item.bodyFatRatio.toString()),
                        Pair("weight", item.weight.toString()),
                        Pair("height", item.height.toString()),
                        Pair("muscleMass", item.muscleMass.toString()),
                        Pair("skeletalMuscleRatio", item.skeletalMuscleRatio.toString()),
                        Pair("totalBodyWater", item.totalBodyWater.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "Device", tracker.devCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as DeviceCollector.Entity
                    val valueMap = mapOf(
                        Pair("id", item.id),
                        Pair("deviceType", item.deviceType),
                        Pair("model", item.model),
                        Pair("name", item.name),
                        Pair("manufacturer", item.manufacturer)
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "NutritionGoal", tracker.nutCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as NutritionGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("calories", item.calories.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "SleepGoal", tracker.sleepGoalCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as SleepGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("wakeUpTime", item.wakeUpTime.toString()),
                        Pair("bedTime", item.bedTime.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "Step", tracker.stepCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as StepCollector.Entity
                    val valueMap = mapOf(
                        Pair("startTime", item.startTime.toString()),
                        Pair("endTime", item.endTime.toString()),
                        Pair("steps", item.steps.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "StepGoal", tracker.stepGoalCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as StepGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("steps", item.steps.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "WaterIntake", tracker.waterCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as WaterIntakeCollector.Entity
                    val valueMap = mapOf(
                        Pair("uid", item.uid),
                        Pair("timestamp", item.timestamp.toString()),
                        Pair("amount", item.amount.toString())
                    )
                    valueMap
                }
            }
            item {
                NonsessionDataPanel(
                    "WaterIntakeGoal", tracker.waterGoalCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { it ->
                    val item = it as WaterIntakeGoalCollector.Entity
                    val valueMap = mapOf(
                        Pair("goalSetTime", item.goalSetTime.toString()),
                        Pair("amount", item.amount.toString())
                    )
                    valueMap
                }
            }
            item {
                SessionDataPanel(
                    "BloodOxygen",
                    tracker.boCollector.metadataStorage,
                    tracker.boCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth(),
                    { it ->
                        val item = it as BloodOxygenCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("oxygenSaturation", item.oxygenSaturation.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as BloodOxygenCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("oxygenSaturation", item.oxygenSaturation.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
            item {
                SessionDataPanel(
                    "HeartRate",
                    tracker.hrCollector.metadataStorage,
                    tracker.hrCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth(),
                    { it ->
                        val item = it as HeartRateCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("heartRateSaturation", item.heartRate.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as HeartRateCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("heartRateSaturation", item.heartRate.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
            item {
                SessionDataPanel(
                    "SkinTemperature",
                    tracker.stCollector.metadataStorage,
                    tracker.stCollector.dataStorage,
                    modifier = Modifier
                        .fillMaxWidth(),
                    { it ->
                        val item = it as SkinTemperatureCollector.MetadataEntity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("skinTemperature", item.skinTemperature.toString())
                        )
                        valueMap
                    },
                    { it ->
                        val item = it as SkinTemperatureCollector.Entity
                        val valueMap = mapOf(
                            Pair("uid", item.uid),
                            Pair("startTime", item.startTime.toString()),
                            Pair("endTime", item.endTime.toString()),
                            Pair("skinTemperature", item.skinTemperature.toString()),
                            Pair("max", item.max.toString()),
                            Pair("min", item.min.toString())
                        )
                        valueMap
                    }
                )
            }
        }*/
    }
}