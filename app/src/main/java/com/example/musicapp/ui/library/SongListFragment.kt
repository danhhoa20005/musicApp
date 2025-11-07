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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicapp.R
import com.example.musicapp.data.SongFilter
import com.example.musicapp.databinding.RecyclerSongsBinding
import com.example.musicapp.ui.main.MainActivity
import com.example.musicapp.ui.viewmodel.LibraryViewModel
import com.example.musicapp.util.navigateFrom

// SongListFragment - hiển thị danh sách theo bộ lọc, dùng ViewModel để nạp/cache
class SongListFragment : Fragment() {

    companion object {
        private const val ARG_FILTER = "filter"
        // newInstance - tạo fragment kèm bộ lọc
        fun newInstance(filter: SongFilter) = SongListFragment().apply {
            arguments = bundleOf(ARG_FILTER to filter.name)
        }
    }

    // binding - truy cập view
    private var _binding: RecyclerSongsBinding? = null
    private val binding get() = _binding!!

    // adapter - danh sách bài hát + xử lý click
    private lateinit var adapter: SongAdapter

    // filter - bộ lọc (ALL/MP3_ONLY/RECENT)
    private val filter: SongFilter by lazy {
        val name = arguments?.getString(ARG_FILTER) ?: SongFilter.ALL.name
        runCatching { SongFilter.valueOf(name) }.getOrDefault(SongFilter.ALL)
    }

    // libraryViewModel - nguồn dữ liệu bài hát
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    // onCreateView - tạo view binding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = RecyclerSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // onViewCreated - cấu hình recycler, observe dữ liệu, xử lý quyền và nạp
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // adapter - nhấn vào bài -> setPlaylist + mở Player
        adapter = SongAdapter { _, position ->
            val service = (requireActivity() as MainActivity).musicService
            val list = adapter.currentList
            if (service == null) {
                Toast.makeText(requireContext(), R.string.service_not_ready, Toast.LENGTH_SHORT).show()
                return@SongAdapter
            }
            service.setPlaylist(list, startIndex = position, playNow = true)
            findNavController().navigateFrom(
                R.id.libraryFragment,
                R.id.action_libraryFragment_to_playerFragment
            )
        }

        // recycler - layout dọc + gán adapter
        binding.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SongListFragment.adapter
        }

        // observe - nhận danh sách từ ViewModel
        libraryViewModel.songs.observe(viewLifecycleOwner) { songs ->
            adapter.submitList(songs)
            binding.textEmpty.isVisible = songs.isEmpty()
        }

        // kiểmQuyền - thiếu quyền thì dọn danh sách và báo trống
        if (!hasReadPermission()) {
            Toast.makeText(requireContext(), R.string.permission_audio_needed, Toast.LENGTH_SHORT).show()
            showEmptyState(R.string.permission_audio_needed)
            libraryViewModel.clear()
            return
        }

        // nạpDữLiệu - gọi ViewModel để tải theo bộ lọc
        libraryViewModel.load(filter)
    }

    // showEmptyState - hiển thị trạng thái trống
    private fun showEmptyState(@StringRes messageRes: Int = R.string.empty_song_list) {
        adapter.submitList(emptyList())
        binding.textEmpty.setText(messageRes)
        binding.textEmpty.isVisible = true
    }

    // hasReadPermission - kiểm tra quyền đọc audio theo phiên bản Android
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

    // onDestroyView - huỷ binding
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
