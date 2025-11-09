package com.example.musicapp.util

import androidx.annotation.IdRes
import androidx.navigation.NavController

// NavControllerExtensions - hàm mở rộng điều hướng an toàn tránh crash lặp điều hướng
fun NavController.navigateFrom(@IdRes expectedCurrentId: Int, @IdRes actionId: Int) {
    val current = currentDestination ?: return
    if (current.id == expectedCurrentId) {
        navigate(actionId)
    }
}
