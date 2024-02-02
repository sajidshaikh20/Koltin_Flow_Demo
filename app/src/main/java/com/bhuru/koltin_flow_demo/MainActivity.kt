package com.bhuru.koltin_flow_demo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.bhuru.koltin_flow_demo.R.id.channelstart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Using a Channel for demonstration purposes
    val channel = Channel<Int>()

    // Declaring a nullable Job for global coroutine management
    var globalJob: Job? = null

    @SuppressLint("MissingInflatedId")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.text).setOnClickListener {
            getFormatedNotes()
        }

        findViewById<Button>(channelstart).setOnClickListener {
            Toast.makeText(this, "start check log", Toast.LENGTH_SHORT).show()
            producerchannel()
            GlobalScope.launch {
                delay(5000)
                consumerchannel()
            }
        }

        findViewById<Button>(R.id.flowstart).setOnClickListener {
            Toast.makeText(this, "cancel", Toast.LENGTH_SHORT).show()
            globalJob?.cancel()
        }

        findViewById<Button>(R.id.flowstarte).setOnClickListener {
            Toast.makeText(this, "flow start check log", Toast.LENGTH_SHORT).show()
            globalJob = GlobalScope.launch {
                val data: Flow<Int> = flowproducer()
                    .onStart {
                        emit(0)
                        Log.i("2181", "onCreate: producer start ")
                    }.onCompletion {
                        emit(11)
                        Log.i("2181", "onCreate: compelete the producer")
                    }.onEach {
                        Log.i("2181", "onCreate: emit se pehele $it")
                    }
                    .buffer(3)
                data.collect {
                    delay(1500)
                    Log.i("2181", "second flow consumer: ${it.toString()}")
                }
            }
        }


        findViewById<Button>(R.id.sharedflowstart).setOnClickListener {
            GlobalScope.launch(Dispatchers.Main){
                val result = singleProducerButMultipleConsumer()
                result.collect{
                    Log.i("sharedflow", "first consumer: item   $it ")
                }
            }
            GlobalScope.launch(Dispatchers.Main){
                val result = singleProducerButMultipleConsumer()
                delay(2500)
                result.collect{
                    Log.i("sharedflow", "second consumer: item   $it ")
                }
            }
        }
    }

    private fun getFormatedNotes() {
        CoroutineScope(Dispatchers.IO).launch {
            //    try {
            getNotes().map {
                FormatedNote(it.isActive, it.title.uppercase(), it.description)
            }.filter {
                it.isActive
            }.onCompletion {
                Log.i("2181", "consumer: ${Thread.currentThread().name}")
            }.flowOn(Dispatchers.Main)
                .collect {
                    Log.i("2181", "collector: ${Thread.currentThread().name}")
                    Log.i("2181", "getuser id: $it")
                }
            /* } catch (e:Exception){
                 Log.i("error", "consumer ${e.toString()}")
             }*/

        }
    }

    private suspend fun getNotes(): Flow<Note> {
        return flow {
            val list = listOf(
                Note(1, true, "first", "first Description"),
                Note(2, false, "second", "second Description"),
                Note(3, true, "third", "third Description"),
                Note(4, false, "fourth", "fourth Description"),
                Note(5, true, "fifth", "fifth Description"),
                Note(6, true, "sixth", "sixth Description"),
                Note(7, false, "seven", "seven Description"),
                Note(8, true, "eight", "eight Description"),
                Note(9, false, "nine", "nine Description"),
            )
            list.asFlow()
            list.forEach {
                emit(it)
                Log.i("2181", "producer: ${Thread.currentThread().name}")
            }

        }.catch {
            Log.i("2182", "error: $it")
        }

    }

    private suspend fun getUserId(id: String): String {

        delay(1000)
        return "USER$id"
    }

    // Function to produce values into the channel
    fun producerchannel() {
        CoroutineScope(Dispatchers.Main).launch {
            channel.send(1)
            channel.send(2)
            channel.send(3)
            channel.send(4)
            channel.send(5)
        }
    }

    // Function to consume values from the channel
    fun consumerchannel() {
        CoroutineScope(Dispatchers.Main).launch {
            Log.i("2181", "consumer: ${channel.receive().toString()}")
            Log.i("2181", "consumer: ${channel.receive().toString()}")
            Log.i("2181", "consumer: ${channel.receive().toString()}")
            Log.i("2181", "consumer: ${channel.receive().toString()}")
            Log.i("2181", "consumer: ${channel.receive().toString()}")
        }
    }

    // Function to create a Flow with delayed emission
    private fun flowproducer() = flow<Int> {
        // val mutableSharedFlow = MutableSharedFlow<>()
        val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        list.forEach {
            delay(1000)
            emit(it)
        }
    }

    //demo for share flow

    private fun singleProducerButMultipleConsumer(): Flow<Int> {
        //replay bhi ker sakte hai
        val mutableSharedFlow = MutableSharedFlow<Int>()
        GlobalScope.launch {
            val movies = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            movies.forEach {
                mutableSharedFlow.emit(it)
                delay(1000)
            }
        }
        return mutableSharedFlow
    }

}

data class Note(val id: Int, val isActive: Boolean, val title: String, val description: String)
data class FormatedNote(val isActive: Boolean, val title: String, val description: String)