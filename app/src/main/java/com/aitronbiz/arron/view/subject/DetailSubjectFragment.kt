package com.aitronbiz.arron.view.subject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentDetailSubjectBinding
import com.aitronbiz.arron.entity.Home
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2
import com.aitronbiz.arron.util.CustomUtil.setStatusBar
import com.aitronbiz.arron.view.room.RoomFragment

class DetailSubjectFragment : Fragment() {
    private var _binding: FragmentDetailSubjectBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailSubjectBinding.inflate(inflater, container, false)

        setStatusBar(requireActivity(), binding.mainLayout)
        dataManager = DataManager.getInstance(requireActivity())

        val homeData = arguments?.getParcelable<Home>("homeData")
        val subjectData = arguments?.getParcelable<Subject>("subjectData")

        binding.btnBack.setOnClickListener {
            replaceFragment1(parentFragmentManager, SubjectFragment())
        }

        binding.btnAddRoom.setOnClickListener {
            if(homeData != null && subjectData != null) {
                val bundle = Bundle().apply {
                    putParcelable("homeData", homeData)
                    putParcelable("subjectData", subjectData)
                }
                replaceFragment2(parentFragmentManager, RoomFragment(), bundle)
            }else {
                Toast.makeText(requireActivity(), "오류 발생", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }
}