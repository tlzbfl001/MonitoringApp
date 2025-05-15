package kr.aitron.aitron.view

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kr.aitron.aitron.MainViewModel
import kr.aitron.aitron.R
import kr.aitron.aitron.databinding.FragmentAddSubjectBinding
import kr.aitron.aitron.database.entity.Subject
import kr.aitron.aitron.util.CustomUtil.replaceFragment1
import java.time.LocalDateTime

class AddSubjectFragment : Fragment() {
    private var _binding: FragmentAddSubjectBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private var dialog : Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSubjectBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        dialog = Dialog(requireActivity())
        dialog!!.setContentView(R.layout.dialog_subject_cancel)
        dialog!!.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val btnCancel = dialog!!.findViewById<ConstraintLayout>(R.id.btnCancel)
        val btnConfirm = dialog!!.findViewById<ConstraintLayout>(R.id.btnConfirm)

        binding.btnBack.setOnClickListener {
            dialog!!.show()

            btnCancel.setOnClickListener {
                dialog!!.dismiss()
            }

            btnConfirm.setOnClickListener {
                replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
                dialog!!.dismiss()
            }
        }

        binding.btnAdd.setOnClickListener {
            val bloodType = if(binding.etBloodType.text.toString() == "") "" else binding.etBloodType.text.toString()
            val address = if(binding.etAddress.text.toString() == "") "" else binding.etAddress.text.toString()

            if(binding.etName.text.trim() == "") {
                Toast.makeText(requireActivity(), "이름을 입력하세요", Toast.LENGTH_SHORT).show()
            }else if(binding.etBirthdate.text.trim() == "") {
                Toast.makeText(requireActivity(), "생년월일을 입력하세요", Toast.LENGTH_SHORT).show()
            }else if(binding.etContact.text.trim() == "") {
                Toast.makeText(requireActivity(), "전화번호를 입력하세요", Toast.LENGTH_SHORT).show()
            }else {
                val subject = Subject(
                    uid = 1,
                    image = "",
                    name = binding.etName.text.trim().toString(),
                    birthdate = binding.etBirthdate.text.trim().toString(),
                    bloodType = bloodType,
                    address = address,
                    contact = binding.etContact.text.trim().toString(),
                    createdAt = LocalDateTime.now().toString(),
                    updatedAt = LocalDateTime.now().toString()
                )

                viewModel.insertSubject(subject)
            }
        }

        viewModel.subjectInsertedLiveData.observe(viewLifecycleOwner, Observer { isInserted ->
            if(isInserted) {
                Toast.makeText(requireActivity(), "등록되었습니다", Toast.LENGTH_SHORT).show()
                viewModel.resetInsertState() // 데이터 삽입 후 상태를 리셋
                replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
            }
        })

        return binding.root
    }
}