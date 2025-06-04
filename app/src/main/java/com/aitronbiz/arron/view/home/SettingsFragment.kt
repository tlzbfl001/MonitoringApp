package com.aitronbiz.arron.view.home

import android.Manifest
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
import com.aitronbiz.arron.util.CustomUtil.networkStatus
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.BuildConfig
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.MenuAdapter
import com.aitronbiz.arron.adapter.OnStartDragListener
import com.aitronbiz.arron.adapter.TodayDecorator
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSettingsBinding
import com.aitronbiz.arron.entity.MenuItem
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.init.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var dataManager: DataManager
    private var transmissionDialog: BottomSheetDialog? = null

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

        setStatusBar(requireActivity(), binding.mainLayout)

        // 싱글톤 인스턴스를 사용하여 DataManager 객체 초기화
        dataManager = DataManager.getInstance(requireContext())

        // BottomSheetDialog 초기화
        transmissionDialog = BottomSheetDialog(requireContext())
        val transmissionDialogView = layoutInflater.inflate(R.layout.dialog_select_transmission, null)
        transmissionDialog!!.setContentView(transmissionDialogView)

        binding.btnSettingMonitoringAlarm.setOnClickListener {
            showCalendarBottomSheet()
        }

        binding.btnTransmission.setOnClickListener {
            val btnOption1 = transmissionDialogView.findViewById<CardView>(R.id.buttonOption1)
            val btnOption2 = transmissionDialogView.findViewById<CardView>(R.id.buttonOption2)
            val btnOption3 = transmissionDialogView.findViewById<CardView>(R.id.buttonOption3)

            btnOption1.setOnClickListener {
                binding.tvTransmissionDesc.text = "10분"
                transmissionDialog!!.dismiss()
            }

            btnOption2.setOnClickListener {
                binding.tvTransmissionDesc.text = "1시간"
                transmissionDialog!!.dismiss()
            }

            btnOption3.setOnClickListener {
                binding.tvTransmissionDesc.text = "10시간"
                transmissionDialog!!.dismiss()
            }

            transmissionDialog!!.show()
        }

        binding.btnAppInfo.setOnClickListener {
            // TODO: 앱 정보 버튼 클릭 시 처리
        }

        binding.btnEditMenu.setOnClickListener {
            showMenuEditSheet()
        }

        binding.btnLogout.setOnClickListener {
            if (!networkStatus(requireActivity())) {
                Toast.makeText(context, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            } else {
                showLogoutDialog()
            }
        }

        return binding.root
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    private fun showMenuEditSheet() {
        val dialog = BottomSheetDialog(requireActivity())

        val view = layoutInflater.inflate(R.layout.dialog_menu_edit, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.menuRecyclerView)
        val btnConfirm = view.findViewById<CardView>(R.id.btnConfirm)

        val adapter = MenuAdapter(menuItems, { index, isVisible ->
            // 메뉴 항목의 visible 상태 변경
            menuItems[index].visible = isVisible
        }, this)

        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.adapter = adapter

        // 드래그를 이용한 아이템 순서 변경을 위한 ItemTouchHelper 콜백 정의
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // 상하 드래그만 허용
            0 // 스와이프 비활성화
        ) {
            override fun onMove(
                rv: RecyclerView,
                from: RecyclerView.ViewHolder,
                to: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = from.adapterPosition
                val toPos = to.adapterPosition
                adapter.moveItem(fromPos, toPos) // 어댑터 내부 아이템 순서 변경
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled() = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // 확인 버튼 클릭 시 동작 정의
        btnConfirm.setOnClickListener {
            val reorderedItems = adapter.getMenuItems() // 변경된 순서 반영
            dialog.dismiss()
        }

        // BottomSheet에 뷰 설정 후 표시
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showCalendarBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_calendar, null)
        val calendarView = view.findViewById<MaterialCalendarView>(R.id.calendarView)

        var selectedDate = CalendarDay.today()
        val decorator = TodayDecorator(requireContext(), selectedDate)
        calendarView.addDecorator(decorator)

        calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = date
            decorator.updateDate(date)
            calendarView.invalidateDecorators() // 갱신

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

    private fun showLogoutDialog() {
        val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .requestEmail()
                    .build()

                val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

                googleSignInClient.signOut().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                        AppController.prefs.removeUID()
                        val intent = Intent(requireActivity(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .create()
        dialog.show()
    }
}