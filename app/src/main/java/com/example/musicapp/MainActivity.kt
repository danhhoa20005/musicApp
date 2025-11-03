package com.example.musicapp.ui.main

import android.content.*
import android.os.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.musicapp.R
import com.example.musicapp.databinding.ActivityMainBinding
import com.example.musicapp.player.MusicService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var musicService: MusicService? = null
        private set

    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            bound = false
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        requestRuntimePermissions()

        // Khởi động dịch vụ phát nhạc ở chế độ foreground
        val intent = Intent(this, MusicService::class.java)
        ContextCompat.startForegroundService(this, intent)

        // Bind để các Fragment có thể gọi API của service qua Activity
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun requestRuntimePermissions() {
        val needs = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            needs += android.Manifest.permission.READ_MEDIA_AUDIO
            needs += android.Manifest.permission.POST_NOTIFICATIONS
        } else {
            needs += android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(needs.toTypedArray())
    }

    override fun onDestroy() {
        if (bound) unbindService(connection)
        super.onDestroy()
    }
}
