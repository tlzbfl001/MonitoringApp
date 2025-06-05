package com.aitronbiz.arron.view.setting

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.BuildConfig
import com.aitronbiz.arron.MainViewModel
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.MenuAdapter
import com.aitronbiz.arron.adapter.OnStartDragListener
import com.aitronbiz.arron.util.TodayDecorator
import com.aitronbiz.arron.util.CustomUtil.networkStatus
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSettingsBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.MenuItem
import com.aitronbiz.arron.entity.User
import com.aitronbiz.arron.view.init.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView

class SettingsFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // MainActivity에서 공유 중인 ViewModel
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var dataManager: DataManager
    private var transmissionDialog: BottomSheetDialog? = null
    private lateinit var user: User

    // 메뉴 항목 (초기값)
    private val menuItems = mutableListOf(
        MenuItem("재실", true),
        MenuItem("활동도", true),
        MenuItem("일간 활동량", false),
        MenuItem("연속 거주 시간", true),
        MenuItem("스마트 절전", false)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        val ctx = context ?: return binding.root

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(ctx)

        // 유저 정보 가져오기
        AppController.prefs.getUID().let { uid ->
            user = dataManager.getUser(uid)
        } ?: run {
            Toast.makeText(ctx, "유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            logoutProcess()
        }

        // Dialog 및 버튼 리스너 초기화
        initTransmissionDialog()

        binding.btnSettingMonitoringAlarm.setOnClickListener {
            showCalendarBottomSheet()
        }

        binding.btnTransmission.setOnClickListener {
            transmissionDialog?.show()
        }

        binding.btnAppInfo.setOnClickListener {

        }

        binding.btnEditMenu.setOnClickListener {
            showMenuEditSheet()
        }

        binding.btnLogout.setOnClickListener {
            if (!networkStatus(requireActivity())) {
                Toast.makeText(ctx, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            } else {
                showLogoutDialog()
            }
        }

        return binding.root
    }

    private fun initTransmissionDialog() {
        val ctx = context ?: return
        transmissionDialog = BottomSheetDialog(ctx)
        val view = layoutInflater.inflate(R.layout.dialog_select_transmission, null)
        transmissionDialog?.setContentView(view)

        val btnOption1 = view.findViewById<CardView>(R.id.buttonOption1)
        val btnOption2 = view.findViewById<CardView>(R.id.buttonOption2)
        val btnOption3 = view.findViewById<CardView>(R.id.buttonOption3)

        btnOption1.setOnClickListener {
            binding.tvTransmissionDesc.text = "10분"
            transmissionDialog?.dismiss()
        }
        btnOption2.setOnClickListener {
            binding.tvTransmissionDesc.text = "1시간"
            transmissionDialog?.dismiss()
        }
        btnOption3.setOnClickListener {
            binding.tvTransmissionDesc.text = "10시간"
            transmissionDialog?.dismiss()
        }
    }

    private fun showLogoutDialog() {
        val ctx = context ?: return

        AlertDialog.Builder(ctx, R.style.AlertDialogStyle)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                when (user.type) {
                    EnumData.GOOGLE.name -> {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .requestEmail()
                            .build()

                        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

                        googleSignInClient.signOut().addOnCompleteListener { task ->
                            if (task.isSuccessful) logoutProcess()
                            else Toast.makeText(ctx, "로그아웃 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // TODO: 향후 카카오/네이버 연동 시 추가 구현
                    EnumData.KAKAO.name -> {}
                    EnumData.NAVER.name -> {}

                    else -> logoutProcess()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun logoutProcess() {
        val ctx = context ?: return

        viewModel.stopTokenAutoRefresh()
        AppController.prefs.removeToken()

        Toast.makeText(ctx, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // 메뉴 편집 BottomSheetDialog 구성
    private fun showMenuEditSheet() {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        val view = layoutInflater.inflate(R.layout.dialog_menu_edit, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.menuRecyclerView)
        val btnConfirm = view.findViewById<CardView>(R.id.btnConfirm)

        val adapter = MenuAdapter(menuItems, { index, isVisible ->
            menuItems[index].visible = isVisible
        }, this)

        recyclerView.layoutManager = LinearLayoutManager(ctx)
        recyclerView.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(from.adapterPosition, to.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled() = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        btnConfirm.setOnClickListener {
            val reorderedItems = adapter.getMenuItems()
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    // 날짜 선택 다이얼로그
    private fun showCalendarBottomSheet() {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        val view = layoutInflater.inflate(R.layout.dialog_calendar, null)
        val calendarView = view.findViewById<MaterialCalendarView>(R.id.calendarView)

        var selectedDate = CalendarDay.today()
        val decorator = TodayDecorator(ctx, selectedDate)
        calendarView.addDecorator(decorator)

        calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = date
            decorator.updateDate(date)
            calendarView.invalidateDecorators()

            Handler(Looper.getMainLooper()).postDelayed({
                binding.tvMonitoring.text = "${date.date}"
                dialog.dismiss()
            }, 300)
        }

        val topBar = calendarView.getChildAt(0) as ViewGroup
        val titleTextView = topBar.getChildAt(1) as TextView
        titleTextView.textSize = 17f
        titleTextView.setTextColor(Color.GRAY)

        dialog.setContentView(view)
        dialog.show()
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}