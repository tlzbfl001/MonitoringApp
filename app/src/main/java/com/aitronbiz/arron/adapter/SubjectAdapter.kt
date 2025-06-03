package com.aitronbiz.arron.adapter

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import java.io.File
import androidx.core.graphics.toColorInt
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.Subject

class SubjectAdapter(
    private val subjects: MutableList<Subject>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var itemClickListener: OnItemClickListener? = null
    private var addClickListener: (() -> Unit)? = null
    private var selectedPosition = 0

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_ADD = 1
    }

    override fun getItemCount(): Int = subjects.size + 1 // +1 for add button

    override fun getItemViewType(position: Int): Int {
        return if (position == subjects.size) TYPE_ADD else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
            SubjectViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add, parent, false)
            AddViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SubjectViewHolder) {
            val subject = subjects[position]
            holder.nameText.text = subject.name

            if (!subject.image.isNullOrEmpty()) {
                holder.imageView.setImageURI(Uri.fromFile(File(subject.image!!)))
            } else {
                holder.imageView.setImageResource(R.drawable.ic_launcher_foreground)
            }

            val isSelected = position == selectedPosition
            holder.mainView.setCardBackgroundColor(if(isSelected) "#AAAAAA".toColorInt() else "#EEEEEE".toColorInt())
            holder.nameText.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            holder.nameText.setTextColor(if (isSelected) Color.BLACK else "#AAAAAA".toColorInt())

            if (subject.status == EnumData.NORMAL.name) {
                holder.signLabel.visibility = View.GONE
            } else {
                holder.signLabel.visibility = View.VISIBLE
                holder.signLabel.text = if (subject.status == EnumData.CAUTION.name) "주의" else "경고"
                holder.signLabel.setBackgroundColor(
                    if (subject.status == EnumData.CAUTION.name) "#FFA500".toColorInt() else Color.RED
                )
                blinkAnimation(holder.signLabel)
            }

            holder.itemView.setOnClickListener {
                itemClickListener?.onItemClick(position)
            }
        } else if (holder is AddViewHolder) {
            holder.itemView.setOnClickListener {
                addClickListener?.invoke()
            }
        }
    }

    fun setOnAddClickListener(listener: () -> Unit) {
        this.addClickListener = listener
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.itemClickListener = listener
    }

    private fun blinkAnimation(view: View) {
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setSelectedPosition(newPosition: Int) {
        val oldPosition = selectedPosition
        selectedPosition = newPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
    }

    class SubjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mainView: CardView = view.findViewById(R.id.mainView)
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val nameText: TextView = view.findViewById(R.id.tvName)
        val signLabel: TextView = view.findViewById(R.id.signLabel)
    }

    class AddViewHolder(view: View) : RecyclerView.ViewHolder(view)
}