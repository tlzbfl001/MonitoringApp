package kr.aitron.aitron.util

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kr.aitron.aitron.R

object CustomUtil {
    const val TAG = "appTAG"

    fun replaceFragment1(fragmentManager: FragmentManager, fragment: Fragment?) {
        fragmentManager.beginTransaction().apply {
            replace(R.id.mainFrame, fragment!!)
            addToBackStack(null)
            commit()
        }
    }

    fun replaceFragment2(fragmentManager: FragmentManager, fragment: Fragment?, bundle: Bundle?) {
        fragmentManager.beginTransaction().apply {
            fragment?.arguments = bundle
            replace(R.id.mainFrame, fragment!!)
            addToBackStack(null)
            commit()
        }
    }
}