@file:JvmName("Values")
package it.albertopasqualetto.soundmeteresp

import java.util.concurrent.ConcurrentLinkedQueue


object Values {
    var lastSecDbLeftList : ConcurrentLinkedQueue<Double> = ConcurrentLinkedQueue<Double>()  // max size = 1 sec = Meter.SAMPLE_RATE
    var lastSecDbRightList : ConcurrentLinkedQueue<Double> = ConcurrentLinkedQueue<Double>()  // max size = 1 sec = Meter.SAMPLE_RATE
    var last5MinDbLeftList : ConcurrentLinkedQueue<Double> = ConcurrentLinkedQueue<Double>()  // max size = 5 min = 1*60*5
    var last5MinDbRightList : ConcurrentLinkedQueue<Double> = ConcurrentLinkedQueue<Double>()  // max size = 5 min = 1*60*5

    fun lastSecDbVecUpdate(measuredVals: List<DoubleArray>) {
        lastSecDbLeftList.addAll(measuredVals[0].toList())
        while (lastSecDbLeftList.size >= Meter.SAMPLE_RATE) { lastSecDbLeftList.poll() }
        lastSecDbRightList.addAll(measuredVals[1].toList())
        while (lastSecDbRightList.size >= Meter.SAMPLE_RATE) { lastSecDbRightList.poll() }
    }
    
    fun last5MinDbVecUpdate() {
        if (last5MinDbLeftList.size >= 1*60*5) { last5MinDbLeftList.poll() }
        last5MinDbLeftList.offer(lastSecDbLeftList.average())
        if (last5MinDbRightList.size >= 1*60*5) { last5MinDbRightList.poll() }
        last5MinDbRightList.offer(lastSecDbRightList.average())
    }

    fun getMaxDbLastSec() : DoubleArray {
        val maxLeft = lastSecDbLeftList.maxOrNull() ?: 0
        val maxRight = lastSecDbRightList.maxOrNull() ?: 0
        return doubleArrayOf(maxLeft as Double, maxRight as Double)
    }
}