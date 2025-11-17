package com.example.musicapp.ui.artist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.musicapp.R
import com.example.musicapp.data.model.Song
import com.example.musicapp.databinding.FragmentArtistSongsBinding
import com.example.musicapp.ui.song.SongAdapter
import com.example.musicapp.ui.main.MainActivity
import com.example.musicapp.ui.viewmodel.LibraryViewModel
import com.example.musicapp.ui.viewmodel.NowPlayingViewModel
import com.example.musicapp.ui.viewmodel.ServiceConnectionViewModel

class ArtistFragment : Fragment() {

    private var _binding: FragmentArtistSongsBinding? = null
    private val binding get() = _binding!!

    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val serviceVM: ServiceConnectionViewModel by activityViewModels()
    private val nowVM: NowPlayingViewModel by activityViewModels()

    private lateinit var songAdapter: SongAdapter
    private var currentSongs: List<Song> = emptyList()

    private var artistName: String = ""
    private var artistCoverUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.let { bundle ->
            artistName = bundle.getString("artistName").orEmpty()
            artistCoverUrl = bundle.getString("artistCoverUrl")
            if (artistCoverUrl.isNullOrBlank()) artistCoverUrl = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupRecyclerView()
        observeSongs()

        serviceVM.service.observe(viewLifecycleOwner) { srv ->
            nowVM.attachService(srv)
        }

        // back / vuốt back → quay về màn trước (LibraryFragment)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )
    }

    private fun setupHeader() {
        binding.textArtistName.text =
            if (artistName.isNotBlank()) artistName
            else getString(R.string.artist_placeholder)

        val cover = artistCoverUrl
        Glide.with(binding.imageArtistCover)
            .load(cover)
            .placeholder(R.drawable.ic_logo)
            .error(R.drawable.ic_logo)
            .fallback(R.drawable.ic_logo)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .into(binding.imageArtistCover)
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter { song, index ->
            val srv = serviceVM.service.value ?: return@SongAdapter
            if (currentSongs.isEmpty()) return@SongAdapter

            // set playlist bài của nghệ sĩ + phát
            srv.setPlaylist(
                list = currentSongs,
                startIndex = index,
                playNow = true
            )

            nowVM.attachService(srv)

            // mở full player (bottom sheet EXPANDED)
            (activity as? MainActivity)?.expandNowPlaying()
        }

        binding.recyclerArtistSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeSongs() {
        libraryViewModel.songs.observe(viewLifecycleOwner) { allSongs ->
            val filtered = allSongs.filter { it.artist == artistName }
            currentSongs = filtered
            songAdapter.submitList(filtered)
            binding.textArtistSubtitle.text = "${filtered.size} bài hát"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
