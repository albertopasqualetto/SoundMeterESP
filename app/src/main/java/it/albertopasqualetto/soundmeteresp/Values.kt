@file:JvmName("Values")
package it.albertopasqualetto.soundmeteresp

import java.util.concurrent.ConcurrentLinkedQueue


object Values {
    private var leftQueue : ConcurrentLinkedQueue<Float> = ConcurrentLinkedQueue<Float>()
    private var rightQueue : ConcurrentLinkedQueue<Float> = ConcurrentLinkedQueue<Float>()
    private var lastSecDbLeftList : MutableList<Float> = mutableListOf<Float>()  // max size = 1 sec = Meter.SAMPLE_RATE
    private var lastSecDbRightList : MutableList<Float> = mutableListOf<Float>()  // max size = 1 sec = Meter.SAMPLE_RATE
    private var last5MinDbLeftQueue : ConcurrentLinkedQueue<Float> = ConcurrentLinkedQueue<Float>()  // max size = 5 min = 1*60*5
    private var last5MinDbRightQueue : ConcurrentLinkedQueue<Float> = ConcurrentLinkedQueue<Float>()  // max size = 5 min = 1*60*5
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
    
    fun updateLast5MinDbLeft() {
        while (last5MinDbLeftQueue.size >= 1*60*5) { last5MinDbLeftQueue.poll() }
        last5MinDbLeftQueue.offer(lastSecDbLeftList.average().toFloat())
    }

    fun updateLast5MinDbRight() {
        while (last5MinDbRightQueue.size >= 1*60*5) { last5MinDbRightQueue.poll() }
        last5MinDbRightQueue.offer(lastSecDbRightList.average().toFloat())
    }

    fun getMaxDbLastSec() : FloatArray {
        val maxLeft = lastSecDbLeftList.maxOrNull() ?: 0f
        val maxRight = lastSecDbRightList.maxOrNull() ?: 0f
        return floatArrayOf(maxLeft, maxRight)
    }

    fun getFirstFromLastSecLeft() : Float? {
        val out = leftQueue.poll()
        if ( out != null ) {
            leftCount++
        }

        if (leftCount >= Meter.SAMPLE_RATE) {   // 1 sec
            leftCount = 0
            updateLast5MinDbLeft()
            lastSecDbLeftList.clear()
        }
        return out
    }

    fun getFirstFromLastSecRight() : Float? {
        val out = rightQueue.poll()
        if ( out != null ) {
            rightCount++
        }

        if (leftCount >= Meter.SAMPLE_RATE) {   // 1 sec
            leftCount = 0
            updateLast5MinDbRight()
            lastSecDbRightList.clear()
        }
        return out
    }
}