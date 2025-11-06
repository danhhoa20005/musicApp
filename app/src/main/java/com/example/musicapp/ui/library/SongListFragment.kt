package com.example.musicapp.ui.library

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicapp.R
import com.example.musicapp.data.SongFilter
import com.example.musicapp.data.SongStore
import com.example.musicapp.databinding.RecyclerSongsBinding
import com.example.musicapp.ui.main.MainActivity


class SongListFragment : Fragment() {

    companion object {
        private const val ARG_FILTER = "filter"

        fun newInstance(filter: SongFilter): SongListFragment = SongListFragment().apply {
            arguments = bundleOf(ARG_FILTER to filter.name)
        }
    }

    private var _binding: RecyclerSongsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SongAdapter

    private val filter: SongFilter by lazy {
        val name = arguments?.getString(ARG_FILTER) ?: SongFilter.ALL.name
        runCatching { SongFilter.valueOf(name) }.getOrDefault(SongFilter.ALL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RecyclerSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SongAdapter { _, position ->
            val service = (requireActivity() as MainActivity).musicService
            val list = adapter.currentList
            if (service == null) {
                Toast.makeText(requireContext(), R.string.service_not_ready, Toast.LENGTH_SHORT).show()
                return@SongAdapter
            }
            service.setPlaylist(list, startIndex = position, playNow = true)
            findNavController().navigate(R.id.action_libraryFragment_to_playerFragment)
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SongListFragment.adapter

        }

        if (!hasReadPermission()) {
            Toast.makeText(
                requireContext(),
                R.string.permission_audio_needed,
                Toast.LENGTH_SHORT
            ).show()
            showEmptyState(R.string.permission_audio_needed)
            return
        }

        val songs = SongStore.loadDeviceSongs(requireContext(), filter)
        adapter.submitList(songs)
        binding.textEmpty.isVisible = songs.isEmpty()
    }

    private fun showEmptyState(@StringRes messageRes: Int = R.string.empty_song_list) {
        adapter.submitList(emptyList())
        binding.textEmpty.setText(messageRes)
        binding.textEmpty.isVisible = true
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
