package net.chmielowski.baggage.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.chmielowski.baggage.ui.databinding.ItemEquipmentBinding

class EquipmentAdapter : ListAdapter<EquipmentItem, EquipmentAdapter.ViewHolder>(Callback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemEquipmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        binding.name.text = item.name
    }

    class ViewHolder(val binding: ItemEquipmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    private object Callback : DiffUtil.ItemCallback<EquipmentItem>() {

        override fun areItemsTheSame(oldItem: EquipmentItem, newItem: EquipmentItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: EquipmentItem, newItem: EquipmentItem) =
            oldItem == newItem
    }
}
