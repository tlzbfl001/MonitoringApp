package com.aitronbiz.arron.view.setting

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentUserInfoBinding
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserInfoFragment : Fragment() {
    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataManager: DataManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        setStatusBar(requireActivity(), binding.mainLayout)

        dataManager = DataManager.getInstance(requireActivity())

        val getUser = dataManager.getUser(AppController.prefs.getUID())
        lifecycleScope.launch(Dispatchers.IO) {
            val getSession = RetrofitClient.authApiService.getSession("Bearer ${getUser.sessionToken}")
            if (getSession.isSuccessful) {
                Log.d(TAG, "user: ${getSession.body()!!.user}")
                binding.tvName.text = getSession.body()!!.user.name
                binding.tvEmail.text = getSession.body()!!.user.email
            } else {
                Log.e(TAG, "getSession 실패: ${getSession.code()}")
            }
        }

        binding.btnBack.setOnClickListener {
            replaceFragment1(parentFragmentManager, SettingsFragment())
        }

        return binding.root
    }
}