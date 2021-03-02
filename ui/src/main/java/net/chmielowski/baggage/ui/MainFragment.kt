package net.chmielowski.baggage.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import net.chmielowski.baggage.ui.databinding.ScreenItemsBinding

class MainFragment : Fragment(R.layout.screen_items) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = ScreenItemsBinding.bind(view)
        val adapter = EquipmentAdapter()
        binding.list.adapter = adapter

        adapter.submitList((1..10).map { EquipmentItem(EquipmentId(it), "Item $it") })
    }
}
