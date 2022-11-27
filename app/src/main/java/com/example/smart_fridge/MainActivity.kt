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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

var get = true;
var fridgeItems: MutableList<MutableMap<String, String>> = mutableListOf()   // the list of all items in the fridge
val itemKeys = arrayOf("type", "in_time", "expire_dates", "level")
val TYPE = 0
val IN_TIME = 1
val EXPIRE_DATES = 2
val LEVEL = 3

var thread = Thread {
    try {
        while (true) {
            if (get) sendGetRequest("a", "123")
            get = false
            sleep(1000)
            Log.d("smartFridge-button", "thread set get = $get")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun sendPostRequest(userName:String, password:String) {

    var reqParam = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(userName, "UTF-8")
    reqParam += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8")
    val mURL = URL("http://192.168.137.1:8080/")

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

fun sendGetRequest(userName:String, password:String) {

//    var reqParam = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(userName, "UTF-8")
//    reqParam += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8")

    val mURL = URL("http://192.168.137.1:8080/")
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
                fridgeItems.add(mapItem)
            }
            Log.d("smartFridge-res", "items : $fridgeItems")
        }
    }
}

@Composable
private fun ItemCard(FridgeItem: MutableMap<String, String>, modifier: Modifier = Modifier) {
    val itemImages = mapOf(
        "apple" to painterResource(R.drawable.apple),
        "banana" to painterResource(R.drawable.banana),
        "tomato" to painterResource(R.drawable.tomato),
        "broccoli" to painterResource(R.drawable.broccoli)
    )
    Card(
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

@Composable
private fun ItemList(modifier: Modifier = Modifier) {
    Column() {
        LazyColumn {
            items(fridgeItems) { fridgeItem ->
                ItemCard(fridgeItem)
            }
        }
        Button(
            onClick = {
                get = true
                Log.d("smartFridge-button", "set get = $get")
            },
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(text = "Refresh")
        }
    }

}

@Composable
fun Greeting(name: String) {
    Surface(color = Color.Gray) {
        Text(text = "Helloaaa $name!", modifier = Modifier.padding(24.dp))
    }
}

@Composable
fun GreetingMessage(message: String, from: String) {
    Column {
        Text(
            text = message,
            fontSize = 36.sp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = 16.dp, top = 16.dp)
        )
        Text(
            text = from,
            fontSize = 24.sp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
fun GreetingImage(message: String, from: String) {
    val image = painterResource(R.drawable.androidparty)
    Box{
        Image(
            painter = image,
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentScale = ContentScale.Crop
        )
        GreetingMessage(message = message, from = from)
        Button(onClick = {
            get = true
            Log.d("smartFridge-button", "set get = $get")
        }) {
            Text(text = "Refresh")
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