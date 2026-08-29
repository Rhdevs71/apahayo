@file:Suppress("DEPRECATION") @file:SuppressLint("WorldReadableFiles")
package com.rhdevs.rhpatch.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.rhdevs.rhpatch.activities.base.BaseActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.rhdevs.rhpatch.R

class TikTokSettingsActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    
    private lateinit var layoutMainMenu: LinearLayout
    private var currentSubMenu: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tiktok_settings)

        prefs = getSharedPreferences("prefs", Context.MODE_WORLD_READABLE)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        layoutMainMenu = findViewById(R.id.layout_main_menu)
        
        setupMenuRow(R.id.row_feed_filter, R.id.layout_sub_feed_filter, "Feed filter")
        setupMenuRow(R.id.row_feed_nav, R.id.layout_sub_feed_nav, "Feed navigation")
        setupMenuRow(R.id.row_interface, R.id.layout_sub_interface, "Interface")
        setupMenuRow(R.id.row_comments, R.id.layout_sub_comments, "Comments and translation")
        setupMenuRow(R.id.row_downloads, R.id.layout_sub_downloads, "Downloads")
        setupMenuRow(R.id.row_bypass_regional, R.id.layout_sub_regional, "Bypass regional restriction")
        setupMenuRow(R.id.row_app_behavior, R.id.layout_sub_app_behavior, "App behavior")

        // 1. Feed Filter
        setupSwitch(R.id.switch_remove_ads, "tiktok_remove_ads")
        setupSwitch(R.id.switch_hide_shop, "tiktok_hide_shop")
        setupSwitch(R.id.switch_hide_live, "tiktok_hide_live")
        setupSwitch(R.id.switch_hide_story, "tiktok_hide_story")
        setupSwitch(R.id.switch_hide_image, "tiktok_hide_image")
        
        setupTextInput(R.id.input_min_max_views, "tiktok_min_max_views", "")
        setupTextInput(R.id.input_min_max_likes, "tiktok_min_max_likes", "")
        
        // 2. Feed Navigation
        setupSwitch(R.id.switch_feed_navigation, "tiktok_feed_navigation")
        setupSwitch(R.id.switch_bottom_navigation, "tiktok_bottom_navigation")
        setupSwitch(R.id.switch_hide_tako_ai, "tiktok_hide_tako_ai")
        
        // 3. Interface
        setupSwitch(R.id.switch_hide_captcha_popups, "tiktok_hide_captcha_popups")
        setupSwitch(R.id.switch_hide_homepage_coin, "tiktok_hide_homepage_coin")
        setupSwitch(R.id.switch_always_show_publish_date, "tiktok_always_show_publish_date")
        setupSwitch(R.id.switch_clear_display, "tiktok_clear_display")
        
        // 4. Comments
        setupSwitch(R.id.switch_comment_batch_translation, "tiktok_comment_batch_translation")
        setupSwitch(R.id.switch_hide_comment_quick_reactions, "tiktok_hide_comment_quick_reactions")
        setupSwitch(R.id.switch_copy_comments_without_username, "tiktok_copy_comments_without_username")
        
        // 5. Downloads
        setupSwitch(R.id.switch_download_watermark, "tiktok_download_watermark")
        setupSwitch(R.id.switch_force_download, "tiktok_force_download")
        setupSwitch(R.id.switch_custom_offline_videos, "tiktok_custom_offline_videos")
        
        // 6. Bypass Regional
        val switchSimSpoof = findViewById<MaterialSwitch>(R.id.switch_sim_spoof)
        val inputCountry = findViewById<TextInputEditText>(R.id.input_country)
        val inputMccMnc = findViewById<TextInputEditText>(R.id.input_mcc_mnc)
        val inputOperator = findViewById<TextInputEditText>(R.id.input_operator_name)
        
        switchSimSpoof.isChecked = prefs.getBoolean("tiktok_sim_spoof", false)
        inputCountry.setText(prefs.getString("tiktok_sim_country", "US"))
        inputMccMnc.setText(prefs.getString("tiktok_sim_mcc_mnc", "310260"))
        inputOperator.setText(prefs.getString("tiktok_sim_operator_name", "T-Mobile"))
        
        val updateEnabledState = { isEnabled: Boolean ->
            inputCountry.isEnabled = isEnabled
            inputMccMnc.isEnabled = isEnabled
            inputOperator.isEnabled = isEnabled
        }
        updateEnabledState(switchSimSpoof.isChecked)
        
        switchSimSpoof.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("tiktok_sim_spoof", isChecked).apply()
            updateEnabledState(isChecked)
        }
        
        setupTextInputWatcher(inputCountry, "tiktok_sim_country") { it?.uppercase() ?: "US" }
        setupTextInputWatcher(inputMccMnc, "tiktok_sim_mcc_mnc") { it ?: "310260" }
        setupTextInputWatcher(inputOperator, "tiktok_sim_operator_name") { it ?: "T-Mobile" }
        // App Behavior
        setupSwitch(R.id.switch_open_external_links, "tiktok_open_external_links")
        setupSwitch(R.id.switch_show_seekbar, "tiktok_show_seekbar")
        setupSwitch(R.id.switch_show_seekbar_thumbnail, "tiktok_show_seekbar_thumbnail")
        setupSwitch(R.id.switch_stop_video_looping, "tiktok_stop_video_looping")
        setupSwitch(R.id.switch_resume_video_after_scroll, "tiktok_resume_video_after_scroll")
        setupSwitch(R.id.switch_enable_long_press_speed_lock, "tiktok_enable_long_press_speed_lock")
        setupSwitch(R.id.switch_disable_long_press_quick_share, "tiktok_disable_long_press_quick_share")
        setupSwitch(R.id.switch_enable_non_personalized_search, "tiktok_enable_non_personalized_search")
        setupSwitch(R.id.switch_tiktok_experimental, "pref_tiktok_experimental")
    }
    
    private fun setupMenuRow(rowId: Int, targetLayoutId: Int, title: String) {
        val row = findViewById<View>(rowId)
        val targetLayout = findViewById<View>(targetLayoutId)
        row.setOnClickListener {
            layoutMainMenu.visibility = View.GONE
            targetLayout.visibility = View.VISIBLE
            currentSubMenu = targetLayout
            supportActionBar?.title = title
        }
    }
    
    private fun setupSwitch(switchId: Int, prefKey: String) {
        val switchView = findViewById<MaterialSwitch>(switchId)
        switchView.isChecked = prefs.getBoolean(prefKey, false)
        switchView.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(prefKey, isChecked).apply()
        }
    }
    
    private fun setupTextInput(inputId: Int, prefKey: String, defaultValue: String) {
        val inputView = findViewById<TextInputEditText>(inputId)
        inputView.setText(prefs.getString(prefKey, defaultValue))
        setupTextInputWatcher(inputView, prefKey) { it ?: defaultValue }
    }
    
    private fun setupTextInputWatcher(inputView: TextInputEditText, prefKey: String, transform: (String?) -> String) {
        inputView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString(prefKey, transform(s?.toString())).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onBackPressed() {
        if (currentSubMenu != null) {
            currentSubMenu?.visibility = View.GONE
            layoutMainMenu.visibility = View.VISIBLE
            currentSubMenu = null
            supportActionBar?.title = "Settings"
        } else {
            super.onBackPressed()
        }
    }
}
