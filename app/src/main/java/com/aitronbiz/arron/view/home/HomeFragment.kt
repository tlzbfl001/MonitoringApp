package com.aitronbiz.arron.view.home

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.HomeAdapter
import com.aitronbiz.arron.adapter.SelectHomeDialogAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.ErrorResponse
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.database.DBHelper.Companion.HOME
import com.aitronbiz.arron.databinding.FragmentHomeBinding
import com.aitronbiz.arron.util.BottomNavVisibilityController
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var deleteDialog : Dialog? = null
    private lateinit var adapter: HomeAdapter
    private var homeList: MutableList<Home> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)

        deleteDialog = Dialog(requireActivity())
        deleteDialog!!.setContentView(R.layout.dialog_delete)
        deleteDialog!!.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnCancel = deleteDialog!!.findViewById<CardView>(R.id.btnCancel)
        val btnDelete = deleteDialog!!.findViewById<CardView>(R.id.btnDelete)

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
                btnCancel.setOnClickListener {
                    deleteDialog!!.dismiss()
                }

                btnDelete.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            val response = RetrofitClient.apiService.deleteHome("Bearer ${AppController.prefs.getToken()}", home.id)
                            if(response.isSuccessful) {
                                Log.d(TAG, "deleteHome: ${response.body()}")
                                Toast.makeText(context, "삭제되었습니다", Toast.LENGTH_SHORT).show()
                                homeList.removeIf { it.id == home.id }
                                adapter.notifyDataSetChanged()
                            } else {
                                val errorBody = response.errorBody()?.string()
                                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                Log.e(TAG, "deleteHome: $errorResponse")
                            }
                        }

                        deleteDialog!!.dismiss()
                    }
                }

                deleteDialog!!.show()
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
                Log.e(TAG, "getAllHome: $response")
            }
        }

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
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