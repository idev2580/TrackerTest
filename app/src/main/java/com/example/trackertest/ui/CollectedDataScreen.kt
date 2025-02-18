package com.example.trackertest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackertest.tracker.collector.core.DataEntity
import com.example.trackertest.ui.theme.Purple40
import com.example.trackertest.ui.theme.Purple80
import com.example.trackertest.ui.theme.PurpleGrey40
import com.example.trackertest.ui.theme.PurpleGrey80
import com.example.trackertest.ui.theme.White
import com.example.trackertest.ui.theme.WhiteGrey

@Composable
fun NamedPanel(name:String, isOpened: MutableState<Boolean>, modifier: Modifier = Modifier, bg:Color = WhiteGrey, content:@Composable ()->Unit){
    Column(
        modifier=modifier.background(bg),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Row(
            modifier= if(isOpened.value)
                Modifier
                    .background(Purple40)
                    .padding(4.dp)
                    .fillMaxWidth()
            else
                Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment =  Alignment.CenterVertically
        ){
            Text(text=name, color=
                if(isOpened.value) Color.White else Color.Black,
                fontSize=18.sp, fontWeight= FontWeight.SemiBold, modifier= Modifier.padding(4.dp))
            Button(onClick = {
                isOpened.value = !isOpened.value
            }){
                Text(text=if (isOpened.value) "Close" else "Open")
            }
        }
        if(isOpened.value)
            content()
    }
}

@Composable
fun NonsessionDataPanel(name:String, dataList:List<DataEntity>, modifier: Modifier = Modifier, dataKeyMapGen:(it: DataEntity)->Map<String, String>){
    val isOpened = remember{ mutableStateOf(false) }
    NamedPanel("$name(${dataList.size})", isOpened, modifier.drawWithContent{
        val lineSize = 1.dp.toPx()
        val y = size.height - lineSize
        drawContent()
        drawLine(
            color = Color.Gray,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = lineSize,
        )
    }){
        Column{
            dataList.forEach{
                DataEntityWidget(dataKeyMapGen(it))
            }
        }
    }
}

@Composable
fun SessionDataPanel(
    name:String, metadataList:List<DataEntity>,
    dataList:List<DataEntity>, modifier: Modifier = Modifier,
    metadataKeyMapGen:(it: DataEntity)->Map<String, String>,
    dataKeyMapGen:(it: DataEntity)->Map<String, String>
){
    val isOpened = remember{ mutableStateOf(false) }
    val isMetaOpened = remember{ mutableStateOf(false) }
    val isRecordOpened = remember{ mutableStateOf(false) }
    NamedPanel(name, isOpened, modifier.border(1.dp, Color.Black)){
        Column{
            NamedPanel(" - Metadata(${metadataList.size})", isMetaOpened, Modifier
                .fillMaxWidth()
                .wrapContentHeight()) {
                LazyColumn() {
                    metadataList.forEach {
                        item {
                            DataEntityWidget(metadataKeyMapGen(it))
                        }
                    }
                }
            }
            NamedPanel(" - Records(${dataList.size})", isRecordOpened, Modifier
                .fillMaxWidth()
                .wrapContentHeight()) {
                LazyColumn() {
                    dataList.forEach {
                        item {
                            DataEntityWidget(dataKeyMapGen(it))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DataEntityWidget(keyVal:Map<String, String>, modifier: Modifier = Modifier){
    Column(
        modifier = modifier.fillMaxWidth()
    ){
        var i = 0;
        keyVal.forEach{
            val rowModifier = if (i == 0) Modifier
                .fillMaxWidth()
                .background(Purple80) else Modifier.fillMaxWidth()
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Text(text = it.key)
                Text(text = it.value)
            }
            i++;
        }
    }
}