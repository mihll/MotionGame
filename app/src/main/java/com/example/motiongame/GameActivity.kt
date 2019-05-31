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

    private var gameFieldHeight = 0F
    private var screenWidth = 0F
    private var screenHeight = 0F

    private lateinit var mHandler : Handler
    private lateinit var mTimer : Timer

    private var isGameStopped = true
    private var isGameInitialised = false

    private lateinit var orangePoint : GameObject
    private lateinit var pinkPoint : GameObject
    private lateinit var spike : GameObject
    private lateinit var box : GameObject

    private var score = 0
    private var speedMultiplier = 1F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this,mLightSensor,SensorManager.SENSOR_DELAY_UI)

        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        screenWidth = size.x.toFloat()
        screenHeight = size.y.toFloat()

        orangePoint = GameObject(-80F,-80F,orangePointView, 20F, 12F, 10)
        pinkPoint = GameObject(-80F,-80F,pinkPointView,5000F, 20F, 30)
        spike = GameObject(-80F,-80F,spikeView,10F, 16F, 0)
        box = GameObject(0F,80F,boxView,0F,10F,-1)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(!isGameStopped){
            if ((event!!.sensor.type == Sensor.TYPE_ACCELEROMETER)) {
                box.yCrd -= event.values[0] * box.baseSpeed
                if (box.yCrd < 0F) box.yCrd = 0F
                if (box.yCrd > gameFieldHeight - box.linkedView.height) box.yCrd = (gameFieldHeight - box.linkedView.height)
                boxView.y = box.yCrd
            }
            if (event.sensor.type == Sensor.TYPE_LIGHT){
                speedMultiplier = if(event.values[0] < 10) { 0.5F } else { 1F }
            }
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(!isGameInitialised) initialiseGame()
        return true
    }

    private fun initialiseGame(){
        isGameInitialised = true
        isGameStopped = false
        gameFieldHeight = gameField.height.toFloat()

        mHandler = Handler()
        mTimer = Timer()
        mTimer.schedule(object : TimerTask() {
            override fun run() {
                mHandler.post { if(!isGameStopped) updateGame() }
            }
        }, 0, 20)

        startText.visibility = View.INVISIBLE
        orangePointView.visibility = View.VISIBLE
        pinkPointView.visibility = View.VISIBLE
        spikeView.visibility = View.VISIBLE
        boxView.visibility = View.VISIBLE
    }

    private fun updateGame(){
        hitCheck(orangePoint)
        hitCheck(pinkPoint)
        hitCheck(spike)
        moveObject(orangePoint)
        moveObject(pinkPoint)
        moveObject(spike)
    }

    private fun hitCheck(objectToCheck: GameObject){

        var objectCenterX = objectToCheck.xCrd + objectToCheck.linkedView.width / 2
        var objectCenterY = objectToCheck.yCrd + objectToCheck.linkedView.height / 2

        if(0F <= objectCenterX && objectCenterX <= box.linkedView.height && box.yCrd <= objectCenterY && objectCenterY <= box.yCrd + box.linkedView.height){
            if(objectToCheck.objectScore != 0) {
                score += objectToCheck.objectScore
                scoreView.text = score.toString()
                objectToCheck.xCrd = -10F
            }else{
                isGameStopped = true
                mTimer.cancel()
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.mg_retry_score_dialogTitle,score.toString()))
                    .setMessage(getString(R.string.mg_retry_message))
                    .setPositiveButton(getString(R.string.mg_yes_dialogButton)){ _, _ ->
                        recreate()
                    }
                    .setNegativeButton(getString(R.string.mg_no_dialogButton)){ _, _ ->
                        mTimer.cancel()
                        finish()
                    }
                    .setOnCancelListener {recreate()}
                    .show()
            }
        }
    }

    private fun moveObject(objectToMove: GameObject){
        objectToMove.xCrd -= objectToMove.baseSpeed * speedMultiplier
        if (objectToMove.xCrd < 0F){
            objectToMove.xCrd = screenWidth + objectToMove.startOffset
            objectToMove.yCrd = floor(nextFloat() * (gameFieldHeight - objectToMove.linkedView.height))
        }
        objectToMove.linkedView.x = objectToMove.xCrd
        objectToMove.linkedView.y = objectToMove.yCrd
    }


    override fun onBackPressed() {
        isGameStopped = true
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.mg_pause_dialogTitle))
            .setMessage(getString(R.string.mg_pause_dialogMessage))
            .setPositiveButton(R.string.mg_yes_dialogButton){ _, _ ->
                mTimer.cancel()
                finish()
            }
            .setNegativeButton(R.string.mg_no_dialogButton){_, _ ->
                isGameStopped = false
            }
            .setOnCancelListener { isGameStopped = false }
            .show()
    }
}

