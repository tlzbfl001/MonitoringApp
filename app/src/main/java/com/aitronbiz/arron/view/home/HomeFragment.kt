package com.aitronbiz.arron.view.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.AppController
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
            onAddClick = {
                replaceFragment1(requireActivity().supportFragmentManager, AddHomeFragment())
            }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

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
                Log.e(TAG, "getAllHome: ${response.code()}")
            }
        }

        binding.btnBack.setOnClickListener {
            if(location == 1) {
                replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
            }else {
                replaceFragment1(requireActivity().supportFragmentManager, DeviceFragment())
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? BottomNavVisibilityController)?.hideBottomNav()
    }
}