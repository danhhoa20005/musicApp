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

// VM đồng bộ trạng thái phát để UI observe
class NowPlayingViewModel : ViewModel() {

    // bài hiện tại
    private val _currentSong = MutableLiveData<Song?>()
    val currentSong: LiveData<Song?> = _currentSong

    // đang phát?
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    // tiến độ / tổng thời gian (ms)
    private val _positionMs = MutableLiveData(0)
    val positionMs: LiveData<Int> = _positionMs
    private val _durationMs = MutableLiveData(0)
    val durationMs: LiveData<Int> = _durationMs

    // shuffle / repeat
    private val _shuffleOn = MutableLiveData(false)
    val shuffleOn: LiveData<Boolean> = _shuffleOn
    private val _repeatMode = MutableLiveData(Player.REPEAT_MODE_OFF)
    val repeatMode: LiveData<Int> = _repeatMode

    // service hiện tại + job ticker
    private var service: MusicService? = null
    private var tickerJob: Job? = null

    // gắn Service và cập nhật state
    fun attachService(newService: MusicService?) {
        service = newService
        tickerJob?.cancel()
        if (newService == null) return

        // init lần đầu
        _currentSong.value = newService.currentSong()
        _isPlaying.value = newService.isPlaying()
        _positionMs.value = newService.position()
        _durationMs.value = newService.duration()
        _shuffleOn.value = newService.isShuffleOn()
        _repeatMode.value = newService.getRepeatMode()

        // lắng nghe player
        newService.addPlayerListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.postValue(isPlaying) // play/pause
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                _currentSong.postValue(newService.currentSong()) // đổi bài
                _durationMs.postValue(newService.duration())
                _positionMs.postValue(newService.position())
            }
            override fun onEvents(player: Player, events: Player.Events) {
                _shuffleOn.postValue(newService.isShuffleOn())   // shuffle
                _repeatMode.postValue(newService.getRepeatMode())// repeat
            }
        })

        // ticker 500ms đẩy tiến độ/duration
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
