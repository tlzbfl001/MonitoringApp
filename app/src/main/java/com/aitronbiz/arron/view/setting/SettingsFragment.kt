package com.aitronbiz.arron.view.setting

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.BuildConfig
import com.aitronbiz.arron.MainViewModel
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DBHelper.Companion.USER
import com.aitronbiz.arron.util.TodayDecorator
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSettingsBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.User
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.OnStartDragListener
import com.aitronbiz.arron.util.PermissionUtil.bluetoothPermissions
import com.aitronbiz.arron.view.init.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView

class SettingsFragment : Fragment(), OnStartDragListener {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var calendarDialog: BottomSheetDialog? = null
    private lateinit var user: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(requireActivity())

        // 유저 정보 가져오기
        AppController.prefs.getUID().let { uid ->
            user = dataManager.getUser(uid)
        } ?: run {
            Toast.makeText(requireActivity(), "유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            logoutProcess()
        }

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        setupCalendarDialog()

        binding.tvNotification.text = if(user.notificationStatus == "") "-" else user.notificationStatus

        binding.btnConnection.setOnClickListener {
            if(bluetoothPermissions.any {
                    ContextCompat.checkSelfPermission(requireActivity(), it) != PackageManager.PERMISSION_GRANTED
                }) {
                ActivityCompat.requestPermissions(requireActivity(), bluetoothPermissions, 100)
            }else {
                replaceFragment1(requireActivity().supportFragmentManager, ConnectFragment())
            }
        }

        binding.btnNotification.setOnClickListener {
            calendarDialog?.show()
        }

        binding.btnLogout.setOnClickListener {
            logoutProcess()
            /*if (!networkStatus(requireActivity())) {
                Toast.makeText(ctx, "네트워크에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            } else {
                showLogoutDialog()
            }*/
        }
    }

    // 날짜 선택 다이얼로그
    private fun setupCalendarDialog() {
        calendarDialog = BottomSheetDialog(requireActivity())
        val view = layoutInflater.inflate(R.layout.dialog_calendar, null)
        val calendarView = view.findViewById<MaterialCalendarView>(R.id.calendarView)

        var selectedDate = CalendarDay.today()
        val decorator = TodayDecorator(requireActivity(), selectedDate)
        calendarView.addDecorator(decorator)

        calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = date
            decorator.updateDate(date)
            calendarView.invalidateDecorators()

            Handler(Looper.getMainLooper()).postDelayed({
                dataManager.updateData(USER, "notificationStatus", date.date.toString(), AppController.prefs.getUID())
                binding.tvNotification.text = "${date.date}"
                calendarDialog?.dismiss()
            }, 300)
        }

        val topBar = calendarView.getChildAt(0) as ViewGroup
        val titleTextView = topBar.getChildAt(1) as TextView
        titleTextView.textSize = 17f
        titleTextView.setTextColor(Color.GRAY)

        calendarDialog?.setContentView(view)
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireActivity(), R.style.AlertDialogStyle)
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
                            else Toast.makeText(requireActivity(), "로그아웃 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    EnumData.KAKAO.name -> {}
                    EnumData.NAVER.name -> {}

                    else -> logoutProcess()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun logoutProcess() {
        viewModel.stopTokenAutoRefresh()
        AppController.prefs.removeToken()
        AppController.prefs.removeUID()

        Toast.makeText(requireActivity(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}