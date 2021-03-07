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
import net.chmielowski.baggage.`object`.ObjectListViewModel.Intent
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
            onItemToggled = { id, isPacked -> viewModel.accept(Intent.MarkPacked(id, isPacked)) },
            onDeleteClicked = { id -> viewModel.accept(Intent.Delete(id)) },
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

        addNewBinding.bindListeners(viewModel)
    }

    private fun ScreenObjectsListBinding.consume(label: ObjectListViewModel.Label) =
        when (label) {
            is ShowUndoSnackbar -> Snackbar
                .make(root, getString(R.string.message_item_deleted, label.itemName), LENGTH_LONG)
                .setAction(R.string.action_undo) {
                    viewModel.accept(Intent.UndoDeleting)
                }
                .show()
        }

    private fun ScreenObjectsListBinding.render(
        adapter: ObjectAdapter,
        model: ObjectListViewModel.Model,
    ) {
        adapter.submitList(model.items)
        progress.root.isVisible = model.isProgressVisible
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
        addNewInputGroup.isVisible = model.isNewObjectViewVisible
    }

    private fun ViewAddObjectBinding.bindListeners(viewModel: ObjectListViewModel) {
        newItemName.doOnTextChanged { text ->
            viewModel.accept(Intent.SetNewObjectName(text))
        }
        confirmAdding.setOnClickListener {
            viewModel.accept(Intent.ConfirmAddingObject)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
        optionsMenuDelegate.onCreateOptionsMenu(menu, inflater)

    override fun onDestroyOptionsMenu() = optionsMenuDelegate.onDestroyOptionsMenu()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.enterEditMode -> {
                viewModel.accept(Intent.EnterEditMode)
                true
            }
            R.id.exitEditMode -> {
                viewModel.accept(Intent.ExitEditMode)
                true
            }
            else -> false
        }
    }

    class OptionsMenuDelegate {

        private var lastModel: ObjectListViewModel.Model? = null

        private var enterEditMode: MenuItem? = null

        private var exitEditMode: MenuItem? = null

        fun render(model: ObjectListViewModel.Model) {
            lastModel = model
            refresh()
        }

        fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            inflater.inflate(R.menu.menu_object, menu)
            enterEditMode = menu.findItem(R.id.enterEditMode)
            exitEditMode = menu.findItem(R.id.exitEditMode)

            refresh()
        }

        private fun refresh() {
            val model = lastModel ?: return
            enterEditMode?.isVisible = model.isEditButtonVisible
            exitEditMode?.isVisible = model.isCancelButtonVisible
        }

        fun onDestroyOptionsMenu() {
            enterEditMode = null
            exitEditMode = null
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
