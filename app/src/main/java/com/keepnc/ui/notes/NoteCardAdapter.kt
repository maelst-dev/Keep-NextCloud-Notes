package com.keepnc.ui.notes

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.keepnc.R
import com.keepnc.data.local.NoteEntity
import com.keepnc.data.local.SyncStatus
import com.keepnc.data.settings.FontSizePreset
import com.keepnc.databinding.ItemNoteCardBinding
import com.keepnc.ui.MarkwonFactory

/**
 * RecyclerView adapter for the note card grid.
 *
 * Uses [ListAdapter] + [DiffUtil.ItemCallback] so only changed cards are redrawn,
 * preventing the whole list from flickering on every Room update.
 *
 * Markwon is created once per ViewHolder (via [MarkwonFactory]) and reused for
 * every rebind — creating it per-bind would be wasteful.
 *
 * @param onNoteClick     Invoked when the user taps a card
 * @param onNoteLongClick Invoked on long-press; return true to consume the event
 * @param cardFontSize    Font size preset for card title and content
 */
class NoteCardAdapter(
    private val onNoteClick: (NoteEntity) -> Unit,
    private val onNoteLongClick: (NoteEntity) -> Boolean,
    var cardFontSize: FontSizePreset = FontSizePreset.DEFAULT
) : ListAdapter<NoteEntity, NoteCardAdapter.NoteViewHolder>(DiffCallback) {

    // -----------------------------------------------------------------------
    // ViewHolder
    // -----------------------------------------------------------------------

    inner class NoteViewHolder(private val binding: ItemNoteCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // One Markwon instance per ViewHolder — created lazily on first bind
        private val markwon by lazy { MarkwonFactory.createForCard(binding.root.context) }

        fun bind(note: NoteEntity) {
            // Apply card font size
            binding.tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, cardFontSize.cardTitleSp)
            binding.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, cardFontSize.cardContentSp)

            // Title — hide when blank so the card looks cleaner
            if (note.title.isBlank()) {
                binding.tvTitle.visibility = View.GONE
            } else {
                binding.tvTitle.visibility = View.VISIBLE
                binding.tvTitle.text = note.title
            }

            // Content excerpt rendered as Markdown (includes task-list checkboxes).
            // Nextcloud Notes stores the title as the first line of content, so we
            // strip it here to avoid showing it twice (the card header already has it).
            val rawExcerpt = note.excerpt
            val excerpt = if (note.title.isNotBlank()) {
                val firstLine = rawExcerpt.substringBefore('\n').trim()
                if (firstLine == note.title.trim()) {
                    rawExcerpt.substringAfter('\n', missingDelimiterValue = "").trimStart('\r', '\n')
                } else rawExcerpt
            } else rawExcerpt

            val formattedExcerpt = MarkwonFactory.formatChecklistStrikethrough(excerpt)

            if (formattedExcerpt.isBlank()) {
                binding.tvContent.visibility = View.GONE
            } else {
                binding.tvContent.visibility = View.VISIBLE
                markwon.setMarkdown(binding.tvContent, formattedExcerpt)
                // Markwon sets LinkMovementMethod which swallows all touch events —
                // remove it so clicks bubble up to the MaterialCardView instead.
                binding.tvContent.movementMethod = null
            }

            // Category chip
            if (note.category.isBlank()) {
                binding.tvCategory.visibility = View.GONE
            } else {
                binding.tvCategory.visibility = View.VISIBLE
                binding.tvCategory.text = note.category
            }

            // Favourite / pin icon
            binding.ivFavorite.setImageResource(
                if (note.favorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )

            // Sync indicator dot — visible when the note has local unsaved changes
            binding.ivSyncIndicator.visibility =
                if (note.syncStatus == SyncStatus.DIRTY ||
                    note.syncStatus == SyncStatus.PENDING_DELETE
                ) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onNoteClick(note) }
            binding.root.setOnLongClickListener { onNoteLongClick(note) }
        }
    }

    // -----------------------------------------------------------------------
    // Adapter overrides
    // -----------------------------------------------------------------------

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // -----------------------------------------------------------------------
    // DiffUtil
    // -----------------------------------------------------------------------

    companion object DiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity) =
            oldItem == newItem
    }
}
