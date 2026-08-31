package com.keepnc.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.keepnc.R
import com.keepnc.databinding.DialogCategoryPickerBinding
import com.keepnc.databinding.ItemCategoryPickerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * BottomSheet dialog for selecting or creating a note category.
 *
 * Allows the user to:
 * 1. Choose from existing categories.
 * 2. Select "No category" to remove categorization.
 * 3. Type a new name and create it immediately.
 */
@AndroidEntryPoint
class CategoryPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogCategoryPickerBinding? = null
    private val binding get() = _binding!!

    private val editorViewModel: EditorViewModel by viewModels({ requireParentFragment() })

    private lateinit var adapter: CategoryAdapter
    private var allCategories: List<String> = emptyList()
    private var currentCategory: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentCategory = arguments?.getString(ARG_CURRENT_CATEGORY) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCategoryPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupInput()
        observeCategories()
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            onCategoryClick = { selected ->
                selectCategoryAndDismiss(selected)
            }
        )
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter
    }

    private fun setupInput() {
        binding.etNewCategory.addTextChangedListener { text ->
            val query = text?.toString()?.trim() ?: ""
            updateListAndCreateRow(query)
        }

        binding.etNewCategory.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = binding.etNewCategory.text?.toString()?.trim() ?: ""
                if (newName.isNotBlank()) {
                    selectCategoryAndDismiss(newName)
                    return@setOnEditorActionListener true
                }
            }
            false
        }

        binding.rowCreateCategory.setOnClickListener {
            val newName = binding.etNewCategory.text?.toString()?.trim() ?: ""
            if (newName.isNotBlank()) {
                selectCategoryAndDismiss(newName)
            }
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                editorViewModel.allCategories.collect { categories ->
                    allCategories = categories
                    val query = binding.etNewCategory.text?.toString()?.trim() ?: ""
                    updateListAndCreateRow(query)
                }
            }
        }
    }

    private fun updateListAndCreateRow(query: String) {
        // Build the list of CategoryItems:
        // Item 1: "No category" (represented as empty string)
        // Items 2..N: filtered or all categories
        val items = mutableListOf<CategoryItem>()

        if (query.isBlank()) {
            // Include "No category" option at top
            items.add(
                CategoryItem(
                    name = "",
                    displayName = getString(R.string.category_none),
                    isNoneOption = true,
                    isSelected = currentCategory.isBlank()
                )
            )
            for (cat in allCategories) {
                items.add(
                    CategoryItem(
                        name = cat,
                        displayName = cat,
                        isNoneOption = false,
                        isSelected = cat.equals(currentCategory, ignoreCase = true)
                    )
                )
            }
            binding.rowCreateCategory.visibility = View.GONE
        } else {
            // Check if exact match exists in categories
            val exactMatchExists = allCategories.any { it.equals(query, ignoreCase = true) }

            if (!exactMatchExists) {
                binding.rowCreateCategory.visibility = View.VISIBLE
                binding.tvCreateCategoryLabel.text = getString(R.string.category_picker_create, query)
            } else {
                binding.rowCreateCategory.visibility = View.GONE
            }

            // Filter existing categories by query
            val filtered = allCategories.filter { it.contains(query, ignoreCase = true) }
            for (cat in filtered) {
                items.add(
                    CategoryItem(
                        name = cat,
                        displayName = cat,
                        isNoneOption = false,
                        isSelected = cat.equals(currentCategory, ignoreCase = true)
                    )
                )
            }
        }

        adapter.submitList(items)
    }

    private fun selectCategoryAndDismiss(categoryName: String) {
        editorViewModel.setCategory(categoryName)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CategoryPickerBottomSheet"
        private const val ARG_CURRENT_CATEGORY = "current_category"

        fun newInstance(currentCategory: String): CategoryPickerBottomSheet {
            return CategoryPickerBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_CATEGORY, currentCategory)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Adapter & View Holder
    // -------------------------------------------------------------------------

    data class CategoryItem(
        val name: String,
        val displayName: String,
        val isNoneOption: Boolean,
        val isSelected: Boolean
    )

    private class CategoryAdapter(
        private val onCategoryClick: (String) -> Unit
    ) : ListAdapter<CategoryItem, CategoryViewHolder>(CategoryDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val binding = ItemCategoryPickerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return CategoryViewHolder(binding, onCategoryClick)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class CategoryViewHolder(
        private val binding: ItemCategoryPickerBinding,
        private val onCategoryClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryItem) {
            binding.tvCategoryName.text = item.displayName
            binding.ivCategoryIcon.setImageResource(
                if (item.isNoneOption) R.drawable.ic_clear else R.drawable.ic_category
            )
            binding.ivSelectedCheck.visibility = if (item.isSelected) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onCategoryClick(item.name)
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryItem>() {
        override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean =
            oldItem == newItem
    }
}
