package com.keepnc.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.keepnc.R
import com.keepnc.data.auth.BiometricAuthHelper
import com.keepnc.data.auth.TokenStorage
import com.keepnc.data.repository.NotesRepository
import com.keepnc.data.settings.SettingsStorage
import com.keepnc.databinding.ActivityMainBinding
import com.keepnc.ui.auth.LoginActivity
import com.keepnc.ui.notes.NotesFilter
import com.keepnc.ui.notes.NotesFragment
import com.keepnc.ui.notes.NotesViewModel
import androidx.fragment.app.viewModels
import com.keepnc.work.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The main shell of the app.
 *
 * Navigation drawer is managed entirely by [setupActionBarWithNavController] —
 * we do NOT use [ActionBarDrawerToggle] because it conflicts with the Navigation
 * component's automatic icon management:
 * - top-level destination (NotesFragment) → hamburger icon → opens drawer
 * - nested destination (EditorFragment)   → back arrow → calls navigateUp()
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var appBarConfiguration: AppBarConfiguration

    @Inject
    lateinit var tokenStorage: TokenStorage

    @Inject
    lateinit var repository: NotesRepository

    @Inject
    lateinit var settingsStorage: SettingsStorage

    private var isAppLocked = false
    private var isAuthenticatingBiometric = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAppLocked = savedInstanceState?.getBoolean(KEY_APP_LOCKED)
            ?: (tokenStorage.isLoggedIn() && settingsStorage.isAppLockEnabled())

        setSupportActionBar(binding.toolbar)

        binding.btnUnlock.setOnClickListener {
            promptUnlock()
        }

        setupNavigation()
        observeCategories()
        observeSync()
        schedulePeriodicSync()
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // AppBarConfiguration tells Navigation component which destinations are "top-level"
        // (show hamburger) vs nested (show back arrow). Providing drawerLayout makes the
        // hamburger click open the drawer automatically via NavigationUI.navigateUp().
        appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(R.id.notesFragment),
            drawerLayout = binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.notesFragment) {
                updateToolbarTitle(currentFilter)
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        }

        // Handle drawer item clicks
        binding.navView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_all_notes -> {
                    showNotesWithFilter(NotesFilter.All)
                    true
                }
                R.id.nav_favorites -> {
                    showNotesWithFilter(NotesFilter.Favorites)
                    true
                }
                R.id.nav_settings -> {
                    navigateToSettings()
                    true
                }
                R.id.nav_logout -> {
                    showLogoutDialog()
                    true
                }
                else -> {
                    // Dynamic category items — the title is the category name
                    val category = item.title?.toString() ?: ""
                    showNotesWithFilter(NotesFilter.ByCategory(category))
                    true
                }
            }
        }
    }

    private fun navigateToSettings() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController ?: return
        if (navController.currentDestination?.id != R.id.settingsFragment) {
            navController.popBackStack(R.id.notesFragment, false)
            navController.navigate(R.id.action_notesFragment_to_settingsFragment)
        }
    }

    private var currentFilter: NotesFilter = NotesFilter.All

    private fun updateToolbarTitle(filter: NotesFilter) {
        supportActionBar?.title = when (filter) {
            is NotesFilter.All -> getString(R.string.notes_all)
            is NotesFilter.Favorites -> getString(R.string.notes_favorites)
            is NotesFilter.ByCategory -> filter.category
            is NotesFilter.Search -> getString(R.string.notes_search_hint)
        }
    }

    /**
     * Applies a filter to the currently visible [NotesFragment].
     * We find the fragment via the NavHostFragment's child fragment manager.
     *
     * If the current destination is not NotesFragment (e.g., user is in the editor),
     * navigate back to it first, then apply the filter.
     */
    private fun showNotesWithFilter(filter: NotesFilter) {
        currentFilter = filter
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController

        // Pop back to the notes list if we're inside the editor
        navController?.popBackStack(R.id.notesFragment, false)

        // Find the NotesFragment and update its filter directly via its ViewModel
        val notesFragment = navHostFragment
            ?.childFragmentManager
            ?.fragments
            ?.filterIsInstance<NotesFragment>()
            ?.firstOrNull()

        notesFragment?.let {
            it.requireView() // ensure view is attached
            (it.viewModels<NotesViewModel>().value).setFilter(filter)
        }

        updateToolbarTitle(filter)
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    updateCategoryMenuItems(categories)
                }
            }
        }
    }

    private fun observeSync() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.isSyncing.collect { isSyncing ->
                    binding.syncProgressBar.visibility = if (isSyncing) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun updateCategoryMenuItems(categories: List<String>) {
        val menu = binding.navView.menu
        // Remove existing dynamic category items (group id = R.id.group_categories)
        menu.removeGroup(R.id.group_categories)

        categories.forEachIndexed { index, category ->
            menu.add(R.id.group_categories, Menu.NONE, index, category).apply {
                setIcon(R.drawable.ic_category)
            }
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.action_logout) { _, _ ->
                logout()
            }
            .show()
    }

    private fun logout() {
        // Cancel all pending sync work
        WorkManager.getInstance(this).cancelUniqueWork(SyncWorker.WORK_NAME_PERIODIC)
        WorkManager.getInstance(this).cancelUniqueWork(SyncWorker.WORK_NAME_ONE_TIME)

        lifecycleScope.launch {
            // Clear local notes and stored credentials
            repository.clearAllLocalNotes()
            tokenStorage.clearCredentials()
            isAppLocked = false
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

            // Go back to login screen
            startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (tokenStorage.isLoggedIn() && settingsStorage.isAppLockEnabled()) {
            if (isAppLocked) {
                binding.layoutLockScreen.visibility = View.VISIBLE
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                promptUnlock()
            } else {
                binding.layoutLockScreen.visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        } else {
            isAppLocked = false
            binding.layoutLockScreen.visibility = View.GONE
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isAuthenticatingBiometric &&
            tokenStorage.isLoggedIn() && settingsStorage.isAppLockEnabled()
        ) {
            isAppLocked = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_APP_LOCKED, isAppLocked)
    }

    private fun promptUnlock() {
        if (!isAppLocked || isAuthenticatingBiometric) return
        isAuthenticatingBiometric = true

        BiometricAuthHelper.authenticate(
            activity = this,
            title = getString(R.string.auth_prompt_title),
            subtitle = getString(R.string.auth_prompt_subtitle)
        ) { success ->
            isAuthenticatingBiometric = false
            if (success) {
                isAppLocked = false
                binding.layoutLockScreen.visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                binding.layoutLockScreen.visibility = View.VISIBLE
            }
        }
    }

    private fun schedulePeriodicSync() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME_PERIODIC,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.buildPeriodicRequest()
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController ?: return super.onSupportNavigateUp()
        // NavigationUI.navigateUp handles BOTH cases:
        //   - top-level destination + drawer → opens the drawer
        //   - nested destination            → pops the back stack (goes back)
        return NavigationUI.navigateUp(navController, appBarConfiguration)
            || super.onSupportNavigateUp()
    }

    companion object {
        private const val KEY_APP_LOCKED = "key_app_locked"
    }
}
