
package com.example.musicapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.musicapp.player.MusicService

class ServiceConnectionViewModel : ViewModel() {
    private val _service = MutableLiveData<MusicService?>()
    val service: LiveData<MusicService?> = _service
    fun set(service: MusicService?) { _service.value = service }
}
