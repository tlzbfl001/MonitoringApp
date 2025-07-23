package com.aitronbiz.arron.view.home

import android.graphics.Color
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
    private var homeId = ""

    companion object {
        fun newInstance(homeId: String): CalendarPopupDialog {
            val dialog = CalendarPopupDialog()
            val args = Bundle().apply {
                putString("homeId", homeId)
            }
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            homeId = it.getString("homeId")!!
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.TOP)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            window.statusBarColor = Color.WHITE
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_calendar_popup, container, false)

        view.findViewById<ConstraintLayout>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        val fragment = CalendarFragment.newInstance(homeId)

        childFragmentManager.beginTransaction()
            .replace(R.id.calendarContainer, fragment)
            .commit()

        return view
    }
}
