package com.example.smart_fridge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smart_fridge.ui.theme.Smart_FridgeTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.util.Date
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.zIndex

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Smart_FridgeTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
//                    Greeting("Android")
//                    GreetingImage(message = getString(R.string.greeting_text), getString(R.string.greeting_intro))
                    ItemList()
                }
            }
        }
        thread.start()
    }
}

// NEED TO CHANGE:
// change the ip address and port according to the server
val IPaddress = "192.168.10.51"
val port = "8080"

var get = true
var fridgeItems: MutableList<MutableMap<String, String>> = mutableListOf()   // the list of all items in the fridge
val itemKeys = arrayOf("type", "in_time", "expire_dates", "level", "status")
val TYPE = 0
val IN_TIME = 1
val EXPIRE_DATES = 2
val LEVEL = 3
val STATUS = 4

var thread = Thread {
    try {
        while (true) {
            if (get) sendGetRequest()
            get = false
            sleep(1000)
            Log.d("smartFridge-button", "thread set get = $get")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// to be implemented
fun sendPostRequest(userName:String, password:String) {

    var reqParam = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(userName, "UTF-8")
    reqParam += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8")
    val mURL = URL("http://$IPaddress:$port/")

    with(mURL.openConnection() as HttpURLConnection) {
        // optional default is GET
        requestMethod = "POST"

        val wr = OutputStreamWriter(getOutputStream());
        wr.write(reqParam);
        wr.flush();

        println("URL : $url")
        println("Response Code : $responseCode")

        BufferedReader(InputStreamReader(inputStream)).use {
            val response = StringBuffer()

            var inputLine = it.readLine()
            while (inputLine != null) {
                response.append(inputLine)
                inputLine = it.readLine()
            }
            println("Response : $response")
        }
    }
}

// send get request to the server to obtain the information of items in the fridge
fun sendGetRequest() {

    val mURL = URL("http://$IPaddress:$port/")
    Log.d("get", "get start")

    with(mURL.openConnection() as HttpURLConnection) {
        // optional default is GET
        requestMethod = "GET"

        Log.d("smartFridge-get", "URL : $url")
        Log.d("smartFridge-res", "Response Code : $responseCode")

        BufferedReader(InputStreamReader(inputStream)).use {
            val response = StringBuffer()

            var inputLine = it.readLine()
            while (inputLine != null) {
                response.append(inputLine)
                inputLine = it.readLine()
            }
            it.close()

            ////////// deal with response from server
            // convert response: String to a list of item objects (string)
            val resList = response.slice(2..response.length-3).split("}, {")
            // convert each item object (string) to a map
            Log.d("smartFridge-res", "before-items : $fridgeItems")
            var newFridgeItems = mutableListOf<MutableMap<String, String>>()
            for (i in resList) {
                val listItem = i.split(", ")

                val mapItem = mutableMapOf<String, String>()
                for (j in listItem.indices) {
                    val key = listItem[j].split(": ")[0]
                    val value = listItem[j].split(": ")[1]
                    var mapKey: String = if (key[0] == '"') {
                        key.slice(1..key.length-2)
                    } else {
                        key
                    }
                    var mapValue = if (value[0] == '"') {
                        value.slice(1..value.length-2)
                    } else {
                        value
                    }
                    mapItem[mapKey] = mapValue
                }
                newFridgeItems.add(mapItem)
            }
            fridgeItems = newFridgeItems
            Log.d("smartFridge-res", "after-items : $fridgeItems")
        }
    }
}

@Composable
private fun ItemCard(FridgeItem: MutableMap<String, String>, modifier: Modifier = Modifier) {
    val itemImages = mapOf(
        "apple" to painterResource(R.drawable.apple),
        "banana" to painterResource(R.drawable.banana),
        "tomato" to painterResource(R.drawable.tomato),
        "broccoli" to painterResource(R.drawable.broccoli),
        "green pepper" to painterResource(R.drawable.green_pepper),
        "orange" to painterResource(R.drawable.orange)
    )
    val colorMap = mapOf(
        "-1" to colorResource(id = R.color.expired),
        "0" to colorResource(id = R.color.bad),
        "1" to colorResource(id = R.color.white)
    )
    colorMap[FridgeItem[itemKeys[STATUS]]]?.let {
        Card(
        backgroundColor = it,
        modifier = modifier
            .padding(2.dp)
            .fillMaxWidth()
    ) {
        Row {
            Column(modifier = Modifier.width(180.dp)) {
                Row {
                    itemImages[FridgeItem[itemKeys[TYPE]]]?.let {
                        Image(
                            painter = it,
                            contentDescription = "item image",
                            modifier = Modifier
                                .padding(5.dp)
                                .width(33.dp)
                        )
                    }
                    FridgeItem[itemKeys[TYPE]]?.let {
                        Text(
                            text = it,
                            textAlign = TextAlign.Start,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(4.dp)
                        )
                    }
                }

                FridgeItem[itemKeys[LEVEL]]?.let {
                    Text(
                        text = "Level: $it",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Column() {
                FridgeItem[itemKeys[IN_TIME]]?.let {
                    Text(
                        text = "Date in: $it",
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                }
                FridgeItem[itemKeys[EXPIRE_DATES]]?.let {
                    Text(
                        text ="Days to expire: $it",
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun ItemList(modifier: Modifier = Modifier) {
    var fridgeItemsState by remember { mutableStateOf(fridgeItems) }
    Box() {
        Button(
            onClick = {
                get = true
                Log.d("smartFridge-button", "set get = $get")
                fridgeItemsState = fridgeItems
            },
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.BottomCenter)
                .zIndex(zIndex = 1F)
        ) {
            Text(text = "Refresh")
        }
        LazyColumn {
            items(fridgeItemsState) { fridgeItem ->
                ItemCard(fridgeItem)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    Smart_FridgeTheme {
//        Greeting("A")
//        GreetingMessage(message = "Nice to meet you!", "I'm from Mars")
        fridgeItems = mutableListOf(mutableMapOf(
            "type" to "apple",
            "in_time" to "2022-11-11",
            "expire_dates" to "30",
            "level" to "1"
        ))
        ItemList()
    }
}