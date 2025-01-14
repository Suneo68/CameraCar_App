package com.example.carapp

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import okhttp3.*
import okio.ByteString

class MainActivity : AppCompatActivity() {

    private lateinit var upButton: AppCompatImageButton
    private lateinit var downButton: AppCompatImageButton
    private lateinit var leftButton: AppCompatImageButton
    private lateinit var rightButton: AppCompatImageButton
    private lateinit var stopButton: AppCompatImageButton
    private lateinit var speedSeekBar: SeekBar
    private lateinit var lightSeekBar: SeekBar
    private lateinit var cameraImage: ImageView

    private val wsCameraUrl = "ws://192.168.4.1/Camera" // ESP32 camera WebSocket
    private val wsCarInputUrl = "ws://192.168.4.1/CarInput" // ESP32 input WebSocket

    private var wsCamera: WebSocket? = null
    private var wsCarInput: WebSocket? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Binding views
        cameraImage = findViewById(R.id.cameraImg)
        upButton = findViewById(R.id.forwardBtn)
        downButton = findViewById(R.id.backwardBtn)
        leftButton = findViewById(R.id.leftBtn)
        rightButton = findViewById(R.id.rightBtn)
        stopButton = findViewById(R.id.stopBtn)
        speedSeekBar = findViewById(R.id.speedseekBar)
        lightSeekBar = findViewById(R.id.lightseekBar)

        initWebSocket()

        // Control buttons for car movement
        upButton.setOnTouchListener { _, event ->
            handleCarInput(event.action, "MoveCar", "1") // Forward
            true
        }

        downButton.setOnTouchListener { _, event ->
            handleCarInput(event.action, "MoveCar", "2") // Backward
            true
        }

        leftButton.setOnTouchListener { _, event ->
            handleCarInput(event.action, "MoveCar", "3") // Left
            true
        }

        rightButton.setOnTouchListener { _, event ->
            handleCarInput(event.action, "MoveCar", "4") // Right
            true
        }

        stopButton.setOnClickListener {
            sendCarInput("MoveCar", "0") // Stop
        }

        // SeekBar for speed
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sendCarInput("Speed", progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // SeekBar for light
        lightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sendCarInput("Light", progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initWebSocket() {
        val client = OkHttpClient()

        // Camera WebSocket
        val cameraRequest = Request.Builder().url(wsCameraUrl).build()
        wsCamera = client.newWebSocket(cameraRequest, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                runOnUiThread {
                    val bitmap = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size)
                    cameraImage.setImageBitmap(bitmap) // Display image from ESP32
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket-Camera", "Error: ${t.message}")
            }
        })

        // Car Input WebSocket
        val carInputRequest = Request.Builder().url(wsCarInputUrl).build()
        wsCarInput = client.newWebSocket(carInputRequest, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket-CarInput", "Connected to ESP32")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket-CarInput", "Error: ${t.message}")
            }
        })
    }


    private fun handleCarInput(action: Int, key: String, value: String) {
        when (action) {
            MotionEvent.ACTION_DOWN -> sendCarInput(key, value)
            MotionEvent.ACTION_UP -> sendCarInput(key, "0")
        }
    }
    private fun sendCarInput(key: String, value: String) {
        val message = "$key,$value"
        wsCarInput?.send(message)
        Log.d("WebSocket-Send", "Sent: $message")
    }


    override fun onDestroy() {
        super.onDestroy()
        wsCamera?.close(1000, null)
        wsCamera = null
        wsCarInput?.close(1000, null)
        wsCarInput = null
    }
}
