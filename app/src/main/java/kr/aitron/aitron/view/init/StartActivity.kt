package kr.aitron.aitron.view.init

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kr.aitron.aitron.AppController
import kr.aitron.aitron.R
import kr.aitron.aitron.database.DataManager
import kr.aitron.aitron.view.home.MainActivity

class StartActivity : AppCompatActivity() {
    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        this.window?.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.WHITE
            navigationBarColor = Color.WHITE
        }

        dataManager = DataManager(this)
        dataManager.open()

        val getUser = dataManager.getUser()

        if(AppController.prefs.getUserPrefs() < 1 || (AppController.prefs.getUserPrefs() > 0 && getUser.email == "")) {
            startActivity(Intent(this, LoginActivity::class.java))
        }else {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}