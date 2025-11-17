package com.example.musicapp.ui.song

import android.Manifest
import android.content.Intent
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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicapp.R
import com.example.musicapp.data.song.SongFilter
import com.example.musicapp.databinding.RecyclerSongsBinding
import com.example.musicapp.player.MusicService
import com.example.musicapp.ui.song.SongAdapter
import com.example.musicapp.ui.main.MainActivity
import com.example.musicapp.ui.viewmodel.LibraryViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior

class SongListFragment : Fragment() {

    companion object {
        private const val ARG_FILTER = "filter"
        fun newInstance(filter: SongFilter) = SongListFragment().apply {
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

    // ViewModel riêng cho từng tab
    private val libraryViewModel: LibraryViewModel by viewModels()

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
            startMusicServiceIfNeeded()
            val activity = requireActivity() as MainActivity
            val service = activity.musicService
            val list = adapter.currentList

            if (service == null) {
                Toast.makeText(
                    requireContext(),
                    R.string.service_not_ready,
                    Toast.LENGTH_SHORT
                ).show()
                return@SongAdapter
            }

            service.setPlaylist(list, position, true)

            val sheet = activity.findViewById<View>(R.id.nowPlayingSheet)
            val behavior = BottomSheetBehavior.from(sheet)
            sheet.visibility = View.VISIBLE
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SongListFragment.adapter
        }

        libraryViewModel.songs.observe(viewLifecycleOwner) { songs ->
            adapter.submitList(songs)
            binding.textEmpty.isVisible = songs.isEmpty()
        }

        if (!hasReadPermission()) {
            Toast.makeText(
                requireContext(),
                R.string.permission_audio_needed,
                Toast.LENGTH_SHORT
            ).show()
            showEmptyState(R.string.permission_audio_needed)
            libraryViewModel.clear()
            return
        }

        // load lần đầu
        libraryViewModel.load(filter)
    }

    // 👉 HÀM NÀY ĐỂ LibraryFragment GỌI KHI KÉO REFRESH
    fun refreshSongs() {
        if (!hasReadPermission()) return
        libraryViewModel.load(filter)
    }

    private fun showEmptyState(@StringRes msg: Int) {
        adapter.submitList(emptyList())
        binding.textEmpty.setText(msg)
        binding.textEmpty.isVisible = true
    }

    private fun hasReadPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33)
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        else
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startMusicServiceIfNeeded() {
        val ctx = requireContext().applicationContext
        val intent = Intent(ctx, MusicService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ContextCompat.startForegroundService(ctx, intent)
        else
            ctx.startService(intent)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}