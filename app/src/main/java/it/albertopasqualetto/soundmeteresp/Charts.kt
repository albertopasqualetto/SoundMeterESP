package it.albertopasqualetto.soundmeteresp

import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

enum class Charts {
    ONE_SEC_LEFT, ONE_SEC_RIGHT,
    FIVE_MIN_LEFT, FIVE_MIN_RIGHT;


    @Composable
    public operator fun invoke(updated : Int, modifier: Modifier = Modifier){
        lateinit var chart: LineChart
        updated  // to trigger recomposition

        val type = this

        AndroidView(
            modifier = modifier,
            factory = { context ->
                LineChart(context).apply {
                    chart = this
                    chart.setTouchEnabled(false)
                    chart.setDrawGridBackground(true)
                    chart.setDrawBorders(false)
                    chart.setBackgroundColor(Color.WHITE)
                    chart.setGridBackgroundColor(Color.WHITE)
                    chart.description.isEnabled = false
                    chart.legend.isEnabled = false
                    chart.axisRight.isEnabled = false
                    chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
//                    chart.xAxis.setDrawLabels(false)  // TODO uncomment
                    chart.isFocusable = false
                    chart.isClickable = false
                    chart.isLongClickable = false
                    chart.isDoubleTapToZoomEnabled = false
                    chart.isAutoScaleMinMaxEnabled = true
//                    chart.setViewPortOffsets(0f, 0f, 0f, 0f)
                    chart.axisLeft.axisMinimum = 0f
                    chart.axisLeft.axisMaximum = 100f   //200f

                    chart.isLogEnabled = false

                    val dataSet = LineDataSet(mutableListOf<Entry>(), "Label"); // add entries to dataset
                    /*dataSet.setColor(...);
                    dataSet.setValueTextColor(...); // styling, ...*/

                    val lineData = LineData(dataSet)
                    chart.data = lineData
                    chart.invalidate() // refresh
                }
            },

            update = { chart ->
                updated  // to trigger recomposition
                // update chart here
//                chart.clear()
                val data: LineData = chart.data
                val set = data.getDataSetByIndex(0)
                var newEntry : Entry? = null
                try{
                    newEntry = when (type) {
                        ONE_SEC_LEFT -> Entry(set.entryCount.toFloat(), Values.getFirstFromLastSecLeft()!!)
                        ONE_SEC_RIGHT -> Entry(set.entryCount.toFloat(), Values.getFirstFromLastSecRight()!!)
                        FIVE_MIN_LEFT -> Entry(set.entryCount.toFloat(), Values.last5MinDbLeftList[set.entryCount])
                        FIVE_MIN_RIGHT -> Entry(set.entryCount.toFloat(), Values.last5MinDbRightList[set.entryCount])
                    }
                } catch (e : Exception){
                    Log.d(MainActivity.TAG, "Chart: exception: $e, $type")
                    return@AndroidView
                }

                data.addEntry(newEntry, 0)

                if (set.entryCount > when(type){
                        ONE_SEC_LEFT -> 100
                        ONE_SEC_RIGHT -> 100
                        FIVE_MIN_LEFT -> 60*5
                        FIVE_MIN_RIGHT -> 60*5
                    }) {
                    set.removeFirst()
                }
//                chart.data = LineData(LineDataSet(data, "Label"))
//                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()    // let the chart know it's data changed
                chart.invalidate()
                Log.d(MainActivity.TAG, "Chart: updated $type")
            }
        )

        SideEffect {
            Log.d(TAG, "Chart: recomposing")
        }
    }

    companion object {
        private val TAG = Charts::class.simpleName
    }
}
