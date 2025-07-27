package com.aitronbiz.arron.view.home

import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.aitronbiz.arron.R
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.adapter.SelectHomeDialogAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.DeviceFragment
import com.aitronbiz.arron.view.notification.NotificationFragment
import com.aitronbiz.arron.view.setting.SettingsFragment
import com.aitronbiz.arron.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
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
        // 외부에서 인스턴스 생성 시 homeId 전달
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

        // 다이얼로그 전체화면 및 상태바, 네비게이션 바 투명 설정
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

        // 상태바 스타일 적용
        setStatusBar(requireActivity(), view.findViewById(R.id.mainLayout))

        setupHomeDialog() // 홈 다이얼로그 초기화

        view.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.dialogRoot).setOnClickListener {
            dismiss()
        }

        tvHome!!.setOnClickListener {
            homeDialog!!.show()
        }

        view.findViewById<ConstraintLayout>(R.id.btnAlarm).setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, NotificationFragment())
            dismiss()
        }

        view.findViewById<ConstraintLayout>(R.id.btnSetting).setOnClickListener { view ->
            showPopupMenu(view)
        }

        // CalendarFragment 삽입
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

        // 홈 목록 조회
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getAllHome("Bearer ${AppController.prefs.getToken()}")

                if (response.isSuccessful) {
                    val homes = response.body()!!.homes

                    withContext(Dispatchers.Main) {
                        val selectedIndex = homes.indexOfFirst { it.id == homeId }.coerceAtLeast(0)

                        // 홈 목록 어댑터 설정
                        val selectHomeDialogAdapter = SelectHomeDialogAdapter(homes, { selectedHome ->
                            homeId = selectedHome.id
                            tvHome!!.text = selectedHome.name

                            // 홈 선택 리스너 콜백 호출
                            homeSelectedListener?.onHomeSelected(selectedHome.id, selectedHome.name)

                            // 선택된 홈 기준으로 달력 갱신
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
                    Log.e(TAG, "getAllHome 실패: ${response.code()}")
                }
            }catch (e: Exception) {
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

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireActivity(), view)
        popupMenu.menuInflater.inflate(R.menu.main_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.device -> {
                    replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
                    dismiss()
                    true
                }
                R.id.setting -> {
                    replaceFragment1(requireActivity().supportFragmentManager, SettingsFragment())
                    dismiss()
                    true
                }
                else -> false
            }
        }
    }

    // 홈 선택 리스너 인터페이스
    interface OnHomeSelectedListener {
        fun onHomeSelected(homeId: String, homeName: String)
    }

    fun setOnHomeSelectedListener(listener: OnHomeSelectedListener) {
        this.homeSelectedListener = listener
    }
}