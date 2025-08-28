package com.aitronbiz.arron.screen.home

import android.os.Bundle
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aitronbiz.arron.databinding.FragmentEntryPatternBinding
import com.aitronbiz.arron.screen.notification.NotificationFragment
import com.aitronbiz.arron.util.CustomUtil.replaceFragment
import com.aitronbiz.arron.viewmodel.EntryPatternsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

class EntryPatternFragment : Fragment() {
    private var _binding: FragmentEntryPatternBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_HOME_ID = "argHomeId"
        private const val ARG_DATE_EPOCH = "argDateEpoch"

        fun newInstance(homeId: String, date: LocalDate) =
            EntryPatternFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HOME_ID, homeId)
                    putLong(ARG_DATE_EPOCH, date.toEpochDay())
                }
            }
    }

    private val viewModel: EntryPatternsViewModel by viewModels()

    private val homeId: String by lazy { arguments?.getString(ARG_HOME_ID).orEmpty() }
    private val selectedDate: LocalDate by lazy {
        LocalDate.ofEpochDay(arguments?.getLong(ARG_DATE_EPOCH) ?: LocalDate.now().toEpochDay())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEntryPatternBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarContainer) { view, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.updatePadding(top = top)
            insets
        }

        binding.btnBk.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.tvSelectedDate.text = selectedDate.toString()

        // 알림 배지
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.checkNotifications { has ->
                binding.badgeDot.visibility = if (has) View.VISIBLE else View.GONE
            }
        }

        binding.btnBell.setOnClickListener {
            val f = NotificationFragment().apply {
                arguments = Bundle().apply {
                    putString("homeId", homeId)
                    putLong("selectedDate", selectedDate.toEpochDay())
                }
            }
            replaceFragment(parentFragmentManager, f, null)
        }

        // 초기 로딩
        viewModel.resetState(homeId)
        viewModel.fetchAvgActivityForHome(homeId, selectedDate)

        viewModel.fetchEntryPatternsData(homeId, selectedDate)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stop()
        _binding = null
    }
}