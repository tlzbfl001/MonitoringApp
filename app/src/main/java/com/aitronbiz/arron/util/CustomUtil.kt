package com.aitronbiz.arron.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.aitronbiz.arron.R
import com.prolificinteractive.materialcalendarview.CalendarDay

object CustomUtil {
    const val TAG = "logTAG"

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
}