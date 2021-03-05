package net.chmielowski.baggage.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.chmielowski.baggage.ui.databinding.ItemEquipmentBinding

class EquipmentAdapter(
    private val onItemToggled: (EquipmentId, isChecked: Boolean) -> Unit,
    private val onDeleteClicked: (EquipmentId) -> Unit,
) : ListAdapter<EquipmentItem, EquipmentAdapter.ViewHolder>(Callback) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).id.value

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemEquipmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(binding)
        binding.name.setOnCheckedChangeListener { _, isChecked ->
            onItemToggled(getItem(holder.adapterPosition).id, isChecked)
        }
        binding.delete.setOnClickListener {
            onDeleteClicked(getItem(holder.adapterPosition).id)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        binding.name.text = item.name
        binding.name.isChecked = item.isChecked
        binding.delete.isVisible = item.isDeleteVisible
    }

    class ViewHolder(val binding: ItemEquipmentBinding) : RecyclerView.ViewHolder(binding.root)

    private object Callback : DiffUtil.ItemCallback<EquipmentItem>() {

        override fun areItemsTheSame(oldItem: EquipmentItem, newItem: EquipmentItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: EquipmentItem, newItem: EquipmentItem) =
            oldItem == newItem
    }
}
