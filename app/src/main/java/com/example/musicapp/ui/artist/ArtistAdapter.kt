package com.example.musicapp.ui.artist

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.musicapp.R
import com.example.musicapp.data.model.ArtistItem
import com.example.musicapp.databinding.ItemArtistBinding
import java.text.Normalizer
import java.util.Locale

class ArtistAdapter : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    private val items = mutableListOf<ArtistItem>()

    private var defaultCoverUri: Uri? = null
    private var forceUseDefaultForAll = false
    private val coverMap = mutableMapOf<String, Uri>()

    // callback click
    private var onItemClick: ((ArtistItem) -> Unit)? = null

    fun setOnItemClick(listener: (ArtistItem) -> Unit) {
        onItemClick = listener
    }

    inner class ArtistViewHolder(val binding: ItemArtistBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArtistViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding
        val ctx = b.root.context

        b.tvName.text = item.name
        b.tvSubtitle.text = "${item.count} bài"

        val placeholder = ColorDrawable(ContextCompat.getColor(ctx, R.color.surface_card))
        val radiusPx = ctx.resources.getDimensionPixelSize(R.dimen.card_radius_12dp)

        val loadUri: Uri? = if (forceUseDefaultForAll) {
            defaultCoverUri
        } else {
            val key = normalizeName(item.name)
            coverMap[key] ?: item.cover ?: defaultCoverUri
        }

        Glide.with(b.imgCover)
            .load(
                loadUri ?: Uri.parse(
                    "https://scontent.fhan5-11.fna.fbcdn.net/v/t39.30808-6/556039321_1421154148960264_6473840734220042421_n.jpg?_nc_cat=103&ccb=1-7&_nc_sid=6ee11a&_nc_eui2=AeFP1K7ihn4M8e2Qvn0cSPLpj6W4MSlqB9-PpbgxKWoH396wW-MjxLCwqWWTe1JLmPxfJUFspScu3G1UW-u5waMn&_nc_ohc=VVJ2p4Cee8UQ7kNvwE3rFlX&_nc_oc=Adls4ZfDUVNwWrEecfZWLfMtxLtHXmIAlGG-pu7zIhffj9SWvXbnHawhRhcqzlR6PuQ&_nc_zt=23&_nc_ht=scontent.fhan5-11.fna&_nc_gid=hNqXjLNa75Ip8UkoMfsWzA&oh=00_AfhJQub9zLsM3mKvFaVaWc2VRbn7omCVfOVyuVDh3HPdDg&oe=69174057"
                )
            )
            .placeholder(placeholder)
            .error(placeholder)
            .fallback(placeholder)
            .transform(CenterCrop(), RoundedCorners(radiusPx))
            .into(b.imgCover)

        // click artist
        b.root.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun onViewRecycled(holder: ArtistViewHolder) {
        Glide.with(holder.binding.imgCover).clear(holder.binding.imgCover)
        super.onViewRecycled(holder)
    }

    // --- API dữ liệu ---
    fun submitList(newItems: List<ArtistItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun submitListAndApplyCovers(newItems: List<ArtistItem>, overwrite: Boolean = false) {
        submitList(newItems)
        applyCoversToItems(overwrite)
    }

    fun setDefaultCoverUrl(url: String?) {
        defaultCoverUri = url?.let(Uri::parse)
        notifyDataSetChanged()
    }

    fun setForceUseDefaultForAll(enable: Boolean) {
        forceUseDefaultForAll = enable
        notifyDataSetChanged()
    }

    fun setCoverMap(map: Map<String, String?>) {
        coverMap.clear()
        map.forEach { (name, url) ->
            if (!url.isNullOrBlank()) coverMap[normalizeName(name)] = Uri.parse(url)
        }
        notifyDataSetChanged()
    }

    fun applyCoversToItems(overwrite: Boolean = false) {
        for (i in items.indices) {
            val it = items[i]
            val key = normalizeName(it.name)
            val uri = coverMap[key] ?: continue
            if (overwrite || it.cover == null) {
                items[i] = it.copy(cover = uri)
            }
        }
        notifyDataSetChanged()
    }

    private fun normalizeName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val lowered = name.trim().lowercase(Locale.ROOT)
        val nfd = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        return nfd.replace("\\p{M}+".toRegex(), "").replace("\\s+".toRegex(), " ")
    }
}
