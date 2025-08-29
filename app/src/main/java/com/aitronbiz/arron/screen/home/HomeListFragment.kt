package com.aitronbiz.arron.screen.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aitronbiz.arron.R
import com.aitronbiz.arron.util.CustomUtil.TAG
import com.aitronbiz.arron.api.RetrofitClient
import com.aitronbiz.arron.api.response.Home
import com.aitronbiz.arron.AppController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aitronbiz.arron.screen.setting.SettingsFragment
import com.aitronbiz.arron.util.CustomUtil.layoutType
import com.aitronbiz.arron.util.CustomUtil.replaceFragment2

class HomeListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { HomeListScreen(onBack = {
                when(layoutType) {
                    3 -> replaceFragment2(requireActivity().supportFragmentManager, SettingsFragment(), null)
                    else -> replaceFragment2(requireActivity().supportFragmentManager, MainFragment(), null)
                }
            }) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when(layoutType) {
                        3 -> replaceFragment2(requireActivity().supportFragmentManager, SettingsFragment(), null)
                        else -> replaceFragment2(requireActivity().supportFragmentManager, MainFragment(), null)
                    }
                }
            }
        )
    }
}

@Composable
private fun HomeListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    var homeList by remember { mutableStateOf<List<Home>>(emptyList()) }

    // 데이터 로드
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val token = AppController.prefs.getToken()
                val response = RetrofitClient.apiService.getAllHome("Bearer $token")
                if (response.isSuccessful) {
                    val homes = response.body()?.homes ?: emptyList()
                    withContext(Dispatchers.Main) { homeList = homes }
                } else {
                    Log.e(TAG, "getAllHome: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAllHome: ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2B4E))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 상단 바
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "홈",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    replaceFragment2(activity.supportFragmentManager, AddHomeFragment(), null)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "추가하기",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(homeList) { home ->
                    Box(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .height(49.dp)
                            .clickable {
                                val bundle = Bundle().apply { putString("homeId", home.id) }
                                replaceFragment2(
                                    fragmentManager = activity.supportFragmentManager,
                                    fragment = SettingHomeFragment(),
                                    bundle = bundle
                                )
                            }
                            .background(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                FrameLayout(ctx).apply {
                                    background = ContextCompat.getDrawable(
                                        ctx,
                                        R.drawable.rec_10_blue
                                    )
                                }
                            },
                            modifier = Modifier.matchParentSize()
                        )

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material.Text(
                                text = home.name,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}