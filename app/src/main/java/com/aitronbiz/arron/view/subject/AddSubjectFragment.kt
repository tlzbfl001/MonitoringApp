package com.aitronbiz.arron.view.subject

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.dto.HomeDTO
import com.aitronbiz.arron.api.dto.SubjectDTO
import com.aitronbiz.arron.database.DBHelper.Companion.HOME
import com.aitronbiz.arron.database.DBHelper.Companion.SUBJECT
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentAddSubjectBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.home.HomeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AddSubjectFragment : Fragment() {
    private var _binding: FragmentAddSubjectBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var homeData: Home? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddSubjectBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        arguments?.let {
            homeData = it.getParcelable("homeData")
        }

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SubjectFragment())
        }

        binding.btnAdd.setOnClickListener {
            val name = binding.etName.text.trim().toString()

            when {
                name.isEmpty() -> Toast.makeText(requireActivity(), "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                homeData == null -> Toast.makeText(requireActivity(), "등록된 홈이 없습니다. 홈 등록 후 등록해주세요.", Toast.LENGTH_SHORT).show()
                else -> {
                    val data = Subject(
                        uid = AppController.prefs.getUID(),
                        homeId = homeData!!.id,
                        name = name,
                        createdAt = LocalDateTime.now().toString()
                    )

                    lifecycleScope.launch(Dispatchers.IO) {
                        val insertedId = dataManager.insertSubject(data)
                        withContext(Dispatchers.Main) {
                            if(insertedId != -1) {
                                val subjectDTO = SubjectDTO(name = data.name!!)
                                val response = RetrofitClient.apiService.createSubject("Bearer ${AppController.prefs.getToken()}", subjectDTO)
                                if(response.isSuccessful) {
                                    Log.d(TAG, "createSubject: ${response.body()}")
                                    dataManager.updateData(SUBJECT, "serverId", response.body()!!.subject.id, insertedId)
                                } else {
                                    Log.e(TAG, "createSubject: $response")
                                }

                                Toast.makeText(requireActivity(), "저장되었습니다", Toast.LENGTH_SHORT).show()
                                replaceFragment1(requireActivity().supportFragmentManager, SubjectFragment())
                            }else {
                                Toast.makeText(requireActivity(), "저장 실패하였습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}