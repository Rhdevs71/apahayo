package com.wmods.wppenhacer.xposed.features.general

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.wmods.wppenhacer.xposed.core.Feature
import com.wmods.wppenhacer.xposed.utils.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class QuickTranslateHook(loader: ClassLoader, preferences: SharedPreferences) : Feature(loader, preferences) {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun getPluginName(): String {
        return "QuickTranslateHook"
    }

    override fun doHook() {
        val enabled = prefs.getBoolean("quick_translate_enabled", false)
        if (!enabled) return

        XposedHelpers.findAndHookMethod(
            Activity::class.java,
            "onCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    if (activity.javaClass.name.contains("Conversation")) {
                        // Delay slightly to ensure views are fully inflated
                        mainHandler.postDelayed({
                            injectTranslateButton(activity)
                        }, 500)
                    }
                }
            }
        )
    }

    private fun injectTranslateButton(activity: Activity) {
        try {
            val packageName = activity.packageName
            val entryId = activity.resources.getIdentifier("entry", "id", packageName)
            val entryView = activity.findViewById<View>(entryId) as? EditText ?: return

            val parent = entryView.parent as? ViewGroup ?: return

            // Check if already injected to avoid duplicates
            if (parent.findViewWithTag<View>("wpp_translate_btn") != null) return

            val context = activity
            val translateBtn = ImageButton(context).apply {
                tag = "wpp_translate_btn"
                // Use standard Android search icon
                setImageResource(android.R.drawable.ic_menu_search)
                background = null
                setPadding(8, 8, 8, 8)
                contentDescription = "Translate Message"
            }

            // Insert button into the text entry container
            parent.addView(translateBtn, parent.indexOfChild(entryView) + 1)

            translateBtn.setOnClickListener {
                val textToTranslate = entryView.text.toString().trim()
                if (textToTranslate.isEmpty()) {
                    Toast.makeText(context, "Enter text first", Toast.LENGTH_SHORT).show()
                    return
                }

                val targetLang = prefs.getString("quick_translate_lang", "en") ?: "en"
                Toast.makeText(context, "Translating to $targetLang...", Toast.LENGTH_SHORT).show()

                executor.execute {
                    val translated = translateText(textToTranslate, targetLang)
                    mainHandler.post {
                        if (translated != null) {
                            entryView.setText(translated)
                            entryView.setSelection(translated.length)
                        } else {
                            Toast.makeText(context, "Translation failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            XposedBridge.log("QuickTranslate Error injecting button: ${e.message}")
        }
    }

    private fun translateText(text: String, targetLang: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encodedText"
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val sentencesArray = jsonArray.getJSONArray(0)
                val result = StringBuilder()
                for (i in 0 until sentencesArray.length()) {
                    val sentence = sentencesArray.getJSONArray(i)
                    result.append(sentence.getString(0))
                }
                result.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }
}
