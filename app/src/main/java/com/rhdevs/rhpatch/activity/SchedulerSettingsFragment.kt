package com.rhdevs.rhpatch.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.rhdevs.rhpatch.R

class SchedulerSettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scheduler_settings, container, false)
        
        view.findViewById<Button>(R.id.btn_lock_config).setOnClickListener {
            startActivity(Intent(requireContext(), com.rhdevs.rhpatch.ui.ScreenLockConfigActivity::class.java))
        }
        
        view.findViewById<Button>(R.id.btn_troubleshoot).setOnClickListener {
            startActivity(Intent(requireContext(), TroubleshootingActivity::class.java))
        }

        return view
    }
}