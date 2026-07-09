package com.neosharks.locky

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neosharks.locky.databinding.ItemAppBinding

/**
 * Full app list in the picker. Tap or long-press a row to toggle its lock state
 * (both confirm identity first). The lock icon shows the current state.
 */
class AllAppsAdapter(
    private val onToggle: (AppInfo) -> Unit
) : RecyclerView.Adapter<AllAppsAdapter.VH>() {

    private val items = mutableListOf<AppInfo>()

    fun submit(list: List<AppInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    /** Update one app's lock state after a successful toggle. */
    fun updateLocked(pkg: String, locked: Boolean) {
        val i = items.indexOfFirst { it.packageName == pkg }
        if (i >= 0) {
            items[i].locked = locked
            notifyItemChanged(i)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class VH(private val b: ItemAppBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(app: AppInfo) {
            b.icon.setImageDrawable(app.icon)
            b.name.text = app.label
            b.lockIndicator.setImageResource(
                if (app.locked) R.drawable.ic_lock else R.drawable.ic_lock_open
            )
            b.lockIndicator.alpha = if (app.locked) 1f else 0.3f
            b.root.setOnClickListener { onToggle(app) }
            b.root.setOnLongClickListener { onToggle(app); true }
        }
    }
}
