package net.chmielowski.baggage.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.chmielowski.baggage.ui.databinding.ItemEquipmentBinding
import net.chmielowski.baggage.ui.databinding.ScreenItemsBinding

class MainFragment : Fragment(R.layout.screen_items) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = ScreenItemsBinding.bind(view)
        val adapter = EquipmentAdapter()
        binding.list.adapter = adapter
    }
}

class EquipmentAdapter : ListAdapter<EquipmentItem, EquipmentAdapter.ViewHolder>(Callback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemEquipmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding
        // TODO
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

data class EquipmentItem(
    val id: EquipmentId,
    val name: String,
)

data class EquipmentId(val value: Int)
