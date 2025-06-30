package com.aitronbiz.arron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aitronbiz.arron.AppController
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentAddSubjectBinding
import com.aitronbiz.arron.entity.EnumData
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class AddSubjectFragment : Fragment() {
    private var _binding: FragmentAddSubjectBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var status = EnumData.NORMAL.name

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSubjectBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireContext())

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, SubjectFragment())
        }

        binding.btnNormal.setOnClickListener {
            status = EnumData.NORMAL.name
            updateStatusButtonUI(binding.btnNormal)
        }

        binding.btnCaution.setOnClickListener {
            status = EnumData.CAUTION.name
            updateStatusButtonUI(binding.btnCaution)
        }

        binding.btnWarning.setOnClickListener {
            status = EnumData.WARNING.name
            updateStatusButtonUI(binding.btnWarning)
        }

        binding.btnAdd.setOnClickListener {
            val name = binding.etName.text.trim().toString()
            val birthdate = binding.etBirthdate.text.trim().toString()
            val contact = binding.etContact.text.trim().toString()
            val bloodType = binding.etBloodType.text.toString()
            val address = binding.etAddress.text.toString()

            when {
                name.isEmpty() -> Toast.makeText(requireActivity(), "이름을 입력하세요", Toast.LENGTH_SHORT).show()
                birthdate.isEmpty() -> Toast.makeText(requireActivity(), "생년월일을 입력하세요", Toast.LENGTH_SHORT).show()
                contact.isEmpty() -> Toast.makeText(requireActivity(), "전화번호를 입력하세요", Toast.LENGTH_SHORT).show()
                else -> {
                    val subject = Subject(
                        uid = AppController.prefs.getUID(),
                        image = "",
                        name = name,
                        birthdate = birthdate,
                        bloodType = bloodType,
                        address = address,
                        contact = contact,
                        status = status,
                        createdAt = LocalDateTime.now().toString()
                    )

                    lifecycleScope.launch(Dispatchers.IO) {
                        val success = dataManager.insertSubject(subject)
                        withContext(Dispatchers.Main) {
                            if(success) {
                                Toast.makeText(requireActivity(), "등록되었습니다", Toast.LENGTH_SHORT).show()
                                replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
                            }else {
                                Toast.makeText(requireActivity(), "등록 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateStatusButtonUI(selected: View) {
        val buttons = listOf(binding.btnNormal, binding.btnCaution, binding.btnWarning)
        for (btn in buttons) {
            val drawableId = if (btn == selected) R.drawable.rec_30_black else R.drawable.rec_30_grey
            btn.background = ContextCompat.getDrawable(requireContext(), drawableId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}