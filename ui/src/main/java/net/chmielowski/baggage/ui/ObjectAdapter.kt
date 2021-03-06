package net.chmielowski.baggage.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.chmielowski.baggage.ui.databinding.ItemObjectBinding

class ObjectAdapter(
    private val onItemToggled: (ObjectId, isChecked: Boolean) -> Unit,
    private val onDeleteClicked: (ObjectId) -> Unit,
) : ListAdapter<ObjectItem, ObjectAdapter.ViewHolder>(Callback) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).id.value

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemObjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    class ViewHolder(val binding: ItemObjectBinding) : RecyclerView.ViewHolder(binding.root)

    private object Callback : DiffUtil.ItemCallback<ObjectItem>() {

        override fun areItemsTheSame(oldItem: ObjectItem, newItem: ObjectItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ObjectItem, newItem: ObjectItem) =
            oldItem == newItem
    }
}
