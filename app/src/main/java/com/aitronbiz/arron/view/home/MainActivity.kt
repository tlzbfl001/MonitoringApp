package com.aitronbiz.arron.view.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aitronbiz.arron.R
import com.aitronbiz.arron.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.mainFrame, MainFragment())
            commit()
        }
    }
}