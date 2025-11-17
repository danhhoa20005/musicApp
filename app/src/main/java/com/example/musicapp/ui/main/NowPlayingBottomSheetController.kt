package com.example.musicapp.ui.main

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.musicapp.R
import com.example.musicapp.data.artist.ArtistCovers
import com.example.musicapp.databinding.ActivityMainBinding
import com.example.musicapp.ui.player.PlayerFragment
import com.example.musicapp.ui.util.CoverResolver
import com.example.musicapp.ui.viewmodel.NowPlayingViewModel
import com.example.musicapp.ui.viewmodel.ServiceConnectionViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior

// NowPlayingBottomSheetController - điều khiển bottom sheet player (mini + full) trong MainActivity
class NowPlayingBottomSheetController(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val nowVM: NowPlayingViewModel,
    private val serviceVM: ServiceConnectionViewModel
) {
    // bottomSheetBehavior - behavior điều khiển kéo/ẩn/hiện bottom sheet
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // init - khởi tạo bottom sheet và đăng ký observer với NowPlayingViewModel + Service
    fun init() {
        setupBottomSheet()
        observeNowPlaying()
        observeIsPlaying()
        observeService()
    }

    // expand - cho phép Activity/Fragment yêu cầu mở full bottom sheet (trạng thái EXPANDED)
    fun expand() {
        binding.nowPlayingSheet.post {
            if (::bottomSheetBehavior.isInitialized) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    // setupBottomSheet - cấu hình behavior, trạng thái ban đầu, mini controls, và PlayerFragment full
    private fun setupBottomSheet() {
        val sheet = binding.nowPlayingSheet              // root của bottom sheet (LinearLayout)
        val peekBinding = binding.peek                   // view mini player (include)
        val peekRoot = peekBinding.root                  // root view của mini player

        // Lấy behavior gắn với nowPlayingSheet
        bottomSheetBehavior = BottomSheetBehavior.from(sheet)
        bottomSheetBehavior.setDraggable(true)           // cho phép kéo
        bottomSheetBehavior.setFitToContents(true)       // fit theo nội dung thay vì full height

        // Thiết lập chiều cao peek (mini player) từ dimens
        try {
            bottomSheetBehavior.setPeekHeight(
                activity.resources.getDimensionPixelSize(R.dimen.peek_68dp),
                true
            )
        } catch (_: Throwable) {
        }

        bottomSheetBehavior.isHideable = true            // cho phép ẩn hoàn toàn
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN   // trạng thái ban đầu: ẩn

        // Ẩn cả sheet và mini player khi chưa có bài
        sheet.visibility = View.GONE
        peekRoot.visibility = View.GONE
        peekRoot.alpha = 0f

        // BottomSheetCallback - xử lý khi kéo (onSlide) và khi đổi trạng thái (onStateChanged)
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {

            // onSlide - khi kéo lên/xuống: chỉnh alpha mini player (mờ dần khi full mở)
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val off = slideOffset.coerceIn(0f, 1f)
                // slideOffset gần 0 → mini player trong, gần 1 → mini player mờ dần
                peekRoot.alpha = 1f - off
            }

            // onStateChanged - phản ứng với các trạng thái EXPANDED/COLLAPSED/HIDDEN
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        // full player mở → ẩn mini player
                        peekRoot.visibility = View.GONE
                        peekRoot.alpha = 0f
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        // thu về mini → hiển thị mini player
                        peekRoot.visibility = View.VISIBLE
                        peekRoot.alpha = 1f
                    }

                    BottomSheetBehavior.STATE_HIDDEN -> {
                        // ẩn hoàn toàn → không hiện gì
                        sheet.visibility = View.GONE
                        peekRoot.visibility = View.GONE
                        peekRoot.alpha = 0f
                    }

                    else -> Unit
                }
            }
        })

        // Click vào sheet: nếu đang collapsed thì mở full, nếu đang full thì thu về mini
        sheet.setOnClickListener {
            bottomSheetBehavior.state =
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
                    BottomSheetBehavior.STATE_EXPANDED
                else
                    BottomSheetBehavior.STATE_COLLAPSED
        }

        // Nhét PlayerFragment vào vùng playerSheetContainer để hiển thị full player
        if (activity.supportFragmentManager.findFragmentById(R.id.playerSheetContainer) == null) {
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.playerSheetContainer, PlayerFragment())
                .commit()
        }

        // Nút prev mini player - gọi previous() trong MusicService
        peekBinding.btnPrev.setOnClickListener {
            serviceVM.service.value?.previous()
            updatePrevNextEnabled()
        }

        // Nút play/pause mini - gọi toggle() trong MusicService và cập nhật icon tạm thời
        peekBinding.buttonPlayPause.setOnClickListener {
            serviceVM.service.value?.toggle()
            updatePlayPauseIcon(!(nowVM.isPlaying.value ?: false))
        }

        // Nút next mini player - gọi next() trong MusicService
        peekBinding.btnNext.setOnClickListener {
            serviceVM.service.value?.next()
            updatePrevNextEnabled()
        }
    }

    // observeNowPlaying - quan sát bài hát hiện tại để cập nhật UI mini + sheet visibility
    private fun observeNowPlaying() {
        val sheet = binding.nowPlayingSheet
        val peekBinding = binding.peek
        val peekRoot = peekBinding.root

        nowVM.currentSong.observe(activity) { s ->
            val sheetVisible = s != null
            // có bài → sheet hiện; không có bài → sheet ẩn
            sheet.visibility = if (sheetVisible) View.VISIBLE else View.GONE

            if (!sheetVisible) {
                // không có bài: ẩn luôn bottom sheet và mini
                runCatching { bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN }
                peekRoot.visibility = View.GONE
                peekRoot.alpha = 0f
            } else {
                // lần đầu có bài mà sheet đang HIDDEN → chuyển sang COLLAPSED (mini player)
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    peekRoot.visibility = View.VISIBLE
                    peekRoot.alpha = 1f
                }

                // Cập nhật text title/artist cho mini player
                peekBinding.nowPlayingTitle.text =
                    s?.title ?: activity.getString(R.string.song_title_placeholder)
                peekBinding.nowPlayingArtist.text =
                    s?.artist ?: activity.getString(R.string.artist_placeholder)

                // Cập nhật ảnh cover cho mini player
                val art = CoverResolver.resolveArtwork(s!!, ArtistCovers.defaultCover)
                Glide.with(peekBinding.nowPlayingCover)
                    .load(art)
                    .placeholder(R.drawable.ic_logo)
                    .error(R.drawable.ic_logo)
                    .fallback(R.drawable.ic_logo)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(peekBinding.nowPlayingCover)

                // cập nhật trạng thái enabled của prev/next dựa trên Service
                updatePrevNextEnabled()
            }
        }
    }

    // observeIsPlaying - quan sát isPlaying để đổi icon play/pause cho mini player
    private fun observeIsPlaying() {
        nowVM.isPlaying.observe(activity) { playing ->
            updatePlayPauseIcon(playing)
        }
    }

    // observeService - khi Service thay đổi (bind/unbind) thì cập nhật lại nút prev/next + icon
    private fun observeService() {
        serviceVM.service.observe(activity) {
            updatePlayPauseIcon(nowVM.isPlaying.value == true)
            updatePrevNextEnabled()
        }
    }

    // updatePlayPauseIcon - đổi icon mini player theo trạng thái playing
    private fun updatePlayPauseIcon(playing: Boolean) {
        val peek = binding.peek
        peek.buttonPlayPause.setImageResource(
            if (playing) R.drawable.ic_pause else R.drawable.ic_play
        )
        peek.buttonPlayPause.contentDescription =
            activity.getString(if (playing) R.string.pause else R.string.play)
    }

    // updatePrevNextEnabled - enable/disable nút prev/next và chỉnh alpha cho mini player
    private fun updatePrevNextEnabled() {
        val peek = binding.peek
        val srv = serviceVM.service.value

        // hasPrev/hasNext có try-catch để tránh crash nếu Service chưa sẵn sàng
        val hasPrev = try {
            srv?.hasPrevious() ?: true
        } catch (_: Throwable) {
            true
        }
        val hasNext = try {
            srv?.hasNext() ?: true
        } catch (_: Throwable) {
            true
        }

        // enable/disable nút và chỉnh độ mờ để phản hồi cho user
        peek.btnPrev.isEnabled = hasPrev
        peek.btnNext.isEnabled = hasNext
        peek.btnPrev.alpha = if (hasPrev) 1f else 0.4f
        peek.btnNext.alpha = if (hasNext) 1f else 0.4f
    }
}
