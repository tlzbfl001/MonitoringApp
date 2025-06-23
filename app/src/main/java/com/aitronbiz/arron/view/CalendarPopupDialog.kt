package com.aitronbiz.arron.view

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.aitronbiz.arron.R
import androidx.core.graphics.drawable.toDrawable

class CalendarPopupDialog : DialogFragment() {
    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // 레이아웃 크기 및 위치
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.TOP)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            // 상태바 배경색 흰색
            window.statusBarColor = Color.WHITE

            // 상태바 아이콘 검정색
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_calendar_popup, container, false)

        view.findViewById<ConstraintLayout>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.calendarContainer, CalendarFragment())
            .commit()

        return view
    }
}