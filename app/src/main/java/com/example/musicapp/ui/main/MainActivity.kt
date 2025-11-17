package com.example.musicapp.ui.main

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import com.example.musicapp.R
import com.example.musicapp.databinding.ActivityMainBinding
import com.example.musicapp.player.MusicService
import com.example.musicapp.ui.viewmodel.NowPlayingViewModel
import com.example.musicapp.ui.viewmodel.ServiceConnectionViewModel

// MainActivity - màn chính, chứa navHost và bottom sheet player
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var musicService: MusicService? = null
        private set

    private val serviceVM: ServiceConnectionViewModel by viewModels()
    private val nowVM: NowPlayingViewModel by viewModels()

    private var isBound = false

    private lateinit var bottomSheetController: NowPlayingBottomSheetController

    // serviceConnection - nhận callback khi bind/unbind MusicService
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binderFromSystem: IBinder?) {
            val localBinder = binderFromSystem as MusicService.LocalBinder
            musicService = localBinder.getService()
            isBound = true

            serviceVM.set(musicService)
            nowVM.attachService(musicService)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
            serviceVM.set(null)
            nowVM.attachService(null)
        }
    }

    // permissionLauncher - xin quyền truy cập nhạc và gửi thông báo
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

    // onCreate - khởi tạo giao diện, edge-to-edge, bind service, setup bottom sheet
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bật edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Chỉ áp insets cho navHost: chừa status bar, KHÔNG chừa navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHost) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,   // chừa chỗ cho status bar
                bottom = 0              // không cộng bottom → không bị đẩy lên vì nav bar
            )
            insets
        }

        // Lấy NavController từ navHost
        (supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment).navController

        requestRuntimePermissions()

        val intent = Intent(this, MusicService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        bottomSheetController =
            NowPlayingBottomSheetController(this, binding, nowVM, serviceVM)
        bottomSheetController.init()
    }

    // requestRuntimePermissions - gom và xin các quyền cần thiết
    private fun requestRuntimePermissions() {
        val needs = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            needs += android.Manifest.permission.READ_MEDIA_AUDIO
            needs += android.Manifest.permission.POST_NOTIFICATIONS
        } else {
            needs += android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (needs.isNotEmpty()) permissionLauncher.launch(needs.toTypedArray())
    }

    // expandNowPlaying - yêu cầu bottom sheet mở full player
    fun expandNowPlaying() {
        bottomSheetController.expand()
    }

    // onDestroy - unbind service và dọn dẹp trước khi hủy activity
    override fun onDestroy() {
        if (isBound) unbindService(serviceConnection)
        serviceVM.set(null)
        nowVM.attachService(null)
        super.onDestroy()
    }
}
