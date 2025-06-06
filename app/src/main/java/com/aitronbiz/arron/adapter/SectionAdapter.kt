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
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.entity.SectionItem
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.view.device.AddDeviceFragment
import com.aitronbiz.arron.view.home.DetailFragment
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import org.threeten.bp.LocalDate

class SectionAdapter(
    private val context: Context,
    private var subjectId: Int,
    private var deviceId: Int,
    private var sections: MutableList<SectionItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int = when (sections[position]) {
        is SectionItem.TodayActivity -> 0
        is SectionItem.WeeklyActivity -> 1
        is SectionItem.ResidenceTime -> 2
        is SectionItem.SmartEnergy -> 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> TodayActivityViewHolder(inflater.inflate(R.layout.section_today_activity, parent, false))
            1 -> WeeklyActivityViewHolder(inflater.inflate(R.layout.section_weekly_activity, parent, false))
            2 -> ResidenceTimeViewHolder(inflater.inflate(R.layout.section_residence_time, parent, false))
            3 -> SmartEnergyViewHolder(inflater.inflate(R.layout.section_smart_energy, parent, false))
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = sections[position]) {
            is SectionItem.TodayActivity -> (holder as TodayActivityViewHolder).bind(context, subjectId, deviceId)
            is SectionItem.WeeklyActivity -> {

            }
            is SectionItem.ResidenceTime -> {

            }
            is SectionItem.SmartEnergy -> {

            }
        }
    }

    override fun getItemCount(): Int = sections.size

    fun moveItem(from: Int, to: Int) {
        val item = sections.removeAt(from)
        sections.add(to, item)
        notifyItemMoved(from, to)
    }

    fun getSections(): List<SectionItem> = sections.toList()

    fun updateDeviceId(newId: Int) {
        deviceId = newId
        notifyDataSetChanged()
    }

    fun updateSections(newSections: List<SectionItem>) {
        sections = newSections.toMutableList()
        notifyDataSetChanged()
    }

    class TodayActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val circularProgress = view.findViewById<CircularProgressBar>(R.id.circularProgress)
        private val progressLabel = view.findViewById<TextView>(R.id.progressLabel)
        private val tvStatus1 = view.findViewById<TextView>(R.id.tvStatus1)
        private val tvStatus2 = view.findViewById<TextView>(R.id.tvStatus2)
        private val tvStatus3 = view.findViewById<TextView>(R.id.tvStatus3)
        private val tvAbsent = view.findViewById<TextView>(R.id.tvAbsent)
        private val btnDetail = view.findViewById<ConstraintLayout>(R.id.btnDetail)

        fun bind(context: Context, subjectId: Int, deviceId: Int) {
            val dataManager = DataManager.getInstance(context)
            val roomStatus = dataManager.getRoomStatus(deviceId)
            val pct = dataManager.getDailyData(deviceId, LocalDate.now().toString())

            if (pct != 0) {
                tvStatus1.visibility = View.VISIBLE
                tvStatus2.visibility = View.VISIBLE
                tvStatus3.visibility = View.VISIBLE

                circularProgress.setProgressWithAnimation(pct.toFloat(), 2000)
                progressLabel.text = "$pct%"

                when (pct) {
                    in 0..30 -> setTextStyle(tvStatus1, tvStatus2, tvStatus3, 1)
                    in 31..70 -> setTextStyle(tvStatus1, tvStatus3, tvStatus2, 1)
                    else -> setTextStyle(tvStatus2, tvStatus3, tvStatus1, 1)
                }
            } else {
                setTextStyle(tvStatus2, tvStatus3, tvStatus1, 2)
            }

            if (roomStatus== 1) {
                tvStatus1.visibility = View.VISIBLE
                tvStatus2.visibility = View.VISIBLE
                tvStatus3.visibility = View.VISIBLE
                tvAbsent.visibility = View.GONE
            } else {
                tvStatus1.visibility = View.GONE
                tvStatus2.visibility = View.GONE
                tvStatus3.visibility = View.GONE
                tvAbsent.visibility = View.VISIBLE
            }

            btnDetail.setOnClickListener {
                val activity = context as? AppCompatActivity
                activity?.let {
                    val bundle = Bundle()
                    bundle.putInt("subjectId", subjectId)
                    bundle.putInt("deviceId", deviceId)
                    replaceFragment2(it.supportFragmentManager, DetailFragment(), bundle)
                }
            }
        }

        private fun setTextStyle(none1: TextView, none2: TextView, active: TextView, type: Int) {
            none1.setTextColor("#CCCCCC".toColorInt())
            none2.setTextColor("#CCCCCC".toColorInt())
            none1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
            none2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
            when (type) {
                1 -> {
                    active.setTextColor(Color.BLACK)
                    active.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
                }
                else -> {
                    active.setTextColor("#CCCCCC".toColorInt())
                    active.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                }
            }
        }
    }

    class WeeklyActivityViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class ResidenceTimeViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class SmartEnergyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}