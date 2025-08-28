package com.aitronbiz.arron.screen.notification

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aitronbiz.arron.adapter.NotificationAdapter
import com.aitronbiz.arron.adapter.NotificationRow
import com.aitronbiz.arron.api.response.NotificationData
import com.aitronbiz.arron.databinding.FragmentNotificationBinding
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import com.aitronbiz.arron.viewmodel.NotificationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationViewModel by viewModels()
    private var spinAnim: ObjectAnimator? = null

    private val adapter by lazy {
        NotificationAdapter(onItemClick = ::onItemClicked)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            (requireContext() as FragmentActivity).onBackPressedDispatcher.onBackPressed()
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter

        spinAnim = ObjectAnimator.ofFloat(binding.ivSpinner, View.ROTATION, 0f, 360f).apply {
            duration = 1200L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        // 스크롤 리스너
        binding.rvNotifications.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                val total = adapter.itemCount
                // 마지막 3개 안쪽으로 오면 다음 페이지 로드
                if (total > 0 && lastVisible >= total - 3) {
                    viewModel.loadNextPage()
                }
            }
        })

        // 데이터 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.notifications.collectLatest { list ->
                        adapter.submitList(buildRows(list))
                    }
                }
                launch {
                    viewModel.loading.collectLatest { loading ->
                        updateFooter(loading, viewModel.hasMore.value)
                    }
                }
                launch {
                    viewModel.hasMore.collectLatest { hasMore ->
                        updateFooter(viewModel.loading.value, hasMore)
                    }
                }
            }
        }

        // 최초 로드
        viewModel.refresh()
    }

    private fun updateFooter(loading: Boolean, hasMore: Boolean) {
        if (hasMore && loading) {
            binding.footerLoader.visibility = View.VISIBLE
            binding.ivSpinner.visibility = View.VISIBLE
            if (spinAnim?.isRunning != true) spinAnim?.start()
        } else {
            spinAnim?.cancel()
            binding.ivSpinner.rotation = 0f
            binding.footerLoader.visibility = View.GONE
        }
    }

    private fun buildRows(notifications: List<NotificationData>): List<NotificationRow> {
        val byDate = notifications
            .sortedByDescending { it.createdAt }
            .groupBy { it.createdAt?.substringBefore("T") ?: "알 수 없음" }

        val rows = mutableListOf<NotificationRow>()
        byDate.forEach { (date, items) ->
            if (!date.isNullOrEmpty()) {
                rows += NotificationRow.DateHeader(date)
                items.forEach { rows += NotificationRow.Item(it) }
            }
        }
        return rows
    }

    private fun onItemClicked(item: NotificationData) {
        val f = NotificationDetailFragment().apply {
            arguments = Bundle().apply { putString("notificationId", item.id) }
        }
        replaceFragment(parentFragmentManager, f, null)
    }

    override fun onDestroyView() {
        spinAnim?.cancel()
        super.onDestroyView()
    }
}
