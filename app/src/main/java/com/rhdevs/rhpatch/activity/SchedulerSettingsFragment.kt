package com.rhdevs.rhpatch.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class SchedulerSettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.parseColor("#0B0E14"))
        }

        val desc = TextView(context).apply {
            text = "Konfigurasi sistem otomatisasi penjadwalan."
            textSize = 14f
            setTextColor(Color.parseColor("#8F9CAE"))
            setPadding(0, 0, 0, 60)
        }
        layout.addView(desc)
        
        val btnLockConfig = Button(context).apply {
            text = "Konfigurasi Kunci Layar (PIN/Sandi)"
            setBackgroundColor(Color.parseColor("#9C87F2"))
            setTextColor(Color.parseColor("#0B0E14"))
            setOnClickListener {
                startActivity(Intent(context, com.rhdevs.rhpatch.ui.ScreenLockConfigActivity::class.java))
            }
        }
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 40) }
        
        layout.addView(btnLockConfig, layoutParams)
        
        val btnTroubleshoot = Button(context).apply {
            text = "Tidak Berfungsi? Pusat Bantuan"
            setBackgroundColor(Color.parseColor("#141A26"))
            setTextColor(Color.parseColor("#E3E6EB"))
            setOnClickListener {
                startActivity(Intent(context, com.rhdevs.rhpatch.activity.TroubleshootingActivity::class.java))
            }
        }
        layout.addView(btnTroubleshoot)

        return layout
    }
}
