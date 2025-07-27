package com.aitronbiz.arron.view.home

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.HomeAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.databinding.FragmentHomeBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.location
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.device.DeviceFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HomeAdapter
    private var homeList: MutableList<Home> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        adapter = HomeAdapter(
            homeList,
            onItemClick = { home ->
                val bundle = Bundle().apply {
                    putString("homeId", home.id)
                }
                replaceFragment2(parentFragmentManager, SettingHomeFragment(), bundle)
            },
            onEditClick = { home ->
                val bundle = Bundle().apply {
                    putString("homeId", home.id)
                }
                replaceFragment2(parentFragmentManager, EditHomeFragment(), bundle)
            },
            onDeleteClick = { home ->
                val dialog = AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
                    .setTitle("홈 삭제")
                    .setMessage("정말 삭제 하시겠습니까?")
                    .setPositiveButton("확인", null)
                    .setNegativeButton("취소", null)
                    .create()

                dialog.setOnShowListener {
                    val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val response = RetrofitClient.apiService.deleteHome("Bearer ${AppController.prefs.getToken()}", home.id)
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Log.d(TAG, "deleteHome: ${response.body()}")
                                    Toast.makeText(requireContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    homeList.removeIf { it.id == home.id }
                                    adapter.notifyDataSetChanged()
                                    dialog.dismiss()
                                } else {
                                    Log.e(TAG, "deleteHome 실패: ${response.code()}")
                                    Toast.makeText(requireContext(), "삭제 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                dialog.show()
            }
        )

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch(Dispatchers.IO) {
            val response = RetrofitClient.apiService.getAllHome("Bearer ${AppController.prefs.getToken()}")
            if (response.isSuccessful) {
                val result = response.body()?.homes ?: emptyList()
                withContext(Dispatchers.Main) {
                    homeList.clear()
                    homeList.addAll(result)
                    adapter.notifyDataSetChanged()
                }
            } else {
                Log.e(TAG, "getAllHome 실패: ${response.code()}")
            }
        }

        binding.btnBack.setOnClickListener {
            if(location == 1) {
                replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
            }else {
                replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            }
        }

        binding.btnAdd.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddHomeFragment())
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}