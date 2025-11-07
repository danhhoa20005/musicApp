package com.example.musicapp.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.Player
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.musicapp.R
import com.example.musicapp.databinding.FragmentPlayerBinding
import com.example.musicapp.ui.viewmodel.NowPlayingViewModel
import com.example.musicapp.ui.viewmodel.ServiceConnectionViewModel

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val serviceVM: ServiceConnectionViewModel by activityViewModels()
    private val nowVM: NowPlayingViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { findNavController().navigateUp() }
            }
        )
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonBackRight.setOnClickListener { findNavController().navigateUp() }
        // điều khiển
        binding.buttonPlayPause.setOnClickListener { serviceVM.service.value?.toggle() }
        binding.buttonNext.setOnClickListener { serviceVM.service.value?.next() }
        binding.buttonPrev.setOnClickListener { serviceVM.service.value?.previous() }
        binding.buttonShuffle.setOnClickListener { serviceVM.service.value?.toggleShuffle() }
        binding.buttonRepeat.setOnClickListener { serviceVM.service.value?.cycleRepeat() }

        // seekbar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { if (fromUser) serviceVM.service.value?.seekTo(p) }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // bài hiện tại
        nowVM.currentSong.observe(viewLifecycleOwner) { song ->
            binding.textTitle.text = song?.title ?: getString(R.string.song_title_placeholder)
            binding.textArtist.text = song?.artist ?: getString(R.string.artist_placeholder)
            Glide.with(binding.imageCover)
                .load(song?.artworkUri ?: song?.uri)
                .placeholder(R.drawable.ic_logo)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(binding.imageCover)
        }

        // trạng thái phát
        nowVM.isPlaying.observe(viewLifecycleOwner) { playing ->
            binding.buttonPlayPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        }

        // shuffle/repeat
        nowVM.shuffleOn.observe(viewLifecycleOwner) { on ->
            binding.buttonShuffle.imageAlpha = if (on) 255 else 120
        }
        nowVM.repeatMode.observe(viewLifecycleOwner) { mode ->
            binding.buttonRepeat.imageAlpha = if (mode == Player.REPEAT_MODE_OFF) 120 else 255
        }

        // tiến độ
        nowVM.durationMs.observe(viewLifecycleOwner) { dur ->
            if (dur > 0) {
                binding.seekBar.max = dur
                binding.textDuration.text = format(dur)
            } else {
                binding.seekBar.max = 0
                binding.textDuration.text = "0:00"
            }
        }
        nowVM.positionMs.observe(viewLifecycleOwner) { pos ->
            if (binding.seekBar.max > 0) {
                binding.seekBar.progress = pos
                binding.textElapsed.text = format(pos)
            } else {
                binding.textElapsed.text = "0:00"
            }
        }

        // gắn service khi có
        serviceVM.service.observe(viewLifecycleOwner) { srv ->
            nowVM.attachService(srv)
        }

        // nhãn
        binding.textHeaderTitle.setText(R.string.now_playing)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun format(ms: Int): String {
        val s = ms / 1000
        val m = s / 60
        val r = s % 60
        return "%d:%02d".format(m, r)
    }
}
