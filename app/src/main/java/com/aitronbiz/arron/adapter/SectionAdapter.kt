package com.aitronbiz.arron.adapter

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Activity
import com.aitronbiz.arron.entity.Item
import com.aitronbiz.arron.view.home.CustomLineChartView
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
    private var roomId: Int,
    private var deviceId: Int,
    private var date: LocalDate,
    private var sections: MutableList<SectionItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int = when (sections[position]) {
        is SectionItem.TodayActivity -> 0
        is SectionItem.DailyActivity -> 1
        is SectionItem.DailyMission -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> TodayActivityViewHolder(inflater.inflate(R.layout.section_today_activity, parent, false))
            1 -> DailyActivityViewHolder(inflater.inflate(R.layout.section_daily_activity, parent, false))
            2 -> DailyMissionViewHolder(inflater.inflate(R.layout.section_daily_mission, parent, false))
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = sections[position]) {
            is SectionItem.TodayActivity ->
                (holder as TodayActivityViewHolder).bind(context, roomId, deviceId, date)
            is SectionItem.DailyActivity ->
                (holder as DailyActivityViewHolder).bind(context, deviceId, date)
            is SectionItem.DailyMission ->
                (holder as DailyMissionViewHolder).bind(context, deviceId, date)
        }

        holder.itemView.setOnTouchListener { _, _ -> false }
    }

    override fun getItemCount(): Int = sections.size

    // roomId와 deviceId를 갱신하는 메서드
    fun updateRoomAndDeviceId(roomId: Int, deviceId: Int) {
        this.roomId = roomId
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
        private val btnActivity = view.findViewById<ConstraintLayout>(R.id.btnActivity)
        private val circularProgress = view.findViewById<CircularProgressBar>(R.id.circularProgress)
        private val progressLabel = view.findViewById<TextView>(R.id.progressLabel)
        private val tvStatus1 = view.findViewById<TextView>(R.id.tvStatus1)
        private val tvStatus2 = view.findViewById<TextView>(R.id.tvStatus2)
        private val tvStatus3 = view.findViewById<TextView>(R.id.tvStatus3)
        private val tvAbsent = view.findViewById<TextView>(R.id.tvAbsent)

        fun bind(context: Context, roomId: Int, deviceId: Int, date: LocalDate) {
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
                        bundle.putInt("roomId", roomId)
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

        private val customChart = view.findViewById<CustomLineChartView>(R.id.customChart)
        private val tvNoData = view.findViewById<TextView>(R.id.tvNoData)

        private fun generateData(list: ArrayList<Activity>): List<Float> {
            val hourlyData = MutableList(24) { 0f }
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

            list.forEach { activity ->
                try {
                    val dateTime = LocalDateTime.parse(activity.createdAt, formatter)
                    val hour = dateTime.hour
                    hourlyData[hour] = activity.activity.toFloat()
                } catch (e: Exception) {
                    // 무시
                }
            }

            return hourlyData
        }

        fun bind(context: Context, deviceId: Int, date: LocalDate) {
            val dataManager = DataManager.getInstance(context)
            val data = dataManager.getDailyActivities(deviceId, date.toString())

            if (data.isNotEmpty()) {
                customChart.visibility = View.VISIBLE
                tvNoData.visibility = View.GONE

                val hourlyData = generateData(data)
                customChart.hourlyData = hourlyData
                customChart.invalidate()

            } else {
                customChart.visibility = View.GONE
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

    class DailyMissionViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(context: Context, deviceId: Int, date: LocalDate) {
            val dataManager = DataManager.getInstance(context)

        }
    }
}