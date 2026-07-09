package com.neosharks.locky

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neosharks.locky.databinding.ItemLockedAppBinding

/** List of currently locked apps. Tap "Unlock" (or long-press the row) to remove one. */
class LockedAppsAdapter(
    private val onUnlock: (AppInfo) -> Unit
) : RecyclerView.Adapter<LockedAppsAdapter.VH>() {

    private val items = mutableListOf<AppInfo>()

    fun submit(list: List<AppInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLockedAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class VH(private val b: ItemLockedAppBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(app: AppInfo) {
            b.icon.setImageDrawable(app.icon)
            b.name.text = app.label
            b.unlockBtn.setOnClickListener { onUnlock(app) }
            b.root.setOnLongClickListener { onUnlock(app); true }
        }
    }
}
