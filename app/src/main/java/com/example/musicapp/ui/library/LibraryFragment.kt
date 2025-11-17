package com.example.musicapp.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.musicapp.R
import com.example.musicapp.data.artist.ArtistCovers
import com.example.musicapp.data.song.SongFilter
import com.example.musicapp.databinding.FragmentLibraryBinding
import com.example.musicapp.ui.artist.ArtistAdapter
import com.example.musicapp.ui.player.PlayerFragment
import com.example.musicapp.ui.song.SongListFragment
import com.example.musicapp.ui.util.CoverResolver
import com.example.musicapp.ui.viewmodel.LibraryViewModel
import com.example.musicapp.ui.viewmodel.NowPlayingViewModel
import com.example.musicapp.ui.viewmodel.ServiceConnectionViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator

// LibraryFragment: màn thư viện chính, gồm tabs bài hát + hàng artist ngang + pull-to-refresh
class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val serviceVM: ServiceConnectionViewModel by activityViewModels()
    private val nowVM: NowPlayingViewModel by activityViewModels()
    private val libraryVM: LibraryViewModel by activityViewModels()

    private lateinit var pagerAdapter: LibraryPagerAdapter
    private lateinit var artistAdapter: ArtistAdapter

    // pages: cấu hình 3 tab (All / Favorites / Recent) cho ViewPager
    private val pages by lazy {
        listOf(
            LibraryPage(R.string.tab_all_songs, SongFilter.ALL),
            LibraryPage(R.string.tab_favorites, SongFilter.FAVORITE),
            LibraryPage(R.string.tab_recent, SongFilter.RECENT)
        )
    }

    // onCreateView: inflate layout và trả về root view
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    // onViewCreated: khởi tạo UI (ViewPager, tabs, hàng artist, swipe refresh) và gắn observer
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPager + TabLayout: gắn adapter và tiêu đề từng tab
        pagerAdapter = LibraryPagerAdapter(this, pages)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.offscreenPageLimit = pages.size
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = getString(pages[pos].titleRes)
        }.attach()

        // Hàng artist ngang
        setupArtistRow()

        // Observer danh sách artist từ ViewModel, cập nhật adapter + ẩn/hiện section
        libraryVM.artists.observe(viewLifecycleOwner) { artists ->
            artistAdapter.submitListAndApplyCovers(artists)
            binding.authorSection.isVisible = artists.isNotEmpty()
            binding.swipeRefresh.isRefreshing = false
        }

        // Load dữ liệu lần đầu (songs + artists)
        binding.swipeRefresh.isRefreshing = true
        libraryVM.load(SongFilter.ALL)

        // Chỉnh khoảng kéo để trigger refresh (kéo xa hơn chút mới refresh)
        val density = resources.displayMetrics.density
        val distanceDp = 200f
        val distancePx = (distanceDp * density).toInt()
        binding.swipeRefresh.setDistanceToTriggerSync(distancePx)

        // Listener kéo để refresh: reload lại dữ liệu Library + list trong các tab
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = true
            libraryVM.load(SongFilter.ALL)

            childFragmentManager.fragments.forEach { f ->
                if (f is SongListFragment) {
                    f.refreshSongs()
                }
            }
        }

        // Gắn service phát nhạc cho NowPlayingViewModel (không xử lý UI bottom sheet tại đây)
        serviceVM.service.observe(viewLifecycleOwner) { srv ->
            nowVM.attachService(srv)
        }
    }

    // setupArtistRow: cấu hình RecyclerView hàng ngang hiển thị danh sách artist + xử lý click
    private fun setupArtistRow() {
        artistAdapter = ArtistAdapter()
        binding.rcTacGia.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = artistAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        // set map cover cho artist + cover mặc định
        artistAdapter.setCoverMap(ArtistCovers.covers)
        artistAdapter.setDefaultCoverUrl(ArtistCovers.defaultCover)

        // xử lý click artist: điều hướng sang ArtistFragment, truyền tên + cover
        artistAdapter.setOnItemClick { artist ->
            val args = bundleOf(
                "artistName" to artist.name,
                "artistCoverUrl" to (artist.cover?.toString() ?: "")
            )
            findNavController().navigate(
                R.id.action_libraryFragment_to_artistFragment,
                args
            )
        }
    }

    // onDestroyView: giải phóng binding khi view bị destroy để tránh leak
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
