package com.example.musicapp.ui.library

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.musicapp.ui.song.SongListFragment

// LibraryPagerAdapter - cung cấp Fragment cho từng tab theo danh sách trang
class LibraryPagerAdapter(
    parent: Fragment,
    private val pages: List<LibraryPage>
) : FragmentStateAdapter(parent) {

    // getItemCount - số lượng tab
    override fun getItemCount(): Int = pages.size

    // createFragment - tạo Fragment danh sách bài hát theo bộ lọc của tab
    override fun createFragment(position: Int): Fragment {
        return SongListFragment.newInstance(pages[position].filter)
    }
}
