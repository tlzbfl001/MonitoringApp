package kr.aitron.aitron

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kr.aitron.aitron.databinding.ActivityMainBinding
import kr.aitron.aitron.view.MainFragment

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