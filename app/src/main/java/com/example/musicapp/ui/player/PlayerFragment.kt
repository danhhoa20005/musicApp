package com.example.musicapp.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.fragment.findNavController
import com.example.musicapp.R
import com.example.musicapp.databinding.FragmentPlayerBinding
import com.example.musicapp.ui.main.MainActivity

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            val s = (requireActivity() as MainActivity).musicService
            if (s != null) {
                val d = s.duration()
                val p = s.position()
                if (d > 0) {
                    binding.seekBar.max = d
                    binding.seekBar.progress = p
                    binding.textElapsed.text = format(p)
                    binding.textDuration.text = format(d)
                } else {
                    binding.seekBar.progress = 0
                    binding.textElapsed.text = "0:00"
                    binding.textDuration.text = "0:00"
                }
            }
            handler.postDelayed(this, 500L)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseIcon()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateTitleArtist()
        }
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                val s = (requireActivity() as MainActivity).musicService
                val d = s?.duration() ?: 0
                val p = s?.position() ?: 0
                if (d > 0) {
                    binding.seekBar.max = d
                    binding.seekBar.progress = p
                    binding.textElapsed.text = format(p)
                    binding.textDuration.text = format(d)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Vuốt/nút Back và nút mũi tên nhỏ → quay lại Library
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { findNavController().navigateUp() } }
        )
        binding.buttonCollapse.setOnClickListener { findNavController().navigateUp() }
        binding.buttonMore.setOnClickListener { /* TODO: mở menu tùy chọn nếu cần */ }

        val s = (requireActivity() as MainActivity).musicService

        // Play/Pause
        binding.buttonPlayPause.setOnClickListener {
            s?.toggle()
            updatePlayPauseIcon()
        }

        // Next/Prev
        binding.buttonNext.setOnClickListener { s?.next() }
        binding.buttonPrev.setOnClickListener { s?.previous() }

        // Shuffle/Repeat (cần thêm 2 hàm trong MusicService, xem bên dưới)
        binding.buttonShuffle.setOnClickListener {
            s?.toggleShuffle()
            updateShuffleIcon()
        }
        binding.buttonRepeat.setOnClickListener {
            s?.cycleRepeat()
            updateRepeatIcon()
        }

        // SeekBar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) (requireActivity() as MainActivity).musicService?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Cập nhật ban đầu
        binding.textNowPlaying.setText(R.string.now_playing)
        updateTitleArtist()
        updatePlayPauseIcon()
        updateShuffleIcon()
        updateRepeatIcon()
        handler.post(ticker)
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).musicService?.addPlayerListener(playerListener)
    }

    override fun onStop() {
        (requireActivity() as MainActivity).musicService?.removePlayerListener(playerListener)
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(ticker)
        _binding = null
    }

    private fun updateTitleArtist() {
        val s = (requireActivity() as MainActivity).musicService
        val cur = s?.currentSong()
        binding.textTitle.text = cur?.title ?: "Unknown"
        binding.textArtist.text = cur?.artist ?: "Unknown"
        // Ảnh bìa: nếu bạn có bitmap/uri thì nạp vào binding.imageCover tại đây
    }

    private fun updatePlayPauseIcon() {
        val isPlaying = (requireActivity() as MainActivity).musicService?.isPlaying() == true
        binding.buttonPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun updateShuffleIcon() {
        val enabled = (requireActivity() as MainActivity).musicService?.isShuffleOn() == true
        binding.buttonShuffle.imageAlpha = if (enabled) 255 else 120
    }

    private fun updateRepeatIcon() {
        val mode = (requireActivity() as MainActivity).musicService?.getRepeatMode() ?: Player.REPEAT_MODE_OFF
        // Đơn giản: sáng khi != OFF
        binding.buttonRepeat.imageAlpha = if (mode == Player.REPEAT_MODE_OFF) 120 else 255
    }

    private fun format(ms: Int): String {
        val s = ms / 1000
        val m = s / 60
        val r = s % 60
        return "%d:%02d".format(m, r)
    }
}
