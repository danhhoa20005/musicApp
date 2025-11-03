package com.example.musicapp.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import com.example.musicapp.databinding.FragmentPlayerBinding
import com.example.musicapp.ui.main.MainActivity

/**
 * Màn Player: điều khiển play/pause/prev/next, seek, đồng bộ thời gian.
 * Sử dụng layout fragment_player.xml bạn đã có.
 */
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            val service = (requireActivity() as MainActivity).musicService
            if (service != null) {
                val d = service.duration()
                val p = service.position()
                if (d > 0) {
                    binding.seekBar.max = d
                    binding.seekBar.progress = p
                    binding.textElapsed.text = format(p)
                    binding.textDuration.text = format(d)
                }
                val current = service.currentSong()
                binding.textTitle.text = current?.title ?: "Unknown"
                binding.textArtist.text = current?.artist ?: "Unknown"
            }
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val service = (requireActivity() as MainActivity).musicService

        binding.buttonPlayPause.setOnClickListener {
            service?.toggle()
            updatePlayPauseText()
        }
        binding.buttonNext.setOnClickListener {
            service?.next()
        }
        binding.buttonPrev.setOnClickListener {
            service?.previous()
        }
        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) service?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        updatePlayPauseText()
        handler.post(ticker)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(ticker)
        _binding = null
    }

    private fun updatePlayPauseText() {
        val isPlaying = (requireActivity() as MainActivity).musicService?.isPlaying() == true
        binding.buttonPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun format(ms: Int): String {
        val s = ms / 1000
        val m = s / 60
        val r = s % 60
        return "%d:%02d".format(m, r)
    }
}
