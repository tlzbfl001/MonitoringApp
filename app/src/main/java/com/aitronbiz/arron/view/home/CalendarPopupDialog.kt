package com.aitronbiz.arron.view.home

import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.SelectHomeDialogAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.DeviceFragment
import com.aitronbiz.arron.view.notification.NotificationFragment
import com.aitronbiz.arron.view.setting.SettingsFragment
import com.aitronbiz.arron.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class CalendarPopupDialog : DialogFragment() {
    private var homeId = ""
    private lateinit var viewModel: MainViewModel
    private var homeDialog: BottomSheetDialog? = null
    private var homeSelectedListener: OnHomeSelectedListener? = null
    private var tvHome: TextView? = null

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
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        arguments?.let {
            homeId = it.getString("homeId") ?: ""
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setGravity(Gravity.TOP)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            window.addFlags(Window.FEATURE_NO_TITLE)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }

            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.BLACK
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_calendar_popup, container, false)

        tvHome = view.findViewById(R.id.tvHome)
        val selectedDate = viewModel.selectedDate.value ?: LocalDate.now()
        val selectedYear = selectedDate.year
        val selectedMonth = selectedDate.monthValue - 1

        setStatusBar(requireActivity(), view.findViewById(R.id.mainLayout))

        setupHomeDialog()

        view.findViewById<View>(R.id.btnClose)?.setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.dialogRoot).setOnClickListener { dismiss() }

        tvHome!!.setOnClickListener { homeDialog!!.show() }

        view.findViewById<ConstraintLayout>(R.id.btnAlarm).setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, NotificationFragment())
            dismiss()
        }

        view.findViewById<ConstraintLayout>(R.id.btnSetting).setOnClickListener { view ->
            showCustomPopupWindow(view)
        }

        val fragment = CalendarFragment.newInstance(homeId, selectedYear, selectedMonth)
        childFragmentManager.beginTransaction()
            .replace(R.id.calendarContainer, fragment)
            .commit()

        return view
    }

    private fun setupHomeDialog() {
        homeDialog = BottomSheetDialog(requireContext())
        val homeDialogView = layoutInflater.inflate(R.layout.dialog_select_home, null)
        val homeRecyclerView = homeDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnAddHome = homeDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)
        homeDialog!!.setContentView(homeDialogView)

        btnAddHome.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, HomeFragment())
            homeDialog?.dismiss()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getAllHome("Bearer ${AppController.prefs.getToken()}")

                if (response.isSuccessful) {
                    val homes = response.body()!!.homes

                    withContext(Dispatchers.Main) {
                        val selectedIndex = homes.indexOfFirst { it.id == homeId }.coerceAtLeast(0)

                        val selectHomeDialogAdapter = SelectHomeDialogAdapter(homes, { selectedHome ->
                            homeId = selectedHome.id
                            tvHome!!.text = selectedHome.name
                            homeSelectedListener?.onHomeSelected(selectedHome.id, selectedHome.name)

                            val selectedDate = viewModel.selectedDate.value ?: LocalDate.now()
                            val newFragment = CalendarFragment.newInstance(homeId, selectedDate.year, selectedDate.monthValue - 1)
                            childFragmentManager.beginTransaction()
                                .replace(R.id.calendarContainer, newFragment)
                                .commit()

                            Handler(Looper.getMainLooper()).postDelayed({
                                homeDialog?.dismiss()
                            }, 300)
                        }, selectedIndex)

                        homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                        homeRecyclerView.adapter = selectHomeDialogAdapter

                        if(homes.isNotEmpty()) {
                            val selectedHome = homes.getOrNull(selectedIndex)
                            if (selectedHome != null) {
                                tvHome?.text = selectedHome.name
                                homeId = selectedHome.id
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "getAllHome: ${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            requestFeature(Window.FEATURE_NO_TITLE)
        }
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    private fun showCustomPopupWindow(anchor: View) {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.popup_menu_layout, null)

        val popupWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 180f, anchor.resources.displayMetrics
        ).toInt()

        val screenWidth = resources.displayMetrics.widthPixels
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val anchorX = anchorLocation[0]

        // anchor 기준 팝업이 화면을 넘지 않도록 왼쪽으로 offset 계산
        val offsetX = if (anchorX + popupWidth > screenWidth) {
            screenWidth - (anchorX + popupWidth) - 20 // -20은 추가 margin
        } else {
            -20 // 기본 왼쪽 offset
        }

        val popupWindow = PopupWindow(popupView, popupWidth, WindowManager.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 10f
        popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupWindow.isOutsideTouchable = true

        popupWindow.showAsDropDown(anchor, offsetX, 0)

        popupView.findViewById<TextView>(R.id.menuDevice).setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            popupWindow.dismiss()
        }

        popupView.findViewById<TextView>(R.id.menuSetting).setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SettingsFragment())
            popupWindow.dismiss()
        }
    }

    interface OnHomeSelectedListener {
        fun onHomeSelected(homeId: String, homeName: String)
    }

    fun setOnHomeSelectedListener(listener: OnHomeSelectedListener) {
        this.homeSelectedListener = listener
    }
}