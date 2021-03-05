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
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import net.chmielowski.baggage.ui.EquipmentListViewModel.Label.ShowUndoSnackbar
import net.chmielowski.baggage.ui.databinding.ScreenEquipmentListBinding
import net.chmielowski.baggage.ui.databinding.ViewAddEquipmentBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class EquipmentListFragment : Fragment(R.layout.screen_equipment_list) {

    private val optionsMenuDelegate = OptionsMenuDelegate()

    private val viewModel by viewModel<EquipmentListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = ScreenEquipmentListBinding.bind(view)
        val addNewBinding = ViewAddEquipmentBinding.bind(view)
        val adapter = EquipmentAdapter(
            onItemToggled = viewModel::onItemPackedToggle,
            onDeleteClicked = viewModel::onDeleteItemClick,
        )
        adapter.setHasStableIds(true)
        binding.list.adapter = adapter
//        (binding.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.observeModel().collectLatest { model ->
                binding.render(adapter, model)
                optionsMenuDelegate.render(model)
                addNewBinding.render(model)
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.observeLabels().collectLatest { label ->
                binding.consume(label)
            }
        }

        binding.bindListeners(viewModel)
        addNewBinding.bindListeners(viewModel)
    }

    private fun ScreenEquipmentListBinding.consume(label: EquipmentListViewModel.Label) =
        when (label) {
            ShowUndoSnackbar -> Snackbar
                .make(root, R.string.message_item_deleted, LENGTH_INDEFINITE)
                .setAction(R.string.action_undo) {
                    viewModel.onUndoDeleteClick()
                }
                .show()
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
        optionsMenuDelegate.onCreateOptionsMenu(menu, inflater)

    override fun onDestroyOptionsMenu() = optionsMenuDelegate.onDestroyOptionsMenu()

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

    class OptionsMenuDelegate {

        private var lastModel: EquipmentListViewModel.Model? = null

        private var delete: MenuItem? = null

        private var cancel: MenuItem? = null

        fun render(model: EquipmentListViewModel.Model) {
            lastModel = model
            refresh()
        }

        fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            inflater.inflate(R.menu.menu_equipment, menu)
            delete = menu.findItem(R.id.menuItemDelete)
            cancel = menu.findItem(R.id.menuItemCancelDeleting)

            refresh()
        }

        private fun refresh() {
            val model = lastModel ?: return
            delete?.isVisible = model.isDeleteButtonVisible
            cancel?.isVisible = model.isCancelDeletingVisible
        }

        fun onDestroyOptionsMenu() {
            delete = null
            cancel = null
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
