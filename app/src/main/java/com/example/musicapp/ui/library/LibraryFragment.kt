package com.example.musicapp.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.musicapp.R
import com.example.musicapp.data.SongFilter
import com.example.musicapp.databinding.FragmentLibraryBinding
import com.example.musicapp.ui.viewmodel.NowPlayingViewModel
import com.example.musicapp.ui.viewmodel.ServiceConnectionViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val serviceVM: ServiceConnectionViewModel by activityViewModels()
    private val nowVM: NowPlayingViewModel by activityViewModels()

    private lateinit var pagerAdapter: LibraryPagerAdapter
    private val pages by lazy { listOf(LibraryPage(R.string.tab_all_songs, SongFilter.ALL)) }

    private var musicService: com.example.musicapp.player.MusicService? = null

    // Mini player views (ở trong bottomSheet)
    private var bottomSheet: View? = null
    private lateinit var behavior: BottomSheetBehavior<View>
    private var miniPlay: ImageView? = null
    private var miniPrev: ImageView? = null
    private var miniNext: ImageView? = null
    private var miniTitle: TextView? = null
    private var miniSub: TextView? = null
    private var miniArt: ImageView? = null

    // Cờ chống navigate nhiều lần khi kéo vượt 70%
    private var navigatedToPlayer = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPager + TabLayout
        pagerAdapter = LibraryPagerAdapter(this, pages)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.offscreenPageLimit = pages.size
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = getString(pages[pos].titleRes)
        }.attach()

        // Lấy bottomSheet & behavior
        bottomSheet = view.findViewById(R.id.bottomSheet)
        requireNotNull(bottomSheet) { "bottomSheet missing. Hãy dùng view_mini_player_sheet.xml như đã hướng dẫn." }
        behavior = BottomSheetBehavior.from(bottomSheet!!)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        behavior.isHideable = true
        behavior.skipCollapsed = false
        behavior.saveFlags = BottomSheetBehavior.SAVE_ALL

        // Ánh xạ control trong mini
        miniPlay  = bottomSheet!!.findViewById(R.id.mini_btnPlayPause)
        miniPrev  = bottomSheet!!.findViewById(R.id.mini_btnPrev)
        miniNext  = bottomSheet!!.findViewById(R.id.mini_btnNext)
        miniTitle = bottomSheet!!.findViewById(R.id.mini_textTitle)
        miniSub   = bottomSheet!!.findViewById(R.id.mini_textSubtitle)
        miniArt   = bottomSheet!!.findViewById(R.id.mini_imageArt)

        // Click mini: có thể mở thẳng Player
        bottomSheet!!.setOnClickListener { openPlayer() }

        // Điều khiển phát từ mini
        miniPlay?.setOnClickListener { musicService?.toggle() }
        miniPrev?.setOnClickListener { musicService?.previous() }
        miniNext?.setOnClickListener { musicService?.next() }

        // Callback kéo: nếu kéo >= 70% thì mở PlayerFragment
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(sheet: View, slideOffset: Float) {
                // slideOffset: [-1, 1]; với BottomSheet standard: 0≈collapsed, 1=expanded.
                if (slideOffset >= 0.7f && !navigatedToPlayer) {
                    navigatedToPlayer = true
                    // Đưa về collapsed để không che animation điều hướng
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    openPlayer()
                }
                // Cập padding cho content tương ứng peekHeight khi gần collapsed
                if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED || slideOffset in 0f..1f) {
                    setBottomInset(behavior.peekHeight)
                }
            }
            override fun onStateChanged(sheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    setBottomInset(0)
                }
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    setBottomInset(behavior.peekHeight)
                }
                // Khi user kéo xuống/đóng, cho phép navigate lần sau
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                    navigatedToPlayer = false
                }
            }
        })

        // Quan sát service
        serviceVM.service.observe(viewLifecycleOwner) { srv ->
            musicService = srv
            nowVM.attachService(srv)
        }

        // Quan sát bài hiện tại: chỉ hiện khi có bài (user đã bấm nhạc)
        nowVM.currentSong.observe(viewLifecycleOwner) { song ->
            if (song == null) {
                hideMini()
            } else {
                miniTitle?.text = song.title
                miniSub?.text = song.artist ?: ""
                // Đặt peekHeight theo chiều cao thực của mini
                bottomSheet?.post {
                    val peek = bottomSheet?.height ?: 0
                    behavior.peekHeight = if (peek > 0) peek else dp(72)
                    showMini()
                }
            }
        }

        // Nút play/pause
        nowVM.isPlaying.observe(viewLifecycleOwner) { playing ->
            miniPlay?.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        }

        // Trạng thái ban đầu ẩn hoàn toàn
        hideMini()
    }

    override fun onDestroyView() {
        musicService = null
        _binding = null
        super.onDestroyView()
    }

    // Điều hướng Player
    private fun openPlayer() {
        // Điều hướng sang PlayerFragment
        findNavController().navigate(R.id.action_libraryFragment_to_playerFragment)
    }

    // Hiện mini: bottom sheet chuyển về collapsed (chỉ cao bằng mini)
    private fun showMini() {
        if (behavior.state == BottomSheetBehavior.STATE_HIDDEN ||
            behavior.state == BottomSheetBehavior.STATE_DRAGGING) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            setBottomInset(behavior.peekHeight)
        }
    }

    // Ẩn mini
    private fun hideMini() {
        if (this::behavior.isInitialized) {
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
            setBottomInset(0)
        }
    }

    // Đệm nội dung phía dưới = chiều cao mini khi mini đang hiện
    private fun setBottomInset(pixels: Int) {
        val c = binding.contentContainer
        c.setPadding(c.paddingLeft, c.paddingTop, c.paddingRight, pixels)
    }

    private fun dp(v: Int): Int {
        val d = resources.displayMetrics.density
        return (v * d + 0.5f).toInt()
    }
}
