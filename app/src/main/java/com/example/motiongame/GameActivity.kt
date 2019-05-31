package com.example.motiongame

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.Point
import android.os.Handler
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import java.util.*
import kotlin.math.floor
import kotlin.random.Random.Default.nextFloat


class GameActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mSensorManager : SensorManager
    private lateinit var mAccelerometer : Sensor
    private lateinit var mLightSensor : Sensor

    private var boxY : Float = 80F
    private var boxSize = 0

    private lateinit var orangePoint : gameObject
    private lateinit var pinkPoint : gameObject
    private lateinit var spike : gameObject

    private var frameHeight = 0
    private var screenWidth = 0F
    private var screenHeight = 0F

    private var mHandler : Handler = Handler()
    private var mTimer : Timer = Timer()

    private var isGameStopped = true
    private var isGameStarted = false

    private var score = 0

    private var speedMultiplier = 1F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this,mLightSensor,SensorManager.SENSOR_DELAY_UI)

        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        screenWidth = size.x.toFloat()
        screenHeight = size.y.toFloat()

        orangePoint = gameObject(0,-80F,-80F,orangePointView, 20F, 12F, 10)
        pinkPoint = gameObject(1,-80F,-80F,pinkPointView,5000F, 20F, 30)
        spike = gameObject(2,-80F,-80F,spikeView,10F, 16F, 0)

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(!isGameStopped){
            if ((event!!.sensor.type == Sensor.TYPE_ACCELEROMETER)) {
                boxY -= event.values[0] * 5
                if (boxY < 0F) boxY = 0F
                if (boxY > frameHeight - boxSize) boxY = (frameHeight - boxSize).toFloat()
                boxView.y = boxY
            }
            if (event.sensor.type == Sensor.TYPE_LIGHT){
                speedMultiplier = if(event.values[0] < 10) { 0.5F } else { 1F }
            }
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(!isGameStarted) initialiseGame()
        return true
    }

    fun initialiseGame(){
        isGameStarted = true
        isGameStopped = false
        frameHeight = gameField.height
        boxSize = boxView.height
        mTimer.schedule(object : TimerTask() {
            override fun run() {
                mHandler.post(Runnable { if(!isGameStopped)updateGame() })
            }
        }, 0, 20)

        startText.visibility = View.INVISIBLE
        orangePointView.visibility = View.VISIBLE
        pinkPointView.visibility = View.VISIBLE
        spikeView.visibility = View.VISIBLE
        boxView.visibility = View.VISIBLE
    }

    fun updateGame(){
        hitCheck(orangePoint)
        hitCheck(pinkPoint)
        hitCheck(spike)
        moveObject(orangePoint)
        moveObject(pinkPoint)
        moveObject(spike)
    }

    fun hitCheck(objectToCheck: gameObject){

        var objectCenterX = objectToCheck.xCrd + objectToCheck.linkedView.width / 2
        var objectCenterY = objectToCheck.yCrd + objectToCheck.linkedView.height / 2

        if(0F <= objectCenterX && objectCenterX <= boxSize && boxY <= objectCenterY && objectCenterY <= boxY + boxSize){
            if(objectToCheck.objectScore != 0) {
                score += objectToCheck.objectScore
                scoreView.text = score.toString()
                objectToCheck.xCrd = -10F
            }else{
                isGameStopped = true
                mTimer.cancel()
                AlertDialog.Builder(this)
                    .setTitle("Your score is: " + score)
                    .setMessage("Do you want to retry?")
                    .setPositiveButton("Yes"){dialog, which ->
                        recreate()
                    }
                    .setNegativeButton("No"){dialog, which ->
                        mTimer.cancel()
                        finish()
                    }
                    .setOnCancelListener { recreate()}
                    .show()
            }
        }
    }

    fun moveObject(objectToMove: gameObject){
        objectToMove.xCrd -= objectToMove.baseSpeed * speedMultiplier
        if (objectToMove.xCrd < 0F){
            objectToMove.xCrd = screenWidth + objectToMove.startOffset
            objectToMove.yCrd = floor(nextFloat() * (frameHeight - objectToMove.linkedView.height))
        }
        objectToMove.linkedView.x = objectToMove.xCrd
        objectToMove.linkedView.y = objectToMove.yCrd
    }


    override fun onBackPressed() {
        isGameStopped = true
        AlertDialog.Builder(this)
            .setTitle("Pause")
            .setMessage("Do you really want to exit?")
            .setPositiveButton("Yes"){dialog, which ->
                mTimer.cancel()
                finish()
            }
            .setNegativeButton("No"){dialog, which ->
                isGameStopped = false
            }
            .setOnCancelListener { isGameStopped = false }
            .show()
    }
}

