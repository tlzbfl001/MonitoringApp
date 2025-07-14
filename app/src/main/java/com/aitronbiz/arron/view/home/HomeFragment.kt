package com.aitronbiz.arron.view.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.adapter.HomeAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.database.DBHelper.Companion.HOME
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentHomeBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: HomeAdapter
    private lateinit var homeList: MutableList<Home>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        homeList = dataManager.getHomes(AppController.prefs.getUID()).toMutableList()

        adapter = HomeAdapter(
            homeList,
            onEditClick = { home ->
                val bundle = Bundle().apply {
                    putParcelable("home", home)
                }
                replaceFragment2(parentFragmentManager, EditHomeFragment(), bundle)
            },
            onDeleteClick = { home ->
                lifecycleScope.launch(Dispatchers.IO) {
                    if(home.serverId != null && home.serverId != "") {
                        val response = RetrofitClient.apiService.deleteHome("Bearer ${AppController.prefs.getToken()}", home.serverId!!)
                        if(response.isSuccessful) {
                            Log.d(TAG, "deleteHome: ${response.body()}")
                        } else {
                            Log.e(TAG, "deleteHome: $response")
                        }
                    }

                    dataManager.deleteData(HOME, home.id)
                    homeList.removeIf { it.id == home.id }
                    withContext(Dispatchers.Main) {
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        )

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.btnAdd.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, AddHomeFragment())
        }

        return binding.root
    }
}