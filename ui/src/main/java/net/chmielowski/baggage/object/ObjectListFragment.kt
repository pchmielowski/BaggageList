package net.chmielowski.baggage.`object`

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
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import net.chmielowski.baggage.`object`.ObjectListViewModel.Label.ShowUndoSnackbar
import net.chmielowski.baggage.ui.R
import net.chmielowski.baggage.ui.databinding.ScreenObjectsListBinding
import net.chmielowski.baggage.ui.databinding.ViewAddObjectBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ObjectListFragment : Fragment(R.layout.screen_objects_list) {

    private val optionsMenuDelegate = OptionsMenuDelegate()

    private val viewModel by viewModel<ObjectListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = ScreenObjectsListBinding.bind(view)
        val addNewBinding = ViewAddObjectBinding.bind(view)
        val adapter = ObjectAdapter(
            onItemToggled = viewModel::onItemPackedToggle,
            onDeleteClicked = viewModel::onDeleteItemClick,
        )
        binding.list.adapter = adapter

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

    private fun ScreenObjectsListBinding.consume(label: ObjectListViewModel.Label) =
        when (label) {
            is ShowUndoSnackbar -> Snackbar
                .make(root, getString(R.string.message_item_deleted, label.itemName), LENGTH_LONG)
                .setAction(R.string.action_undo) {
                    viewModel.onUndoDeleteClick()
                }
                .show()
        }

    private fun ScreenObjectsListBinding.render(
        adapter: ObjectAdapter,
        model: ObjectListViewModel.Model,
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

    private fun ViewAddObjectBinding.render(
        model: ObjectListViewModel.Model
    ) {
        addNewInputGroup.isVisible = model.isInputVisible
    }

    private fun ScreenObjectsListBinding.bindListeners(viewModel: ObjectListViewModel) {
        addNew.setOnClickListener {
            viewModel.onAddItemClick()
        }
    }

    private fun ViewAddObjectBinding.bindListeners(viewModel: ObjectListViewModel) {
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

        private var lastModel: ObjectListViewModel.Model? = null

        private var delete: MenuItem? = null

        private var cancel: MenuItem? = null

        fun render(model: ObjectListViewModel.Model) {
            lastModel = model
            refresh()
        }

        fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            inflater.inflate(R.menu.menu_object, menu)
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
