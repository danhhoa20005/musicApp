package com.example.musicapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.musicapp.data.model.Song
import com.example.musicapp.player.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// NowPlayingViewModel - đồng bộ trạng thái phát từ MusicService
class NowPlayingViewModel : ViewModel() {

    // currentSong - bài hát hiện tại
    private val _currentSong = MutableLiveData<Song?>()
    val currentSong: LiveData<Song?> = _currentSong

    // isPlaying - đang phát
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // positionMs/durationMs - tiến độ/tổng thời gian
    private val _positionMs = MutableLiveData(0)
    val positionMs: LiveData<Int> = _positionMs
    private val _durationMs = MutableLiveData(0)
    val durationMs: LiveData<Int> = _durationMs

    // shuffleOn/repeatMode - trộn/lặp
    private val _shuffleOn = MutableLiveData(false)
    val shuffleOn: LiveData<Boolean> = _shuffleOn
    private val _repeatMode = MutableLiveData(Player.REPEAT_MODE_OFF)
    val repeatMode: LiveData<Int> = _repeatMode

    // service & ticker
    private var service: MusicService? = null
    private var tickerJob: Job? = null

    // attachService - đăng ký lắng nghe + khởi chạy vòng cập nhật tiến độ
    fun attachService(newService: MusicService?) {
        service = newService
        tickerJob?.cancel()
        if (newService == null) return

        // cập nhật lần đầu
        _currentSong.value = newService.currentSong()
        _isPlaying.value = newService.isPlaying()
        _positionMs.value = newService.position()
        _durationMs.value = newService.duration()
        _shuffleOn.value = newService.isShuffleOn()
        _repeatMode.value = newService.getRepeatMode()

        // lắng nghe sự kiện player (nhẹ)
        newService.addPlayerListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.postValue(isPlaying)
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                _currentSong.postValue(newService.currentSong())
                _durationMs.postValue(newService.duration())
                _positionMs.postValue(newService.position())
            }
            override fun onEvents(player: Player, events: Player.Events) {
                _shuffleOn.postValue(newService.isShuffleOn())
                _repeatMode.postValue(newService.getRepeatMode())
            }
        })

        // ticker - 500ms/lần: cập nhật tiến độ/duration mượt
        tickerJob = viewModelScope.launch {
            while (true) {
                val s = service ?: break
                val dur = s.duration()
                val pos = s.position()
                if (dur >= 0 && pos >= 0) {
                    _durationMs.postValue(dur)
                    _positionMs.postValue(pos)
                }
                delay(500)
            }
        }
    }
}
