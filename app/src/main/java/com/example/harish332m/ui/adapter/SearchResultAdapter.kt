package com.example.harish332m.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.harish332m.data.model.SearchResultItem
import com.example.harish332m.databinding.ItemSearchResultBinding
import java.util.Locale

class SearchResultAdapter : ListAdapter<SearchResultItem, SearchResultAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchResultItem) {
            val formattedDoc = "${item.document} • Page ${item.page}"
            binding.tvDocName.text = formattedDoc

            val matchPercent = (item.score * 100).coerceIn(0f, 100f)
            val scoreText = String.format(Locale.US, "%.1f%% Match", matchPercent)
            binding.tvScoreBadge.text = scoreText

            binding.tvChunkText.text = item.text
            binding.tvChunkMeta.text = "Chunk #${item.chunkId}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SearchResultItem>() {
        override fun areItemsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem): Boolean {
            return oldItem.chunkId == newItem.chunkId
        }

        override fun areContentsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem): Boolean {
            return oldItem == newItem
        }
    }
}
