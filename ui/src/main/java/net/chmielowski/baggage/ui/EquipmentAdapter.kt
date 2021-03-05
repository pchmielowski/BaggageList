package net.chmielowski.baggage.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.chmielowski.baggage.ui.databinding.ItemEquipmentBinding

class EquipmentAdapter(
    private val onItemToggled: (EquipmentId, isChecked: Boolean) -> Unit,
    private val onDeleteClicked: (EquipmentId) -> Unit,
) : ListAdapter<EquipmentItem, EquipmentAdapter.ViewHolder>(Callback) {

//    init {
//        setHasStableIds(true)
//    }

    override fun getItemId(position: Int): Long {
        val id = getItem(position).id.value
        Log.d("pchm", "getItemId $id")
        return id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("pchm", "onCreateViewHolder")
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        super.onBindViewHolder(holder, position, payloads)
//        Log.d("pchm", "$payloads")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("pchm", "onBindViewHolder ${holder.hashCode()}")
        val item = getItem(position)
        val binding = holder.binding

//        binding.name.text = item.name
//        binding.name.isChecked = item.isChecked
//        binding.delete.isVisible = item.isDeleteVisible
    }

    class ViewHolder(val binding: ItemEquipmentBinding) : RecyclerView.ViewHolder(binding.root)

    private object Callback : DiffUtil.ItemCallback<EquipmentItem>() {

        override fun areItemsTheSame(oldItem: EquipmentItem, newItem: EquipmentItem): Boolean {
            val ret = oldItem.id == newItem.id
            Log.d("pchm", "areItemsTheSame $ret")
            return ret
        }

        override fun areContentsTheSame(oldItem: EquipmentItem, newItem: EquipmentItem): Boolean {
            val ret = oldItem == newItem
            Log.d("pchm", "areContentsTheSame $ret")
            return ret
        }

        override fun getChangePayload(oldItem: EquipmentItem, newItem: EquipmentItem): Any? {
//            Log.d("pchm", "$oldItem -> $newItem")
            return super.getChangePayload(oldItem, newItem)
        }


    }
}
