@file:JvmName("Values")
package it.albertopasqualetto.soundmeteresp

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue


object Values {
    private var leftQueue : ConcurrentLinkedQueue<Float> = ConcurrentLinkedQueue<Float>()
    private var rightQueue : ConcurrentLinkedQueue<Float> = ConcurrentLinkedQueue<Float>()
    private var lastSecDbLeftList : MutableList<Float> = mutableListOf<Float>()  // max size = 1 sec = Meter.SAMPLE_RATE
    private var lastSecDbRightList : MutableList<Float> = mutableListOf<Float>()  // max size = 1 sec = Meter.SAMPLE_RATE
    var last5MinDbLeftList : MutableList<Float> = mutableListOf<Float>()  // max size = 5 min = 1*60*5
    var last5MinDbRightList: MutableList<Float> = mutableListOf<Float>()  // max size = 5 min = 1*60*5
    private var leftCount = 0
    private var rightCount = 0


    fun updateLastSecDbVec(measuredVals: List<FloatArray>) {
        leftQueue.clear()
        leftQueue.addAll(measuredVals[0].toList())
//        while (lastSecDbLeftQueue.size >= Meter.SAMPLE_RATE) { lastSecDbLeftQueue.poll() }
        rightQueue.clear()
        rightQueue.addAll(measuredVals[1].toList())
//        while (lastSecDbRightQueue.size >= Meter.SAMPLE_RATE) { lastSecDbRightQueue.poll() }
    }
    
    private fun updateLast5MinDbLeft(newMaxLeft: Float) {
        while (last5MinDbLeftList.size >= 1*60*5) { last5MinDbLeftList.removeFirst() }
        last5MinDbLeftList.add(newMaxLeft)
    }

    private fun updateLast5MinDbRight(newMaxRight: Float) {
        while (last5MinDbRightList.size >= 1*60*5) { last5MinDbRightList.removeFirst() }
        last5MinDbRightList.add(newMaxRight)
    }

    fun getMaxDbLastSec() : FloatArray {
        // TODO only changes if there is an higher value
        val maxLeft = lastSecDbLeftList.maxOrNull() ?: if (lastSecDbLeftList.size == 1) lastSecDbLeftList.first() else 0f
        updateLast5MinDbLeft(maxLeft)
        val maxRight = lastSecDbRightList.maxOrNull() ?: if (lastSecDbRightList.size == 1) lastSecDbRightList.first() else 0f
        updateLast5MinDbLeft(maxRight)
        Log.d(MainActivity.TAG, "Values: getMaxDbLastSec: $maxLeft, $maxRight, sizes: ${lastSecDbLeftList.size}, ${lastSecDbRightList.size}")
        return floatArrayOf(maxLeft, maxRight)
    }

    fun getFirstFromLastSecLeft() : Float? {
        val out = leftQueue.poll()
        if ( out != null ) {
            leftCount++
            lastSecDbLeftList.add(out)
        }
        return out
    }

    fun getFirstFromLastSecRight() : Float? {
        val out = rightQueue.poll()
        if ( out != null ) {
            rightCount++
            lastSecDbRightList.add(out)
        }
        return out
    }
}