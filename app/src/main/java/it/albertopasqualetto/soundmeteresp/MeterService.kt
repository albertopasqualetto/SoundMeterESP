package it.albertopasqualetto.soundmeteresp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log


class MeterService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var recordThread: AudioRecordThread
    private lateinit var readThread: AudioReadThread

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        meter = AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE)

        // Create the NotificationChannel, but only on API level 26+ because
        // the NotificationChannel class is new and not in the support library.
        // See https://developer.android.com/training/notify-user/channels
        val channel = NotificationChannel(CHANNEL_ID, "SoundMeterESP", NotificationManager.IMPORTANCE_LOW)
        channel.description = "SoundMeterESP"
        // Register the channel with the system
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // Clients can not bind to this service
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (isRecording) return START_NOT_STICKY

        // Build a notification
        val notificationBuilder: Notification.Builder =
            Notification.Builder(applicationContext, CHANNEL_ID)
        notificationBuilder.setContentTitle("SoundMeterESP")
        notificationBuilder.setContentText("Recording sounds...")
        notificationBuilder.setSmallIcon(R.mipmap.ear_launcher_round)
        val goToMainActivityIntent = Intent(applicationContext, MainActivity::class.java).apply {
//            this.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, goToMainActivityIntent, PendingIntent.FLAG_IMMUTABLE)
        notificationBuilder.setContentIntent(pendingIntent)
        val notification = notificationBuilder.build() // Requires API level 16
        // Runs this service in the foreground,
        // supplying the ongoing notification to be shown to the user
        val notificationID = 2000162 // An ID for this notification unique within the app
        startForeground(notificationID, notification)
        Log.d(TAG, "rec: startForeground")

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoundMeterESP::$TAG").apply {
                acquire(10*60*1000L /*10 minutes*/)
            }
        }


//        t = thread(start = true, isDaemon = true, name = "MeterServiceThread") { rec() }
        Log.d(TAG, "Start recording thread")
        recordThread = AudioRecordThread()
        recordThread.start()

        Log.d(TAG, "Start reading thread")
        readThread = AudioReadThread()
        readThread.start()

        return START_NOT_STICKY
    }

    override fun onDestroy()
    {
        readThread.stopReading()
        recordThread.stopRecording()

        if (meter?.state == AudioRecord.STATE_INITIALIZED)
            meter?.release() ?: Log.d(TAG, "meter was not initialized")
        meter = null

        recordThread.interrupt()

        stopForeground(STOP_FOREGROUND_REMOVE)

        wakeLock?.release()

        super.onDestroy()
    }


    private inner class AudioRecordThread : Thread("AudioRecordThread") {
        init {
            isDaemon = true
        }

        override fun run() {
            if (meter == null) Log.d(TAG, "rec: meter is null")
            isRecording = true
            meter?.startRecording()

            sleep(1000)
            // Start recording loop
            while (isRecording)
                if (meter?.recordingState == AudioRecord.RECORDSTATE_RECORDING) readLeftRightMeter(meter!!)

            // Release AudioRecord resources here
            Log.d(TAG, "state: "+meter?.state.toString())
            Log.d(TAG, "recordingState: "+meter?.recordingState.toString())
            if (meter?.recordingState == AudioRecord.RECORDSTATE_RECORDING) meter?.stop()
        }

        fun stopRecording() {
            // Set isRecording to false to stop the recording loop
            isRecording = false
        }
    }

    private inner class AudioReadThread : Thread("AudioReadThread") {
        init {
            isDaemon = true
        }
        var isReading = true

        override fun run() {
            super.run()
            sleep(2000)

            var countToSec = 0  // count to 16,6 = 1 sec
            while(isReading){
                countToSec++
                if (countToSec >= 62) { // 1000/16 ~= 62
                    countToSec = 0
                    Values.getMaxDbLastSec()
//                    Log.d(TAG, "Saved last second's data")
                }

                Values.getFirstFromQueueLeft()
                Values.getFirstFromQueueRight()
//                Log.d(TAG, "Saved real time data")
                sleep(16)   // ~16 ms = 60 Hz
            }
        }

        fun stopReading() {
            // Set isReading to false to stop the reading loop
            isReading = false
        }
    }


    private fun readLeftRightMeter(meter: AudioRecord) {
        val buf : ShortArray = ShortArray(BUFFER_SIZE)
        var readN = 0

        try{
            readN += meter.read(buf, 0, BUFFER_SIZE)
            if (readN == 0) Log.d(TAG, "readN=0")
        }catch (e: Exception){
            Log.d(TAG, e.toString())
            return
        }
        val left = buf.slice(0 until readN step 2).map { Values.PCMtoDB(it) }.toFloatArray()
        val right = buf.slice(1 until readN step 2).map { Values.PCMtoDB(it) }.toFloatArray()
        Log.d(TAG, "readLeftRightMeter: left: ${left.size} right: ${right.size}")
        Values.updateQueues(left, right, readN)
    }


    companion object
    {
        private var meter: AudioRecord? = null

        private const val CHANNEL_ID = "soundmeteresp"
        const val REC_START = "MeterStart"
        const val REC_STOP = "MeterStop"

        var isRecording = false

        private val TAG = MeterService::class.simpleName

        const val AUDIO_SOURCE = MediaRecorder.AudioSource.UNPROCESSED
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2    // under load this will guarantee a smooth recording
        val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
    }




}

