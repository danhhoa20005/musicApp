package com.example.musicapp.ui.song

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.musicapp.R
import com.example.musicapp.data.artist.ArtistCovers
import com.example.musicapp.data.model.Song
import com.example.musicapp.databinding.ItemSongBinding
import com.example.musicapp.ui.util.CoverResolver
import java.util.concurrent.TimeUnit

class SongAdapter(
    private val onClick: (Song, Int) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }

    init {
        // ổn định itemId để hạn chế nhấp nháy ảnh khi update
        setHasStableIds(true)
    }
    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    inner class SongViewHolder(val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song) {
            val ctx = binding.root.context
            val artistFallback = ctx.getString(R.string.artist_placeholder)

            // tiêu đề + nghệ sĩ
            binding.textTitle.text = song.title
            binding.textSubtitle.text = buildSubtitle(song, artistFallback)

            // thời lượng m:ss (ẩn nếu không có)
            val dur = formatDuration(song.durationMs)
            binding.textDuration.text = dur ?: ""
            binding.textDuration.isVisible = !dur.isNullOrEmpty()

            // Ảnh bìa: nghệ sĩ -> artworkUri -> default
            val art = CoverResolver.resolveArtwork(song, ArtistCovers.defaultCover)
            Glide.with(ctx)
                .load(art)
                .placeholder(R.drawable.ic_logo)
                .error(R.drawable.ic_logo)
                .fallback(R.drawable.ic_logo)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(binding.imageArt)

            // click item
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(song, pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: SongViewHolder) {
        // giải phóng request Glide khi tái sử dụng view
        Glide.with(holder.binding.imageArt).clear(holder.binding.imageArt)
        super.onViewRecycled(holder)
    }

    // ===== helpers =====
    private fun buildSubtitle(song: Song, artistFallback: String): String {
        val parts = mutableListOf<String>()
        if (!song.artist.isNullOrBlank()) parts += song.artist
        return if (parts.isEmpty()) artistFallback else parts.joinToString(" • ")
    }

    private fun formatDuration(durationMs: Long): String? {
        if (durationMs <= 0) return null
        val m = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val s = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return "%d:%02d".format(m, s)
    }
}