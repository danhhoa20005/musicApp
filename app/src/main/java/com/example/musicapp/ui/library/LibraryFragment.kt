package com.example.musicapp.ui.library

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.musicapp.R
import com.example.musicapp.data.SongFilter
import com.example.musicapp.data.SongStore
import com.example.musicapp.databinding.FragmentLibraryBinding
import com.example.musicapp.ui.main.MainActivity
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
            onShuffleAll()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onShuffleAll() {
        val activity = requireActivity() as MainActivity
        val service = activity.musicService
        if (service == null) {
            Toast.makeText(requireContext(), R.string.service_not_ready, Toast.LENGTH_SHORT).show()
            return
        }

        if (!hasReadPermission()) {
            Toast.makeText(requireContext(), R.string.permission_audio_needed, Toast.LENGTH_SHORT).show()
            return
        }

        val songs = SongStore.loadDeviceSongs(requireContext(), SongFilter.ALL)
        if (songs.isEmpty()) {
            Toast.makeText(requireContext(), R.string.toast_no_songs, Toast.LENGTH_SHORT).show()
            return
        }

        val startIndex = songs.indices.random()
        service.setPlaylist(songs, startIndex, playNow = true)
        findNavController().navigate(R.id.action_libraryFragment_to_playerFragment)
    }

    private fun hasReadPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
