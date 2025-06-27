package com.aitronbiz.arron.adapter

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.SectionItem
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.view.home.DetailFragment
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.time.LocalDate
import androidx.cardview.widget.CardView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.Item
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SectionAdapter(
    private val context: Context,
    private var subjectId: Int,
    private var deviceId: Int,
    private var date: LocalDate,
    private var sections: MutableList<SectionItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int = when (sections[position]) {
        is SectionItem.TodayActivity -> 0
        is SectionItem.DailyActivity -> 1
        is SectionItem.ResidenceTime -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> TodayActivityViewHolder(inflater.inflate(R.layout.section_today_activity, parent, false))
            1 -> DailyActivityViewHolder(inflater.inflate(R.layout.section_daily_activity, parent, false))
            2 -> ResidenceTimeViewHolder(inflater.inflate(R.layout.section_residence_time, parent, false))
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = sections[position]) {
            is SectionItem.TodayActivity ->
                (holder as TodayActivityViewHolder).bind(context, subjectId, deviceId, date)

            is SectionItem.DailyActivity ->
                (holder as DailyActivityViewHolder).bind(context, deviceId, date)

            is SectionItem.ResidenceTime -> {
                (holder as ResidenceTimeViewHolder).bind(context, deviceId, date)
            }
        }

        holder.itemView.setOnTouchListener { _, _ -> false }
    }

    override fun getItemCount(): Int = sections.size

    // subjectId와 deviceId를 갱신하는 메서드
    fun updateSubjectAndDeviceId(subjectId: Int, deviceId: Int) {
        this.subjectId = subjectId
        this.deviceId = deviceId
        notifyDataSetChanged()
    }

    // 날짜를 갱신하는 메서드
    fun updateSelectedDate(date: LocalDate) {
        this.date = date
        notifyDataSetChanged()
    }

    // sections 리스트를 갱신하는 메서드
    fun updateSections(newSections: List<SectionItem>) {
        sections = newSections.toMutableList()
        notifyDataSetChanged()
    }

    class TodayActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val btnActivity = view.findViewById<CardView>(R.id.btnActivity)
        private val circularProgress = view.findViewById<CircularProgressBar>(R.id.circularProgress)
        private val progressLabel = view.findViewById<TextView>(R.id.progressLabel)
        private val tvStatus1 = view.findViewById<TextView>(R.id.tvStatus1)
        private val tvStatus2 = view.findViewById<TextView>(R.id.tvStatus2)
        private val tvStatus3 = view.findViewById<TextView>(R.id.tvStatus3)
        private val tvAbsent = view.findViewById<TextView>(R.id.tvAbsent)

        fun bind(context: Context, subjectId: Int, deviceId: Int, date: LocalDate) {
            val dataManager = DataManager.getInstance(context)
            val roomStatus = dataManager.getRoomStatus(deviceId)
            val pct = dataManager.getDailyData(deviceId, date.toString())

            // 항상 progress 초기화
            circularProgress.progress = 0f
            circularProgress.setProgressWithAnimation(0f, 1)
            progressLabel.text = "0%"

            if(roomStatus == 1) {
                tvStatus1.visibility = View.VISIBLE
                tvStatus2.visibility = View.VISIBLE
                tvStatus3.visibility = View.VISIBLE
                tvAbsent.visibility = View.GONE

                circularProgress.setProgressWithAnimation(pct.toFloat(), 2000)
                progressLabel.text = "$pct%"

                if(pct == 0) {
                    progressLabel.setTextColor("#DDDDDD".toColorInt())
                    setTextStyle(tvStatus2, tvStatus3, tvStatus1, 2)
                }else {
                    progressLabel.setTextColor(Color.BLACK)
                    when (pct) {
                        in 1..30 -> setTextStyle(tvStatus1, tvStatus2, tvStatus3, 1)
                        in 31..70 -> setTextStyle(tvStatus1, tvStatus3, tvStatus2, 1)
                        else -> setTextStyle(tvStatus2, tvStatus3, tvStatus1, 1)
                    }
                }
            }else {
                tvStatus1.visibility = View.GONE
                tvStatus2.visibility = View.GONE
                tvStatus3.visibility = View.GONE
                tvAbsent.visibility = View.VISIBLE
            }

            btnActivity.setOnClickListener {
                if(deviceId == 0) {
                    Toast.makeText(context, "기기를 먼저 등록해주세요", Toast.LENGTH_SHORT).show()
                }else {
                    val activity = context as? AppCompatActivity
                    activity?.let {
                        val bundle = Bundle()
                        bundle.putInt("subjectId", subjectId)
                        bundle.putInt("deviceId", deviceId)
                        replaceFragment2(it.supportFragmentManager, DetailFragment(), bundle)
                    }
                }
            }
        }

        private fun setTextStyle(none1: TextView, none2: TextView, active: TextView, type: Int) {
            none1.setTextColor("#DDDDDD".toColorInt())
            none2.setTextColor("#DDDDDD".toColorInt())
            none1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
            none2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)

            when (type) {
                1 -> {
                    active.setTextColor(Color.BLACK)
                    active.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
                }
                else -> {
                    active.setTextColor("#DDDDDD".toColorInt())
                    active.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                }
            }
        }
    }

    class DailyActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val lineChart = view.findViewById<LineChart>(R.id.lineChart)
        private val tvNoData = view.findViewById<TextView>(R.id.tvNoData)

        private fun generateData(list: ArrayList<Activity>): List<Item> {
            val formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

            val hourToValueMap = list.mapNotNull { activity ->
                try {
                    val dateTime = LocalDateTime.parse(activity.createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    val hour = dateTime.hour
                    hour to activity.activity
                } catch (e: Exception) {
                    null
                }
            }.toMap()

            val baseDate = if (list.isNotEmpty()) {
                LocalDateTime.parse(list[0].createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
            } else {
                LocalDate.now()
            }

            return (0..23).map { hour ->
                val dateTime = baseDate.atTime(hour, 0)
                val timeStr = dateTime.format(formatterOutput)
                val value = hourToValueMap[hour]
                Item(value, timeStr)
            }
        }

        fun bind(context: Context, deviceId: Int, date: LocalDate) {
            val dataManager = DataManager.getInstance(context)
            val data = dataManager.getDailyActivities(deviceId, date.toString())

            if(data.isNotEmpty()) {
                lineChart.visibility = View.VISIBLE
                tvNoData.visibility = View.GONE

                val testData = generateData(data)
                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val hourlyData = MutableList(24) { 0f }

                testData.forEach { item ->
                    item.time?.let {
                        val hour = LocalDateTime.parse(it, formatter).hour
                        item.data?.let { value -> hourlyData[hour] = value.toFloat() }
                    }
                }

                val entries = hourlyData.mapIndexed { hour, value ->
                    Entry(hour.toFloat(), value)
                }

                val dataSet = LineDataSet(entries, "시간별 활동량").apply {
                    color = "#5558FF".toColorInt()
                    lineWidth = 2.7f
                    setDrawFilled(true)
                    fillColor = "#5558FF".toColorInt()
                    fillAlpha = 75
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawCircles(false)
                    setDrawValues(false)
                }

                val markerView = CustomMarkerView(context, R.layout.marker_view)
                markerView.chartView = lineChart
                lineChart.marker = markerView

                lineChart.data = LineData(dataSet)
                lineChart.xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    axisMinimum = 0f
                    axisMaximum = 23f
                    granularity = 1f
                    labelCount = 24
                    textSize = 9f
                    textColor = Color.BLACK
                    valueFormatter = HourAxisFormatter()
                    setCenterAxisLabels(false)
                    setDrawGridLines(false)
                    setAvoidFirstLastClipping(false)
                }

                lineChart.xAxis.setCenterAxisLabels(false)
                lineChart.axisRight.isEnabled = false
                lineChart.axisLeft.apply {
                    axisMaximum = 110f
                    axisMinimum = 0f
                    spaceTop = 12f
                    setDrawGridLines(false)
                }

                lineChart.setScaleEnabled(false)
                lineChart.setDragEnabled(false)
                lineChart.setTouchEnabled(true)
                lineChart.setPinchZoom(false)
                lineChart.isDoubleTapToZoomEnabled = false
                lineChart.setVisibleXRangeMaximum(24f)
                lineChart.moveViewToX(0f)
                lineChart.description.isEnabled = false
                lineChart.legend.isEnabled = false
                lineChart.invalidate()
            }else {
                lineChart.visibility = View.GONE
                tvNoData.visibility = View.VISIBLE
            }
        }
    }

    private class HourAxisFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return when (value.toInt()) {
                0 -> "오전12"
                6 -> "오전6"
                12 -> "오후12"
                18 -> "오후6"
                else -> ""
            }
        }
    }

    private class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
        private val tvContent: TextView = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            tvContent.text = e?.y?.toInt().toString()
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2).toFloat(), -height.toFloat())
        }
    }

    class ResidenceTimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(context: Context, deviceId: Int, date: LocalDate) {
        }
    }
}