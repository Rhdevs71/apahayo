package com.rhdevs.rhpatch.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wmods.wppenhacer.R

class SchedulerDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduler_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val fab = findViewById<FloatingActionButton>(R.id.fab_add_task)
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    toolbar.title = "Tugas"
                    loadFragment(TasksFragment())
                    fab.show()
                    fab.setImageResource(android.R.drawable.ic_input_add)
                    true
                }
                R.id.nav_recipients -> {
                    toolbar.title = "Penerima"
                    loadFragment(RecipientsFragment())
                    fab.show()
                    fab.setImageResource(android.R.drawable.ic_menu_myplaces)
                    true
                }
                R.id.nav_templates -> {
                    toolbar.title = "Templat"
                    loadFragment(TemplatesFragment())
                    fab.show()
                    fab.setImageResource(android.R.drawable.ic_menu_edit)
                    true
                }
                R.id.nav_settings -> {
                    toolbar.title = "Setelan"
                    loadFragment(SchedulerSettingsFragment())
                    fab.hide()
                    true
                }
                else -> false
            }
        }

        fab.setOnClickListener {
            when (bottomNav.selectedItemId) {
                R.id.nav_tasks -> {
                    startActivity(android.content.Intent(this, ComposeScheduleActivity::class.java))
                }
                R.id.nav_recipients -> {
                    Toast.makeText(this, "Fitur Buat Grup Penerima Baru (Segera Hadir)", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_templates -> {
                    Toast.makeText(this, "Fitur Buat Templat Baru (Segera Hadir)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Load initial fragment
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_tasks
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.scheduler_fragment_container, fragment)
            .commit()
    }
}
