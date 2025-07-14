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
import com.aitronbiz.arron.api.dto.SubjectDTO
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentEditSubjectBinding
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class EditSubjectFragment : Fragment() {
    private var _binding: FragmentEditSubjectBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var subjectData: Subject? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditSubjectBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        subjectData = arguments?.getParcelable("subjectData")

        if(subjectData!!.name != null) binding.etName.setText(subjectData!!.name)

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SubjectFragment())
        }

        binding.btnEdit.setOnClickListener {
            val data = Subject(
                id = subjectData!!.id,
                uid = AppController.prefs.getUID(),
                name = binding.etName.text.trim().toString(),
                createdAt = LocalDateTime.now().toString(),
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val updatedRows = dataManager.updateSubject(data)
                withContext(Dispatchers.Main) {
                    if(updatedRows > 0 && subjectData?.serverId != null && subjectData?.serverId != "") {
                        val dto = SubjectDTO(
                            name = binding.etName.text.trim().toString()
                        )
                        val response = RetrofitClient.apiService.updateSubject("Bearer ${AppController.prefs.getToken()}", subjectData!!.serverId!!, dto)
                        if(response.isSuccessful) {
                            Log.d(TAG, "updateSubject: ${response.body()}")
                        } else {
                            Log.e(TAG, "updateSubject: $response")
                        }

                        Toast.makeText(requireActivity(), "수정되었습니다", Toast.LENGTH_SHORT).show()
                        replaceFragment1(requireActivity().supportFragmentManager, SubjectFragment())
                    }else {
                        Toast.makeText(requireActivity(), "수정 실패하였습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return binding.root
    }
}