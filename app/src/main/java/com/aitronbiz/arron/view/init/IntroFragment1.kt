package com.aitronbiz.arron.view.init

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aitronbiz.arron.R
import com.aitronbiz.arron.databinding.FragmentDetailBinding
import com.aitronbiz.arron.databinding.FragmentIntro1Binding

class IntroFragment1 : Fragment() {
    private var _binding: FragmentIntro1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntro1Binding.inflate(inflater, container, false)

        return binding.root
    }
}