package com.keepnc.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.keepnc.R
import com.keepnc.data.auth.BiometricAuthHelper
import com.keepnc.data.settings.FontSizePreset
import com.keepnc.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Settings screen allowing users to toggle confirmations for note saving and deletion,
 * configure font sizes, and view application information.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupAboutInfo()
        observePreferences()
    }

    private fun setupClickListeners() {
        binding.rowConfirmSave.setOnClickListener {
            viewModel.setConfirmSaveOnExit(!binding.switchConfirmSave.isChecked)
        }

        binding.rowConfirmDelete.setOnClickListener {
            viewModel.setConfirmDeleteNote(!binding.switchConfirmDelete.isChecked)
        }

        binding.rowEditorFontSize.setOnClickListener {
            showFontSizeDialog(
                titleRes = R.string.settings_editor_font_size_title,
                currentPreset = viewModel.editorFontSize.value
            ) { selectedPreset ->
                viewModel.setEditorFontSize(selectedPreset)
            }
        }

        binding.rowCardFontSize.setOnClickListener {
            showFontSizeDialog(
                titleRes = R.string.settings_card_font_size_title,
                currentPreset = viewModel.cardFontSize.value
            ) { selectedPreset ->
                viewModel.setCardFontSize(selectedPreset)
            }
        }

        binding.rowAppLock.setOnClickListener {
            val currentlyEnabled = binding.switchAppLock.isChecked
            if (!currentlyEnabled) {
                if (!BiometricAuthHelper.isDeviceSecure(requireContext())) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_app_lock_title)
                        .setMessage(R.string.auth_error_no_credentials)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                } else {
                    BiometricAuthHelper.authenticate(
                        activity = requireActivity(),
                        title = getString(R.string.auth_enable_prompt_title),
                        subtitle = getString(R.string.auth_prompt_subtitle)
                    ) { success ->
                        if (success) {
                            viewModel.setAppLockEnabled(true)
                        }
                    }
                }
            } else {
                BiometricAuthHelper.authenticate(
                    activity = requireActivity(),
                    title = getString(R.string.auth_disable_prompt_title),
                    subtitle = getString(R.string.auth_prompt_subtitle)
                ) { success ->
                    if (success) {
                        viewModel.setAppLockEnabled(false)
                    }
                }
            }
        }
    }

    private fun showFontSizeDialog(
        titleRes: Int,
        currentPreset: FontSizePreset,
        onSelect: (FontSizePreset) -> Unit
    ) {
        val presets = FontSizePreset.entries.toTypedArray()
        val items = presets.map { getString(it.labelRes) }.toTypedArray()
        val checkedItem = presets.indexOf(currentPreset).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                onSelect(presets[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupAboutInfo() {
        binding.tvAppVersion.text = getString(R.string.settings_app_version, viewModel.appVersion)
        binding.tvServerUrl.text = viewModel.serverUrl ?: "—"
    }

    private fun observePreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.confirmSaveOnExit.collect { isChecked ->
                        binding.switchConfirmSave.isChecked = isChecked
                    }
                }
                launch {
                    viewModel.confirmDeleteNote.collect { isChecked ->
                        binding.switchConfirmDelete.isChecked = isChecked
                    }
                }
                launch {
                    viewModel.editorFontSize.collect { preset ->
                        binding.tvEditorFontSizeValue.text = getString(preset.labelRes)
                    }
                }
                launch {
                    viewModel.cardFontSize.collect { preset ->
                        binding.tvCardFontSizeValue.text = getString(preset.labelRes)
                    }
                }
                launch {
                    viewModel.appLockEnabled.collect { isChecked ->
                        binding.switchAppLock.isChecked = isChecked
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
