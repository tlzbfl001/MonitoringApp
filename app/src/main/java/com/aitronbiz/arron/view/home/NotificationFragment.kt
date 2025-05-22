package com.aitronbiz.arron.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitronbiz.arron.adapter.NotificationAdapter
import com.aitronbiz.arron.entity.Subject
import com.aitronbiz.arron.databinding.FragmentNotificationBinding
import com.aitronbiz.arron.util.CustomUtil.replaceFragment1

class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NotificationAdapter
    private val subjects = listOf(
        Subject(name = "대상자1", birthdate = "48세"),
        Subject(name = "대상자2", birthdate = "55세"),
        Subject(name = "대상자3", birthdate = "58세")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)

        binding.btnBack.setOnClickListener {
            replaceFragment1(requireActivity().supportFragmentManager, MainFragment())
        }

        binding.tvNotificationCount.text = "${subjects.size}개의 알림"

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = NotificationAdapter(subjects)
        recyclerView.adapter = adapter

        return binding.root
    }
}