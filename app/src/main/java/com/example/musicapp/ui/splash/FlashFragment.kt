package com.example.musicapp.ui.flash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.musicapp.R
import com.example.musicapp.util.navigateFrom
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// FlashFragment - màn splash đơn giản trước khi vào thư viện
class FlashFragment : Fragment() {

    private var navigated = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Giữ splash 800ms rồi vào LibraryFragment (điểm bắt đầu app)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(800)
            if (!navigated && isAdded) {
                navigated = true
                findNavController().navigateFrom(
                    R.id.flashFragment,
                    R.id.action_flashFragment_to_libraryFragment
                )
            }
        }

        // Không cho back thoát app khi đang ở splash (tuỳ chọn)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) { /* bỏ trống để chặn back trong 800ms */ }
    }
}
