package net.chmielowski.baggage.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import net.chmielowski.baggage.ui.databinding.ScreenEquipmentListBinding

class EquipmentListFragment : Fragment(R.layout.screen_equipment_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = ScreenEquipmentListBinding.bind(view)
        val adapter = EquipmentAdapter()
        binding.list.adapter = adapter

        adapter.submitList((1..30).map { EquipmentItem(EquipmentId(it), "Item $it") })
    }
}
