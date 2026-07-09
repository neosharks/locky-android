package com.neosharks.locky

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.neosharks.locky.databinding.ActivityPickerBinding
import java.util.concurrent.Executors

/**
 * Lists every installed app. Tap or long-press an app to lock / unlock it —
 * each action confirms identity via fingerprint / device credential first.
 */
class AppPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPickerBinding
    private lateinit var repo: LockRepository
    private lateinit var adapter: AllAppsAdapter
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = LockRepository(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = AllAppsAdapter(onToggle = ::onToggle)
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        loadApps()
    }

    private fun loadApps() {
        binding.progress.visibility = View.VISIBLE
        io.execute {
            val apps = AppsProvider.loadAllApps(this, repo.getLockedApps())
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                adapter.submit(apps)
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun onToggle(app: AppInfo) {
        val locking = !app.locked
        val title = if (locking) R.string.lock_title else R.string.unlock_title
        BiometricHelper.prompt(
            activity = this,
            title = getString(title, app.label),
            subtitle = getString(R.string.confirm_identity),
            onSuccess = {
                if (locking) repo.lock(app.packageName) else repo.unlock(app.packageName)
                adapter.updateLocked(app.packageName, locking)
            },
            onFailure = {}
        )
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}
