package com.example.musicapp.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.data.model.Song
import com.example.musicapp.databinding.ItemSongBinding
import java.util.concurrent.TimeUnit

class SongAdapter(
    private val onClick: (Song, Int) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }

    inner class SongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Song) {
            val fallback = binding.root.context.getString(R.string.artist_placeholder)
            binding.textTitle.text = item.title
            binding.textSubtitle.text = buildSubtitle(item, fallback)
            binding.root.setOnClickListener { onClick(item, bindingAdapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun buildSubtitle(item: Song, fallback: String): String {
        val parts = mutableListOf<String>()
        if (!item.artist.isNullOrBlank()) {
            parts += item.artist
        }
        formatDuration(item.durationMs)?.let { parts += it }
        return if (parts.isEmpty()) fallback else parts.joinToString(" • ")
    }

    private fun formatDuration(durationMs: Long): String? {
        if (durationMs <= 0) return null
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
