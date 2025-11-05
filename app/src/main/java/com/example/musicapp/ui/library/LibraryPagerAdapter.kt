package com.example.musicapp.ui.library

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class LibraryPagerAdapter(
    parent: Fragment,
    private val pages: List<LibraryPage>
) : FragmentStateAdapter(parent) {

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return SongListFragment.newInstance(pages[position].filter)
    }
}
