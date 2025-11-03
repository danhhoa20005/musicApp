package com.example.musicapp.ui.library

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicapp.R
import com.example.musicapp.data.SongStore
import com.example.musicapp.data.model.Song
import com.example.musicapp.databinding.RecyclerSongsBinding
import com.example.musicapp.ui.main.MainActivity

/**
 * Fragment con hiển thị toàn bộ bài hát cục bộ.
 * Dùng RecyclerView + item_song.xml (bạn đã có).
 */
class AllSongsFragment : Fragment() {

    private var _binding: RecyclerSongsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SongAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Tạo layout RecyclerView đơn giản bằng viewbinding tự chế (RecyclerSongsBinding)
        // Bạn có thể thay bằng 1 file layout khác tuỳ ý.
        _binding = RecyclerSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SongAdapter { song, position ->
            val service = (requireActivity() as MainActivity).musicService
            val list = adapter.currentList
            if (service == null) {
                Toast.makeText(requireContext(), "Service not ready", Toast.LENGTH_SHORT).show()
                return@SongAdapter
            }
            service.setPlaylist(list, startIndex = position, playNow = true)
            findNavController().navigate(R.id.action_libraryFragment_to_playerFragment)
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AllSongsFragment.adapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        // Xin quyền nếu cần (dự phòng)
        if (!hasReadPermission()) {
            Toast.makeText(requireContext(), "App needs storage/audio permission", Toast.LENGTH_SHORT).show()
            return
        }

        // Nạp dữ liệu
        val songs = SongStore.loadDeviceSongs(requireContext())
        adapter.submitList(songs)
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
