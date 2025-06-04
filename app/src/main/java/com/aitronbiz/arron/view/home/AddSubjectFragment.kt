package com.aitronbiz.arron.view.home

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
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
    private var dialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSubjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager.getInstance(requireContext())
        setStatusBar(requireActivity(), binding.mainLayout)

        setupCancelDialog()
        setupUI()
    }

    private fun setupCancelDialog() {
        dialog = Dialog(requireActivity()).apply {
            setContentView(R.layout.dialog_subject_cancel)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }

        dialog?.findViewById<ConstraintLayout>(R.id.btnCancel)?.setOnClickListener {
            dialog?.dismiss()
        }

        dialog?.findViewById<ConstraintLayout>(R.id.btnConfirm)?.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
            dialog?.dismiss()
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            dialog?.show()
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
                name.isEmpty() -> showToast("이름을 입력하세요")
                birthdate.isEmpty() -> showToast("생년월일을 입력하세요")
                contact.isEmpty() -> showToast("전화번호를 입력하세요")
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
                            if (success) {
                                showToast("등록되었습니다")
                                replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
                            } else {
                                showToast("등록 실패")
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

    private fun showToast(msg: String) {
        Toast.makeText(requireActivity(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}