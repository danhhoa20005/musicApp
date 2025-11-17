package com.example.musicapp.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.Player
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.musicapp.R
import com.example.musicapp.data.artist.ArtistCovers
import com.example.musicapp.data.song.SongRepository
import com.example.musicapp.databinding.FragmentPlayerBinding
import com.example.musicapp.ui.util.CoverResolver
import com.example.musicapp.ui.viewmodel.NowPlayingViewModel
import com.example.musicapp.ui.viewmodel.ServiceConnectionViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.abs

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val serviceVM: ServiceConnectionViewModel by activityViewModels()
    private val nowVM: NowPlayingViewModel by activityViewModels()

    private var currentFavorite = false   // trạng thái yêu thích hiện tại

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sheet = requireActivity().findViewById<View>(R.id.nowPlayingSheet)
        val behavior = BottomSheetBehavior.from(sheet)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val state = behavior.state
                    if (state == BottomSheetBehavior.STATE_EXPANDED) {
                        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        binding.buttonBack.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        // ❤️ yêu thích
        binding.buttonLove.setOnClickListener {
            val song = nowVM.currentSong.value ?: return@setOnClickListener
            val newFav = !currentFavorite
            currentFavorite = newFav

            val appCtx = requireContext().applicationContext
            SongRepository.updateFavorite(appCtx, song.id, newFav)

            updateLoveIcon(newFav)
        }

        // Điều khiển phát
        binding.buttonPlayPause.setOnClickListener { serviceVM.service.value?.toggle() }
        binding.buttonNext.setOnClickListener { serviceVM.service.value?.next() }
        binding.buttonPrev.setOnClickListener { serviceVM.service.value?.previous() }
        binding.buttonShuffle.setOnClickListener { serviceVM.service.value?.toggleShuffle() }
        binding.buttonRepeat.setOnClickListener { serviceVM.service.value?.toggleRepeatOne() }

        // SeekBar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) serviceVM.service.value?.seekTo(p)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        attachSwipeOnCover()

        // Bài hiện tại
        nowVM.currentSong.observe(viewLifecycleOwner) { s ->
            binding.textTitle.text = s?.title ?: getString(R.string.song_title_placeholder)
            binding.textArtist.text = s?.artist ?: getString(R.string.artist_placeholder)

            val art = CoverResolver.resolveArtwork(s, ArtistCovers.defaultCover)
            Glide.with(binding.imageCover)
                .load(art)
                .placeholder(R.drawable.ic_logo)
                .error(R.drawable.ic_logo)
                .fallback(R.drawable.ic_logo)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(binding.imageCover)

            binding.textTitle.isSelected = true
            binding.textArtist.isSelected = true

            // đồng bộ icon love với song.isFavorite
            currentFavorite = s?.isFavorite == true
            updateLoveIcon(currentFavorite)
        }

        // Trạng thái phát
        nowVM.isPlaying.observe(viewLifecycleOwner) { playing ->
            binding.buttonPlayPause.setImageResource(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        // Shuffle
        nowVM.shuffleOn.observe(viewLifecycleOwner) { on ->
            binding.buttonShuffle.isSelected = on
            binding.buttonShuffle.imageAlpha = if (on) 255 else 120
        }

        // Repeat
        nowVM.repeatMode.observe(viewLifecycleOwner) { mode ->
            val isOne = mode == Player.REPEAT_MODE_ONE
            binding.buttonRepeat.setImageResource(
                if (isOne) R.drawable.ic_repeat_one else R.drawable.ic_repeat
            )
            binding.buttonRepeat.imageAlpha = if (isOne) 255 else 120
        }

        // Thời gian
        nowVM.durationMs.observe(viewLifecycleOwner) { d ->
            val dur = d.coerceAtLeast(0)
            binding.seekBar.max = dur
            binding.textDuration.text = format(dur)
        }
        nowVM.positionMs.observe(viewLifecycleOwner) { p ->
            if (binding.seekBar.max > 0) {
                binding.seekBar.progress = p
                binding.textElapsed.text = format(p)
            } else {
                binding.textElapsed.text = "0:00"
            }
        }

        serviceVM.service.observe(viewLifecycleOwner) { srv ->
            nowVM.attachService(srv)
            setControlsEnabled(srv != null)
        }
    }

    private fun setControlsEnabled(enabled: Boolean) {
        binding.buttonPlayPause.isEnabled = enabled
        binding.buttonNext.isEnabled = enabled
        binding.buttonPrev.isEnabled = enabled
        binding.buttonShuffle.isEnabled = enabled
        binding.buttonRepeat.isEnabled = enabled
        binding.seekBar.isEnabled = enabled
        val alpha = if (enabled) 1f else 0.5f
        binding.buttonPlayPause.alpha = alpha
        binding.buttonNext.alpha = alpha
        binding.buttonPrev.alpha = alpha
        binding.buttonShuffle.alpha = alpha
        binding.buttonRepeat.alpha = alpha
        binding.seekBar.alpha = alpha
    }

    private fun attachSwipeOnCover() {
        var downX = 0f
        val threshold = 80f

        binding.imageCover.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = ev.x - downX
                    if (abs(dx) > threshold) {
                        if (dx < 0) serviceVM.service.value?.next()
                        else serviceVM.service.value?.previous()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun updateLoveIcon(fav: Boolean) {
        binding.buttonLove.setImageResource(
            if (fav) R.drawable.ic_love2 else R.drawable.ic_love1
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun format(ms: Int): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val m = s / 60
        val r = s % 60
        return "%d:%02d".format(m, r)
    }
}
