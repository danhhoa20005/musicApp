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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.musicapp.R
import com.example.musicapp.data.SongFilter
import com.example.musicapp.data.model.Song
import com.example.musicapp.databinding.FragmentLibraryBinding
import com.example.musicapp.ui.viewmodel.NowPlayingViewModel
import com.example.musicapp.ui.viewmodel.ServiceConnectionViewModel
import com.example.musicapp.util.navigateFrom
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator

// LibraryFragment - màn hình thư viện chính với danh sách và now playing peek
class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val serviceVM: ServiceConnectionViewModel by activityViewModels()
    private val nowVM: NowPlayingViewModel by activityViewModels()

    private lateinit var pagerAdapter: LibraryPagerAdapter
    private val pages by lazy { listOf(LibraryPage(R.string.tab_all_songs, SongFilter.ALL)) }

    private var musicService: com.example.musicapp.player.MusicService? = null

    // Now playing peek (kéo lên giống YouTube Music)
    private var nowPlayingSheet: View? = null
    private lateinit var nowPlayingBehavior: BottomSheetBehavior<View>
    private var peekHost: View? = null
    private var peekContainer: View? = null
    private var peekPlay: ImageView? = null
    private var peekPrev: ImageView? = null
    private var peekNext: ImageView? = null
    private var peekTitle: TextView? = null
    private var peekSubtitle: TextView? = null
    private var peekArtwork: ImageView? = null
    private var peekHandle: View? = null
    private var peekBackdrop: View? = null

    // Cờ chống navigate nhiều lần khi kéo vượt 80%
    private var navigatedToPlayer = false
    private var lastSong: Song? = null

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
        nowPlayingSheet = view.findViewById(R.id.nowPlayingSheet)
        requireNotNull(nowPlayingSheet) { "nowPlayingSheet missing. Hãy dùng view_now_playing_sheet.xml như đã hướng dẫn." }
        nowPlayingBehavior = BottomSheetBehavior.from(nowPlayingSheet!!)
        nowPlayingBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        nowPlayingBehavior.isHideable = true
        nowPlayingBehavior.skipCollapsed = false
        nowPlayingBehavior.saveFlags = BottomSheetBehavior.SAVE_ALL
        nowPlayingBehavior.isDraggable = true

        // Ánh xạ control trong peek
        peekHost = nowPlayingSheet!!.findViewById(R.id.nowPlayingPeekHost)
        peekContainer = nowPlayingSheet!!.findViewById(R.id.nowPlayingPeek)
        peekPlay  = nowPlayingSheet!!.findViewById(R.id.peek_btnPlayPause)
        peekPrev  = nowPlayingSheet!!.findViewById(R.id.peek_btnPrev)
        peekNext  = nowPlayingSheet!!.findViewById(R.id.peek_btnNext)
        peekTitle = nowPlayingSheet!!.findViewById(R.id.peek_textTitle)
        peekSubtitle   = nowPlayingSheet!!.findViewById(R.id.peek_textSubtitle)
        peekArtwork   = nowPlayingSheet!!.findViewById(R.id.peek_imageArt)
        peekHandle = nowPlayingSheet!!.findViewById(R.id.nowPlayingHandle)
        peekBackdrop = nowPlayingSheet!!.findViewById(R.id.nowPlayingBackdrop)

        // Click peek: có thể mở thẳng Player
        peekContainer?.setOnClickListener { openPlayer() }

        // Điều khiển phát từ peek
        peekPlay?.setOnClickListener { musicService?.toggle() }
        peekPrev?.setOnClickListener { musicService?.previous() }
        peekNext?.setOnClickListener { musicService?.next() }

        // Callback kéo: nếu kéo >= 80% thì mở PlayerFragment
        nowPlayingBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(sheet: View, slideOffset: Float) {
                // slideOffset: [-1, 1]; với BottomSheet standard: 0≈collapsed, 1=expanded.
                val normalized = slideOffset.coerceIn(0f, 1f)
                updateSlideEffects(normalized)

                if (normalized >= 0.8f && !navigatedToPlayer) {
                    navigatedToPlayer = true
                    // Đưa về collapsed để không che animation điều hướng
                    nowPlayingBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    openPlayer()
                }
                // Cập padding cho content tương ứng peekHeight khi gần collapsed
                if (nowPlayingBehavior.state == BottomSheetBehavior.STATE_COLLAPSED || slideOffset in 0f..1f) {
                    setBottomInset(nowPlayingBehavior.peekHeight)
                }
            }
            override fun onStateChanged(sheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    setBottomInset(0)
                    updateSlideEffects(0f)
                }
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    setBottomInset(nowPlayingBehavior.peekHeight)
                    updateSlideEffects(0f)
                }
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    updateSlideEffects(1f)
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
            if (srv == null) {
                lastSong = null
                showEmptyPeek(resetArtwork = true)
                hidePeek()
            }
        }

        // Quan sát bài hiện tại: chỉ hiện khi có bài (user đã bấm nhạc)
        nowVM.currentSong.observe(viewLifecycleOwner) { song ->
            if (song == null) {
                if (lastSong == null) {
                    showEmptyPeek(resetArtwork = true)
                    hidePeek()
                } else {
                    showEmptyPeek(resetArtwork = false)
                    showPeek()
                }
            } else {
                lastSong = song
                applySongToPeek(song)
                nowPlayingSheet?.post {
                    val peekHeight = peekHost?.height ?: peekContainer?.height ?: 0
                    nowPlayingBehavior.peekHeight = if (peekHeight > 0) peekHeight else dp(88)
                    showPeek()
                }
            }
        }

        // Nút play/pause
        nowVM.isPlaying.observe(viewLifecycleOwner) { playing ->
            peekPlay?.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        }

        // Trạng thái ban đầu ẩn hoàn toàn
        hidePeek()
    }

    override fun onDestroyView() {
        peekBackdrop = null
        peekHandle = null
        peekHost = null
        peekContainer = null
        peekPlay = null
        peekPrev = null
        peekNext = null
        peekTitle = null
        peekSubtitle = null
        peekArtwork = null
        musicService = null
        _binding = null
        super.onDestroyView()
    }

    // Điều hướng Player
    private fun openPlayer() {
        // Điều hướng sang PlayerFragment nếu đang ở LibraryFragment
        findNavController().navigateFrom(
            R.id.libraryFragment,
            R.id.action_libraryFragment_to_playerFragment
        )
    }

    // Hiện peek: bottom sheet chuyển về collapsed (chỉ cao bằng phần peek)
    private fun showPeek() {
        if (nowPlayingBehavior.state == BottomSheetBehavior.STATE_HIDDEN ||
            nowPlayingBehavior.state == BottomSheetBehavior.STATE_DRAGGING) {
            nowPlayingBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            setBottomInset(nowPlayingBehavior.peekHeight)
            updateSlideEffects(0f)
        }
    }

    // Ẩn peek
    private fun hidePeek() {
        if (this::nowPlayingBehavior.isInitialized) {
            nowPlayingBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            setBottomInset(0)
            updateSlideEffects(0f)
        }
    }

    // Đệm nội dung phía dưới = chiều cao peek khi đang hiện
    private fun setBottomInset(pixels: Int) {
        val c = binding.contentContainer
        c.setPadding(c.paddingLeft, c.paddingTop, c.paddingRight, pixels)
    }

    private fun applySongToPeek(song: Song) {
        peekTitle?.text = song.title
        peekSubtitle?.text = song.artist ?: ""
        peekArtwork?.let { art ->
            Glide.with(art)
                .load(song.artworkUri ?: song.uri)
                .placeholder(R.drawable.ic_logo)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(art)
        }
    }

    private fun showEmptyPeek(resetArtwork: Boolean) {
        if (resetArtwork) {
            peekArtwork?.setImageResource(R.drawable.ic_logo)
            lastSong = null
        }
        peekTitle?.text = lastSong?.title ?: getString(R.string.now_playing_peek_empty_title)
        peekSubtitle?.text = lastSong?.artist ?: getString(R.string.now_playing_peek_empty_subtitle)
    }

    private fun updateSlideEffects(offset: Float) {
        val clamped = offset.coerceIn(0f, 1f)
        peekBackdrop?.alpha = clamped
        peekHandle?.alpha = 0.15f + (0.85f * clamped)
        peekContainer?.let { peek ->
            if (peek.width > 0 && peek.height > 0) {
                peek.pivotX = peek.width / 2f
                peek.pivotY = peek.height.toFloat()
                peek.scaleX = 1f + 0.05f * clamped
                peek.scaleY = 1f + 0.08f * clamped
            }
        }
    }

    private fun dp(v: Int): Int {
        val d = resources.displayMetrics.density
        return (v * d + 0.5f).toInt()
    }
}
