package com.example.musicapp.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.musicapp.databinding.FragmentLibraryBinding
import com.google.android.material.tabs.TabLayoutMediator


class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: LibraryPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tạo adapter cho ViewPager2
        pagerAdapter = LibraryPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = true

        // Gắn TabLayout với ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "All Songs"
                else -> "Tab ${position + 1}"
            }
        }.attach()

        // Nút Shuffle (tạm ẩn nếu chưa cần)
        binding.fabShuffle.isVisible = true
        binding.fabShuffle.setOnClickListener {
            // TODO: triển khai shuffle nếu cần
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
