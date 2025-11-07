package com.example.musicapp.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.data.model.Song
import com.example.musicapp.databinding.ItemSongBinding
import java.util.concurrent.TimeUnit
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

// SongAdapter - hiển thị danh sách bài hát và xử lý nhấn vào một bài
class SongAdapter(
    private val onClick: (Song, Int) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(Diff) {

    // Diff - so sánh phần tử để cập nhật danh sách hiệu quả
    object Diff : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }

    // SongViewHolder - giữ binding và gán dữ liệu cho từng hàng
    inner class SongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // bind - hiển thị tiêu đề, nghệ sĩ, thời lượng và ảnh bìa; gắn sự kiện nhấn
        fun bind(song: Song) {
            val context = binding.root.context
            val artistFallback = context.getString(R.string.artist_placeholder)

            // textTitle - hiển thị tên bài hát
            binding.textTitle.text = song.title

            // textSubtitle - hiển thị nghệ sĩ (hoặc giá trị dự phòng)
            binding.textSubtitle.text = buildSubtitle(song, artistFallback)

            // textDuration - hiển thị thời lượng dạng m:ss (ẩn nếu không có dữ liệu)
            binding.textDuration.text = formatDuration(song.durationMs) ?: ""
            binding.textDuration.isVisible = !binding.textDuration.text.isNullOrEmpty()

            // imageArt - tải ảnh bìa bằng Glide (ưu tiên artworkUri, sau đó đến uri)
            Glide.with(context)
                .load(song.artworkUri ?: song.uri)
                .placeholder(R.drawable.ic_logo)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(binding.imageArt)

            // root.onClick - chuyển sự kiện nhấn ra ngoài (đưa kèm vị trí)
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(song, position)
                }
            }
        }
    }

    // onCreateViewHolder - tạo ViewHolder với view binding của item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    // onBindViewHolder - gán dữ liệu bài hát tại vị trí tương ứng
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // buildSubtitle - kết hợp các trường phụ (ví dụ nghệ sĩ) thành chuỗi hiển thị
    private fun buildSubtitle(song: Song, artistFallback: String): String {
        val parts = mutableListOf<String>()
        if (!song.artist.isNullOrBlank()) {
            parts += song.artist
        }
        return if (parts.isEmpty()) artistFallback else parts.joinToString(" • ")
    }

    // formatDuration - đổi mili-giây thành chuỗi m:ss
    private fun formatDuration(durationMs: Long): String? {
        if (durationMs <= 0) return null
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
