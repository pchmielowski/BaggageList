package net.chmielowski.baggage.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import net.chmielowski.baggage.ui.databinding.ScreenEquipmentListBinding
import net.chmielowski.baggage.ui.databinding.ViewAddEquipmentBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class EquipmentListFragment : Fragment(R.layout.screen_equipment_list) {

    private var optionsMenu: Menu? = null

    private val viewModel by viewModel<EquipmentListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = ScreenEquipmentListBinding.bind(view)
        val addNewBinding = ViewAddEquipmentBinding.bind(view)
        val adapter = EquipmentAdapter(
            onItemToggled = { id, isToggled -> viewModel.onItemPackedToggle(id, isToggled) },
        )
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.observeModel().collectLatest { model ->
                binding.render(adapter, model)
                optionsMenu!!.findItem(R.id.menuItemDelete).isVisible = model.isDeleteButtonVisible
                optionsMenu!!.findItem(R.id.menuItemCancelDeleting).isVisible = model.isCancelDeletingVisible
                addNewBinding.render(model)
            }
        }

        binding.bindListeners(viewModel)
        addNewBinding.bindListeners(viewModel)
    }

    private fun ScreenEquipmentListBinding.render(
        adapter: EquipmentAdapter,
        model: EquipmentListViewModel.Model,
    ) {
        adapter.submitList(model.items)
        addNew.isVisible = model.isAddNewVisible
        val animation = ProgressBarAnimation(
            progress.progressIndicator,
            model.progress
        )
        animation.duration = 100
        progress.progressIndicator.startAnimation(animation)

        progress.progressMessage.text =
            requireContext().getString(R.string.label_packing_progress, model.progress)
    }

    private fun ViewAddEquipmentBinding.render(
        model: EquipmentListViewModel.Model
    ) {
        addNewInputGroup.isVisible = model.isInputVisible
    }

    private fun ScreenEquipmentListBinding.bindListeners(viewModel: EquipmentListViewModel) {
        addNew.setOnClickListener {
            viewModel.onAddItemClick()
        }
    }

    private fun ViewAddEquipmentBinding.bindListeners(viewModel: EquipmentListViewModel) {
        newItemName.doOnTextChanged { text ->
            viewModel.onNewItemNameEnter(text)
        }
        confirmAdding.setOnClickListener {
            viewModel.onAddingNewItemConfirm()
        }
        cancel.setOnClickListener {
            viewModel.onCancelAddingClick()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_equipment, menu)
        optionsMenu = menu
    }

    override fun onDestroyOptionsMenu() {
        optionsMenu = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuItemDelete -> {
                viewModel.onDeleteClick()
                true
            }
            R.id.menuItemCancelDeleting -> {
                viewModel.onCancelDeletingClick()
                true
            }
            else -> false
        }
    }
}

// TODO: Move
fun TextView.doOnTextChanged(action: (text: String) -> Unit) =
    doOnTextChanged { text, _, _, _ ->
        action(text!!.toString())
    }

class ProgressBarAnimation(
    private val progressBar: ProgressBar,
    private val to: Int
) : Animation() {

    private val from = progressBar.progress

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        val value = from + (to - from) * interpolatedTime
        progressBar.progress = value.toInt()
    }
}
