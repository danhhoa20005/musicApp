package com.example.musicapp.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.musicapp.R
import com.example.musicapp.data.SongFilter
import com.example.musicapp.databinding.FragmentLibraryBinding
import com.google.android.material.tabs.TabLayoutMediator

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: LibraryPagerAdapter

    private val pages by lazy {
        listOf(
            LibraryPage(R.string.tab_all_songs, SongFilter.ALL),
            LibraryPage(R.string.tab_mp3_songs, SongFilter.MP3_ONLY),
            LibraryPage(R.string.tab_recent_songs, SongFilter.RECENT)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pagerAdapter = LibraryPagerAdapter(this, pages)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.offscreenPageLimit = pages.size

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getString(pages[position].titleRes)
        }.attach()

        binding.fabShuffle.isVisible = true
        binding.fabShuffle.setOnClickListener {

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
