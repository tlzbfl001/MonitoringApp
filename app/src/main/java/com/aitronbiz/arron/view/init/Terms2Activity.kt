package com.aitronbiz.arron.view.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aitronbiz.arron.R
import com.aitronbiz.arron.databinding.ActivityTerms1Binding
import com.aitronbiz.arron.databinding.ActivityTerms2Binding

class Terms2Activity : AppCompatActivity() {
    private var _binding: ActivityTerms2Binding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityTerms2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바 관련 설정
        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.BLACK

            val resourceId =
                context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight =
                if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
            binding.mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }

        val type = intent.getIntExtra("type", 0)

        binding.btnBack.setOnClickListener {
            if(type == 1) {
                val intent = Intent(this, TermsActivity::class.java)
                startActivity(intent)
            }else if(type == 2) {
                val intent = Intent(this, SignUpActivity::class.java)
                startActivity(intent)
            }
        }
    }
}