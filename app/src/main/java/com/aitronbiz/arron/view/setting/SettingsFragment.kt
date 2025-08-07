package com.aitronbiz.arron.view.setting

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.BuildConfig
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSettingsBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.User
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.OnStartDragListener
import com.aitronbiz.arron.util.PermissionUtil.bluetoothPermissions
import com.aitronbiz.arron.view.init.LoginActivity
import com.aitronbiz.arron.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog

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
        binding.btnConnection.setOnClickListener {
            if(bluetoothPermissions.any {
                    ContextCompat.checkSelfPermission(requireActivity(), it) != PackageManager.PERMISSION_GRANTED
                }) {
                ActivityCompat.requestPermissions(requireActivity(), bluetoothPermissions, 100)
            }else {
                replaceFragment1(requireActivity().supportFragmentManager, ConnectFragment())
            }
        }

        binding.btnUser.setOnClickListener {
            replaceFragment1(parentFragmentManager, UserInfoFragment())
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