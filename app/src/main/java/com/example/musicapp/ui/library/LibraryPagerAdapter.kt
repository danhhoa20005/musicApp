package com.example.musicapp.ui.library

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter


class LibraryPagerAdapter(parent: Fragment) : FragmentStateAdapter(parent) {
    override fun getItemCount(): Int = 1
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AllSongsFragment()
            else -> AllSongsFragment()
        }
    }
}
