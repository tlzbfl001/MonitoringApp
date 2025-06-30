package com.aitronbiz.arron.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.R
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import java.io.File

class SubjectAdapter(
    private val list: List<Subject>
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {
    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
        context = parent.context
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.tvName.text = list[position].name

        if(list[position].image != "") {
            val imgPath = context!!.filesDir.toString() + "/" + list[position].image
            val file = File(imgPath)
            if(file.exists()){
                val bm = BitmapFactory.decodeFile(imgPath)
                holder.ivSubject.setImageBitmap(bm)
            }
        }
    }

    inner class SubjectViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val ivSubject: ImageView = view.findViewById(R.id.ivSubject)
        val tvName: TextView = view.findViewById(R.id.tvName)
    }

    override fun getItemCount(): Int = list.size
}