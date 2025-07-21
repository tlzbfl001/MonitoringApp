package com.aitronbiz.arron.view.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aitronbiz.arron.R
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.FragmentFallDetectionBinding
import com.aitronbiz.arron.databinding.FragmentMainBinding

class FallDetectionFragment : Fragment() {
    private var _binding: FragmentFallDetectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFallDetectionBinding.inflate(inflater, container, false)


        return binding.root
    }
}