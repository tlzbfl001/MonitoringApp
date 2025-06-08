package com.aitronbiz.arron.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.aitronbiz.arron.R
import com.prolificinteractive.materialcalendarview.CalendarDay

object CustomUtil {
    const val TAG = "logTAG"

    fun replaceFragment1(fragmentManager: FragmentManager, fragment: Fragment?) {
        fragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.slide_in_right, // 진입 애니메이션
                R.anim.slide_out_left, // 퇴장 애니메이션
                R.anim.slide_in_left, // 팝 진입 애니메이션 (뒤로가기)
                R.anim.slide_out_right // 팝 퇴장 애니메이션 (뒤로가기)
            )
            replace(R.id.mainFrame, fragment!!)
            addToBackStack(null)
            commit()
        }
    }

    fun replaceFragment2(fragmentManager: FragmentManager, fragment: Fragment?, bundle: Bundle?) {
        fragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            fragment?.arguments = bundle
            replace(R.id.mainFrame, fragment!!)
            addToBackStack(null)
            commit()
        }
    }

    fun networkStatus(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun getFormattedDate(date: CalendarDay): String {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonth(), date.getDay())
    }

    fun setStatusBar(context: Activity, mainLayout: ConstraintLayout) {
        context.window?.apply {
            val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else { 0 }
            mainLayout.setPadding(0, statusBarHeight, 0, 0)
        }
    }
}