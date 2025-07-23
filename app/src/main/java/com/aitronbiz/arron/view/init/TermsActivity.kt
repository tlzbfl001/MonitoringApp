package com.aitronbiz.arron.view.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aitronbiz.arron.view.MainActivity
import com.aitronbiz.arron.database.DataManager
import com.aitronbiz.arron.databinding.ActivityTermsBinding

class TermsActivity : AppCompatActivity() {
    private var _binding: ActivityTermsBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private var checkAll = true
    private var check1 = true
    private var check2 = true
    private var check3 = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityTermsBinding.inflate(layoutInflater)
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

        dataManager = DataManager.getInstance(this)

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.checkAll.setOnClickListener {
            binding.checkAll.isChecked = checkAll
            checkAll = !checkAll
            if(binding.checkAll.isChecked) {
                binding.check1.isChecked = true
                binding.check2.isChecked = true
                binding.check3.isChecked = true
            }else {
                binding.check1.isChecked = false
                binding.check2.isChecked = false
                binding.check3.isChecked = false
            }
        }

        binding.check1.setOnClickListener {
            binding.check1.isChecked = check1
            check1 = !check1
            updateCheckAllState()
        }

        binding.check2.setOnClickListener {
            binding.check2.isChecked = check2
            check2 = !check2
            updateCheckAllState()
        }

        binding.check3.setOnClickListener {
            binding.check3.isChecked = check3
            check3 = !check3
            updateCheckAllState()
        }

        binding.btnTerms1.setOnClickListener {
            val intent = Intent(this, Terms1Activity::class.java)
            intent.putExtra("type", 1)
            startActivity(intent)
        }

        binding.btnTerms2.setOnClickListener {
            val intent = Intent(this, Terms2Activity::class.java)
            intent.putExtra("type", 1)
            startActivity(intent)
        }

        binding.btnTerms3.setOnClickListener {
            val intent = Intent(this, Terms3Activity::class.java)
            intent.putExtra("type", 1)
            startActivity(intent)
        }

        binding.btnConfirm.setOnClickListener {
            if (binding.check1.isChecked && binding.check2.isChecked && binding.check3.isChecked) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }else {
                Toast.makeText(this, "필수 이용약관에 모두 체크해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCheckAllState() {
        if (binding.check1.isChecked && binding.check2.isChecked && binding.check3.isChecked) {
            binding.checkAll.isChecked = true
        }

        if (!binding.check1.isChecked || !binding.check2.isChecked || !binding.check3.isChecked) {
            binding.checkAll.isChecked = false
        }
    }
}