package com.example.musicapp.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import com.example.musicapp.R
import com.example.musicapp.databinding.ActivityMainBinding
import com.example.musicapp.player.MusicService
import com.example.musicapp.ui.viewmodel.ServiceConnectionViewModel

// MainActivity - khởi động/bind service, phát service qua ViewModel, edge-to-edge
class MainActivity : AppCompatActivity() {

    // binding - truy cập view
    private lateinit var binding: ActivityMainBinding

    // musicService - giữ cục bộ (Fragment sẽ lấy qua ViewModel)
    var musicService: MusicService? = null
        private set

    // serviceVM - chia sẻ MusicService
    private val serviceVM: ServiceConnectionViewModel by viewModels()

    // isBound - trạng thái bind
    private var isBound = false

    // serviceConnection - nhận binder và phát cho ViewModel
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binderFromSystem: IBinder?) {
            val localBinder = binderFromSystem as MusicService.LocalBinder
            musicService = localBinder.getService()
            isBound = true
            serviceVM.set(musicService) // phát service
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
            serviceVM.set(null)
        }
    }

    // permissionLauncher - xin quyền runtime
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

    // onCreate - cấu hình & bind
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // edge-to-edge - nội dung tràn hệ thống
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHost) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom); insets
        }

        // navController - nếu cần
        (supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment).navController

        // xin quyền
        requestRuntimePermissions()

        // startForegroundService - khởi động service (không tự phát nhạc)
        val intent = Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)

        // bindService - kết nối
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // xin quyền
    private fun requestRuntimePermissions() {
        val needs = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            needs += android.Manifest.permission.READ_MEDIA_AUDIO
            needs += android.Manifest.permission.POST_NOTIFICATIONS
        } else {
            needs += android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (needs.isNotEmpty()) permissionLauncher.launch(needs.toTypedArray())
    }

    // onDestroy - unbind + clear
    override fun onDestroy() {
        if (isBound) unbindService(serviceConnection)
        serviceVM.set(null)
        super.onDestroy()
    }
}
