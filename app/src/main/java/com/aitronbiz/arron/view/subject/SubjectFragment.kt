package com.aitronbiz.arron.view.subject

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.adapter.SelectHomeDialogAdapter
import com.aitronbiz.arron.adapter.SubjectAdapter
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.database.DBHelper.Companion.SUBJECT
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentSubjectBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.AddHomeFragment
import com.aitronbiz.arron.view.home.MainFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubjectFragment : Fragment() {
    private var _binding: FragmentSubjectBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var adapter: SubjectAdapter
    private var homeDialog: BottomSheetDialog? = null
    private var homeData: Home? = null
    private var homeId = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubjectBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        // 다이얼로그 초기 설정
        homeDialog = BottomSheetDialog(requireContext())
        val homeDialogView = layoutInflater.inflate(R.layout.dialog_select_room, null)
        val homeRecyclerView = homeDialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val tvTitle = homeDialogView.findViewById<TextView>(R.id.tvTitle)
        val tvBtnName = homeDialogView.findViewById<TextView>(R.id.tvBtnName)
        val btnAddHome = homeDialogView.findViewById<ConstraintLayout>(R.id.btnAdd)

        tvTitle.text = "홈 선택"
        tvBtnName.text = "홈 추가"

        // 홈 데이터 불러오기
        val homes = dataManager.getHomes(AppController.prefs.getUID())
        if (homes.isNotEmpty()) {
            homeData = homes[0]
            homeId = homes[0].id
        }
        homeDialog!!.setContentView(homeDialogView)

        val subjectList = if (homeData != null) {
            dataManager.getSubjects(AppController.prefs.getUID(), homeId)
        } else {
            mutableListOf()
        }

        adapter = SubjectAdapter(
            subjectList,
            onItemClick = { subject ->
                val bundle = Bundle().apply {
                    putParcelable("homeData", homeData)
                    putParcelable("subjectData", subject)
                }
                replaceFragment2(parentFragmentManager, DetailSubjectFragment(), bundle)
            },
            onEditClick = { subject ->
                val bundle = Bundle().apply {
                    putParcelable("subjectData", subject)
                }
                replaceFragment2(parentFragmentManager, EditSubjectFragment(), bundle)
            },
            onDeleteClick = { subject ->
                lifecycleScope.launch(Dispatchers.IO) {
                    if(subject.serverId != null && subject.serverId != "") {
                        val response = RetrofitClient.apiService.deleteSubject("Bearer ${AppController.prefs.getToken()}", subject.serverId!!)
                        if(response.isSuccessful) {
                            Log.d(TAG, "deleteSubject: ${response.body()}")
                        } else {
                            Log.e(TAG, "deleteSubject: $response")
                        }
                    }

                    dataManager.deleteData(SUBJECT, subject.id)
                    subjectList.removeIf { it.id == subject.id }
                    withContext(Dispatchers.Main) {
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        )

        // RecyclerView 연결
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        if (homeId > 0) {
            binding.tvHome.text = "홈 : ${homes[0].name}"
        } else {
            binding.tvHome.text = "홈 :   "
        }

        val selectHomeDialogAdapter = SelectHomeDialogAdapter(homes) { selectedItem ->
            homeId = selectedItem.id
            binding.tvHome.text = "홈 : ${selectedItem.name}"
            Handler(Looper.getMainLooper()).postDelayed({
                val data = dataManager.getSubjects(AppController.prefs.getUID(), homeId)
                adapter.updateData(data)
                homeDialog?.dismiss()
            }, 300)
        }
        homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeRecyclerView.adapter = selectHomeDialogAdapter

        btnAddHome.setOnClickListener {
            homeDialog?.dismiss()
            replaceFragment1(parentFragmentManager, AddHomeFragment())
        }

        binding.btnBack.setOnClickListener {
            replaceFragment1(parentFragmentManager, MainFragment())
        }

        binding.btnAdd.setOnClickListener {
            if (homeId > 0) {
                val args = Bundle().apply {
                    putParcelable("homeData", homeData)
                }
                replaceFragment2(parentFragmentManager, AddSubjectFragment(), args)
            } else {
                Toast.makeText(requireActivity(), "등록된 홈이 없습니다", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnHome.setOnClickListener {
            homeDialog!!.show()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
