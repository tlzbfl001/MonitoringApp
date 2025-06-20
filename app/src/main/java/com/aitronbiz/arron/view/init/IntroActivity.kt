package com.aitronbiz.arron.view.init

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.aitronbiz.arron.MainActivity
import com.aitronbiz.arron.databinding.ActivityIntroBinding
import com.aitronbiz.arron.util.CustomUtil.TAG

class IntroActivity : AppCompatActivity() {
    private var _binding: ActivityIntroBinding? = null
    private val binding get() = _binding!!

    private var adapter: PagerAdapter = PagerAdapter(supportFragmentManager)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK

            when(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> setStatusBarIconColor(true)
                else -> setStatusBarIconColor(false)
            }
        }

        adapter.add(IntroFragment1(), "1")
        adapter.add(IntroFragment2(), "2")

        binding.viewpager.adapter = adapter
        binding.dotsIndicator.setViewPager(binding.viewpager)

        binding.viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        binding.btnSkip.visibility = View.VISIBLE
                        binding.btnContinue.visibility = View.VISIBLE
                        binding.btnStart.visibility = View.GONE
                    }
                    1 -> {
                        binding.btnSkip.visibility = View.GONE
                        binding.btnContinue.visibility = View.GONE
                        binding.btnStart.visibility = View.VISIBLE
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        })

        binding.btnSkip.setOnClickListener {
            startActivity(Intent(this@IntroActivity, LoginActivity::class.java))
        }

        binding.btnContinue.setOnClickListener {
            binding.viewpager.currentItem = 1
        }

        binding.btnStart.setOnClickListener {
            startActivity(Intent(this@IntroActivity, LoginActivity::class.java))
        }
    }

    class PagerAdapter(fm: FragmentManager): FragmentStatePagerAdapter(fm) {
        private val fragmentList = ArrayList<Fragment>()
        private val fragmentTitle = ArrayList<String>()

        override fun getCount(): Int = fragmentList.size
        override fun getItem(position: Int): Fragment = fragmentList[position]
        override fun getPageTitle(position: Int): CharSequence = fragmentTitle[position]
        fun add(fragment: Fragment, title: String) {
            fragmentList.add(fragment)
            fragmentTitle.add(title)
        }
    }

    private fun setStatusBarIconColor(isDarkText: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                if (isDarkText) {
                    it.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    it.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            // API 30 이하 처리
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (isDarkText) {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
    }
}