package com.rhdevs.rhpatch.xposed.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.crossbowffs.remotepreferences.RemotePreferences
import com.rhdevs.rhpatch.App
import com.rhdevs.rhpatch.BuildConfig
import com.rhdevs.rhpatch.R
import com.rhdevs.rhpatch.UpdateChecker
import com.rhdevs.rhpatch.WppXposed
import com.rhdevs.rhpatch.activities.CrashReportActivity
import com.rhdevs.rhpatch.xposed.core.components.AlertDialogWpp
import com.rhdevs.rhpatch.xposed.core.components.FMessageWpp
import com.rhdevs.rhpatch.xposed.core.components.FStatusWpp
import com.rhdevs.rhpatch.xposed.core.components.ProtocolTreeNodeWpp
import com.rhdevs.rhpatch.xposed.core.components.SharedPreferencesWrapper
import com.rhdevs.rhpatch.xposed.core.components.WaContactWpp
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator
import com.rhdevs.rhpatch.xposed.core.devkit.UnobfuscatorCache
import com.rhdevs.rhpatch.xposed.features.customization.BubbleColors
import com.rhdevs.rhpatch.xposed.features.customization.ContactVerify
import com.rhdevs.rhpatch.xposed.features.customization.CustomThemeV2
import com.rhdevs.rhpatch.xposed.features.customization.CustomTime
import com.rhdevs.rhpatch.xposed.features.customization.CustomToolbar
import com.rhdevs.rhpatch.xposed.features.customization.CustomView
import com.rhdevs.rhpatch.xposed.features.customization.DefaultEmoji
import com.rhdevs.rhpatch.xposed.features.customization.FilterGroups
import com.rhdevs.rhpatch.xposed.features.customization.FloatingBottomBar
import com.rhdevs.rhpatch.xposed.features.customization.HideSeenView
import com.rhdevs.rhpatch.xposed.features.customization.HideTabs
import com.rhdevs.rhpatch.xposed.features.customization.IGStatus
import com.rhdevs.rhpatch.xposed.features.customization.SeparateGroup
import com.rhdevs.rhpatch.xposed.features.customization.ShowOnline
import com.rhdevs.rhpatch.xposed.features.general.AboutContactPicker
import com.rhdevs.rhpatch.xposed.features.general.AntiRevoke
import com.rhdevs.rhpatch.xposed.features.general.MessageSchedulerHook
import com.rhdevs.rhpatch.xposed.features.general.AutoReplyHook
import com.rhdevs.rhpatch.xposed.features.general.QuickTranslateHook
import com.rhdevs.rhpatch.xposed.features.general.VoiceChangerHook
import com.rhdevs.rhpatch.xposed.features.general.ScreenSecurityHook
import com.rhdevs.rhpatch.xposed.features.general.CallType
import com.rhdevs.rhpatch.xposed.features.general.ChatLimit
import com.rhdevs.rhpatch.xposed.features.general.DeleteStatus
import com.rhdevs.rhpatch.xposed.features.general.NewChat
import com.rhdevs.rhpatch.xposed.features.general.Others
import com.rhdevs.rhpatch.xposed.features.general.PinnedLimit
import com.rhdevs.rhpatch.xposed.features.general.SeenTick
import com.rhdevs.rhpatch.xposed.features.general.ShareLimit
import com.rhdevs.rhpatch.xposed.features.general.ShowEditMessage
import com.rhdevs.rhpatch.xposed.features.general.Tasker
import com.rhdevs.rhpatch.xposed.features.listeners.ContactItemListener
import com.rhdevs.rhpatch.xposed.features.listeners.ConversationItemListener
import com.rhdevs.rhpatch.xposed.features.listeners.MenuStatusListener
import com.rhdevs.rhpatch.xposed.features.media.CallRecording
import com.rhdevs.rhpatch.xposed.features.media.VideoCallRecorder
import com.rhdevs.rhpatch.xposed.features.media.DownloadProfile
import com.rhdevs.rhpatch.xposed.features.media.DownloadViewOnce
import com.rhdevs.rhpatch.xposed.features.media.MediaPreview
import com.rhdevs.rhpatch.xposed.features.media.MediaQuality
import com.rhdevs.rhpatch.xposed.features.media.StatusDownload
import com.rhdevs.rhpatch.xposed.features.others.ActivityController
import com.rhdevs.rhpatch.xposed.features.others.AudioTranscript
import com.rhdevs.rhpatch.xposed.features.others.BackupRestore

import com.rhdevs.rhpatch.xposed.features.others.Channels
import com.rhdevs.rhpatch.xposed.features.others.ChatFilters
import com.rhdevs.rhpatch.xposed.features.others.CopySelectionMessage
import com.rhdevs.rhpatch.xposed.features.others.CopyStatus
import com.rhdevs.rhpatch.xposed.features.others.DebugFeature
import com.rhdevs.rhpatch.xposed.features.others.UnlockPremium
import com.rhdevs.rhpatch.xposed.features.others.GoogleTranslate
import com.rhdevs.rhpatch.xposed.features.others.GroupAdmin
import com.rhdevs.rhpatch.xposed.features.others.JumpFirstMessage
import com.rhdevs.rhpatch.xposed.features.others.MenuHome
import com.rhdevs.rhpatch.xposed.features.others.Stickers
import com.rhdevs.rhpatch.xposed.features.others.TextStatusComposer
import com.rhdevs.rhpatch.xposed.features.others.ToastViewer
import com.rhdevs.rhpatch.xposed.features.privacy.AntiWa
import com.rhdevs.rhpatch.xposed.features.privacy.CallPrivacy
import com.rhdevs.rhpatch.xposed.features.privacy.CustomPrivacy
import com.rhdevs.rhpatch.xposed.features.privacy.DndMode
import com.rhdevs.rhpatch.xposed.features.privacy.FreezeLastSeen
import com.rhdevs.rhpatch.xposed.features.privacy.HideChat
import com.rhdevs.rhpatch.xposed.features.privacy.HideSeen
import com.rhdevs.rhpatch.xposed.features.privacy.LockedChatsEnhancer
import com.rhdevs.rhpatch.xposed.features.privacy.TagMessage
import com.rhdevs.rhpatch.xposed.features.privacy.TypingPrivacy
import com.rhdevs.rhpatch.xposed.features.privacy.ViewOnce
import com.rhdevs.rhpatch.xposed.spoofer.HookBL
import com.rhdevs.rhpatch.xposed.utils.DesignUtils
import com.rhdevs.rhpatch.xposed.utils.ReflectionUtils
import com.rhdevs.rhpatch.xposed.utils.Utils
import de.robv.android.xposed.SELinuxHelper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FeatureLoader {

    companion object {
        @JvmField
        var mApp: Application? = null

        lateinit var moduleContext: Context

        const val PACKAGE_WPP = "com.whatsapp"
        const val PACKAGE_BUSINESS = "com.whatsapp.w4b"

        private val list = ArrayList<ErrorItem>()
        private var supportedVersions: List<String>? = null
        private var currentVersion: String? = null
        private var crashHandlerInstalled = false
        private const val UPDATE_CHECK_COOLDOWN_MS = 60_000L
        private var lastUpdateCheckScheduledAt: Long = 0L

        @JvmStatic
        fun start(loader: ClassLoader, sourceDir: String) {
            if (!Unobfuscator.initWithPath(sourceDir)) {
                XposedBridge.log("Can't init dexkit")
                return
            }

            Utils.appClassLoader = loader

            XposedHelpers.findAndHookMethod(
                Instrumentation::class.java, "callApplicationOnCreate", Application::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        mApp = param.args[0] as Application
                        val application = mApp!!
                        val pref = getPreferences(application)
                        Feature.DEBUG = pref.getBoolean("enablelogs", true)
                        Utils.xprefs = pref

                        if (pref.getBoolean("bootloader_spoofer", false)) {
                            HookBL.hook(loader, pref)
                            XposedBridge.log("Bootloader Spoofer is Injected")
                        }

                        val packageManager = application.packageManager
                        val packageInfo = packageManager.getPackageInfo(application.packageName, 0)
                        XposedBridge.log(packageInfo.versionName)
                        currentVersion = packageInfo.versionName
                        installCrashHandler(application, packageInfo.versionName.orEmpty())

                        try {
                            initializeModuleContext()
                            
                            val resIdArray = if (application.packageName == PACKAGE_WPP)
                                R.array.supported_versions_wpp
                            else
                                R.array.supported_versions_business

                            supportedVersions =
                                moduleContext.resources.getStringArray(resIdArray).toList()
                                
                            application.registerActivityLifecycleCallbacks(WaCallback())
                            registerReceivers()

                            val timeMillis = System.currentTimeMillis()
                            UnobfuscatorCache.init(application)
                            SharedPreferencesWrapper.hookInit(application.classLoader)
                            ReflectionUtils.initCache(application)

                            val isSupported = supportedVersions?.any { s ->
                                packageInfo.versionName?.startsWith(s.replace(".xx", "")) ?: false
                            } ?: false

                            if (!isSupported) {
                                disableExpirationVersion(application.classLoader)
                                if (!pref.getBoolean("bypass_version_check", false)) {
                                    val errorMsg = """
                                        Unsupported version: ${packageInfo.versionName}
                                        Only the function of ignoring the expiration of the WhatsApp version has been applied!
                                    """.trimIndent()
                                    throw Exception(errorMsg)
                                }
                            }

                            initComponents(loader, pref)
                            plugins(loader, pref, packageInfo.versionName!!)
                            sendEnabledBroadcast(application)

                            val totalTime = System.currentTimeMillis() - timeMillis
                            XposedBridge.log("Loaded Hooks in ${totalTime}ms")

                        } catch (e: Throwable) {
                            XposedBridge.log(e)
                            val error = ErrorItem().apply {
                                pluginName = "MainFeatures[Critical]"
                                whatsAppVersion = packageInfo.versionName
                                moduleVersion = BuildConfig.VERSION_NAME
                                message = e.message
                                errorDetail = e.stackTrace
                                    .filter { s ->
                                        !s.className.startsWith("android") && !s.className.startsWith(
                                            "com.android"
                                        )
                                    }
                                    .joinToString(prefix = "[", postfix = "]")
                            }
                            list.add(error)
                        }
                    }
                })

            XposedHelpers.findAndHookMethod(
                Activity::class.java, "onCreate", Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.thisObject.javaClass.simpleName != "HomeActivity") return
                        if (list.isNotEmpty()) {
                            val activity = param.thisObject as Activity
                            val msg = list.joinToString("\n") { "${it.pluginName} - ${it.message}" }

                            AlertDialogWpp(activity)
                                .setTitle(com.rhdevs.rhpatch.xposed.core.FeatureLoader.moduleContext.getString(R.string.error_detected))
                                .setMessage(
                                    "${com.rhdevs.rhpatch.xposed.core.FeatureLoader.moduleContext.getString(R.string.version_error)}$msg\n\nCurrent Version: $currentVersion\nSupported Versions:\n${
                                        supportedVersions?.joinToString(
                                            "\n"
                                        )
                                    }"
                                )
                                .setPositiveButton(com.rhdevs.rhpatch.xposed.core.FeatureLoader.moduleContext.getString(R.string.copy_to_clipboard)) { dialog, _ ->
                                    val clipboard =
                                        mApp?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "text",
                                        list.joinToString("\n") { it.toString() })
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(
                                        mApp,
                                        com.rhdevs.rhpatch.xposed.core.FeatureLoader.moduleContext.getString(R.string.copied_to_clipboard),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dialog.dismiss()
                                }
                                .show()
                        }
                    }
                })
        }

        private fun getPreferences(context: Context): SharedPreferences {
            val pref = WppXposed.getPref()
            pref.reload()
            try {
                val fileCanRead =
                    SELinuxHelper.getAppDataFileService().checkFileAccess(pref.file.absolutePath, 4)
                if (fileCanRead) {
                    return pref
                }
            } catch (e: Exception) {
                XposedBridge.log(e)
            }
            XposedBridge.log("XSharedPreferences not accessible, using RemotePreferences fallback")
            return RemotePreferences(
                context,
                BuildConfig.APPLICATION_ID + ".preferences",
                BuildConfig.APPLICATION_ID + "_preferences"
            )
        }

        private fun initializeModuleContext() {
            try {
                val context = mApp!!.createPackageContext(
                    BuildConfig.APPLICATION_ID,
                    Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
                )
                moduleContext = android.view.ContextThemeWrapper(context, R.style.AppTheme)
            } catch (_: PackageManager.NameNotFoundException) {
                throw PackageManager.NameNotFoundException(Utils.application.getString(R.string.alert_module_notfound))
            }
        }

        private fun installCrashHandler(application: Application, whatsAppVersion: String) {
            if (crashHandlerInstalled) return
            crashHandlerInstalled = true

            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    de.robv.android.xposed.XposedBridge.log(throwable)
                    val isMainThread = android.os.Looper.getMainLooper().thread == thread
                    val isFatalSystemError = throwable is Error
                    if (!isMainThread && !isFatalSystemError) {
                        previousHandler?.uncaughtException(thread, throwable)
                        return@setDefaultUncaughtExceptionHandler
                    }
                    val crashInfo = buildCrashInfo(application, whatsAppVersion)
                    val intent = Intent().apply {
                        component = ComponentName(
                            BuildConfig.APPLICATION_ID,
                            CrashReportActivity::class.java.name
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(CrashReportActivity.EXTRA_CRASH_INFO, crashInfo)
                        putExtra(
                            CrashReportActivity.EXTRA_CRASH_TRACE,
                            Log.getStackTraceString(throwable)
                        )
                    }
                    application.startActivity(intent)
                } catch (e: Throwable) {
                    XposedBridge.log(e)
                } finally {
                    if (previousHandler != null) {
                        previousHandler.uncaughtException(thread, throwable)
                    } else {
                        Runtime.getRuntime().exit(2)
                    }
                }
            }
        }

        private fun buildCrashInfo(application: Application, whatsAppVersion: String): String {
            val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            val deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" ")

            return listOf(
                "${application.getString(R.string.whatsapp_version)}: $whatsAppVersion",
                "${application.getString(R.string.whatsapp_package)}: ${application.packageName}",
                "${application.getString(R.string.wae_version)}: ${BuildConfig.VERSION_NAME}",
                "${application.getString(R.string.crash_android_version)}: $androidVersion",
                "${application.getString(R.string.device_model)}: $deviceModel"
            ).joinToString("\n")
        }


        @JvmStatic
        @Throws(Exception::class)
        fun disableExpirationVersion(classLoader: ClassLoader) {
            val expirationClass = Unobfuscator.loadExpirationClass(classLoader)
            val methods =
                ReflectionUtils.findAllMethodsUsingFilter(expirationClass) { m -> m.returnType == Date::class.java }
            for (method in methods) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val calendar = Calendar.getInstance().apply {
                            set(2099, 11, 31)
                        }
                        param.result = calendar.time
                    }
                })
            }
        }

        @Throws(Exception::class)
        private fun initComponents(loader: ClassLoader, pref: SharedPreferences) {
            FMessageWpp.initialize(loader)
            FStatusWpp.initialize(loader)
            ProtocolTreeNodeWpp.initialize(loader)
            AlertDialogWpp.initDialog(loader)
            WaContactWpp.initialize(loader)
            WppCore.initialize(loader, pref)
            DesignUtils.setPrefs(pref)
            Utils.init()

            WppCore.addListenerActivity { activity, type ->
                if (type == WppCore.ActivityChangeState.ChangeType.RESUMED) {
                    checkUpdate(activity)
                }


                if (App.isOriginalPackage && pref.getBoolean("update_check", true)) {
                    if (activity.javaClass.simpleName == "HomeActivity" && type == WppCore.ActivityChangeState.ChangeType.RESUMED) {
                        val now = System.currentTimeMillis()
                        val shouldSchedule = synchronized(FeatureLoader::class.java) {
                            if (now - lastUpdateCheckScheduledAt < UPDATE_CHECK_COOLDOWN_MS) {
                                false
                            } else {
                                lastUpdateCheckScheduledAt = now
                                true
                            }
                        }
                        if (shouldSchedule) {
                            activity.window.decorView.postDelayed({
                                CompletableFuture.runAsync(UpdateChecker(activity))
                            }, 2000)
                        }
                    }
                }
            }

        }

        private fun checkUpdate(activity: Activity) {
            if (WppCore.getPrivBoolean("need_restart", false)) {
                WppCore.setPrivBoolean("need_restart", false)
                try {
                    AlertDialogWpp(activity)
                        .setMessage(com.rhdevs.rhpatch.xposed.core.FeatureLoader.moduleContext.getString(R.string.restart_wpp))
                        .setPositiveButton(com.rhdevs.rhpatch.xposed.core.FeatureLoader.moduleContext.getString(R.string.yes)) { _, _ ->
                            if (!Utils.doRestart(activity)) {
                                Toast.makeText(
                                    activity,
                                    "Unable to rebooting activity",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .setNegativeButton(com.rhdevs.rhpatch.xposed.core.FeatureLoader.moduleContext.getString(R.string.no), null)
                        .show()
                } catch (_: Throwable) {
                }
            }
        }

        @SuppressLint("WrongConstant")
        private fun registerReceivers() {
            val app = mApp ?: return

            // Reboot receiver
            val restartReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (context.packageName == intent.getStringExtra("PKG")) {
                        val appName =
                            context.packageManager.getApplicationLabel(context.applicationInfo)
                        Toast.makeText(
                            context,
                            "${moduleContext.getString(R.string.rebooting)} $appName...",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (!Utils.doRestart(context)) {
                            Toast.makeText(
                                context,
                                "Unable to rebooting $appName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            ContextCompat.registerReceiver(
                app, restartReceiver,
                IntentFilter("${BuildConfig.APPLICATION_ID}.WHATSAPP.RESTART"),
                ContextCompat.RECEIVER_EXPORTED
            )

            // Wpp receiver
            val wppReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    sendEnabledBroadcast(context)
                }
            }
            ContextCompat.registerReceiver(
                app, wppReceiver,
                IntentFilter("${BuildConfig.APPLICATION_ID}.CHECK_WPP"),
                ContextCompat.RECEIVER_EXPORTED
            )

            // Dialog receiver restart
            val restartManualReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    WppCore.setPrivBoolean("need_restart", true)
                }
            }
            ContextCompat.registerReceiver(
                app, restartManualReceiver,
                IntentFilter("${BuildConfig.APPLICATION_ID}.MANUAL_RESTART"),
                ContextCompat.RECEIVER_EXPORTED
            )
        }

        private fun sendEnabledBroadcast(context: Context) {
            try {
                val wppIntent = Intent("${BuildConfig.APPLICATION_ID}.RECEIVER_WPP").apply {
                    putExtra(
                        "VERSION",
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    )
                    putExtra("PKG", context.packageName)
                    setPackage(BuildConfig.APPLICATION_ID)
                }
                context.sendBroadcast(wppIntent)
            } catch (_: Exception) {
            }
        }

        @Throws(Exception::class)
        private fun plugins(loader: ClassLoader, pref: SharedPreferences, versionWpp: String) {
            val classes = arrayOf(
                DebugFeature::class.java,
                UnlockPremium::class.java,
                ContactItemListener::class.java,
                ConversationItemListener::class.java,
                MenuStatusListener::class.java,
                ShowEditMessage::class.java,
                AntiRevoke::class.java,
                CustomToolbar::class.java,
                CustomView::class.java,
                SeenTick::class.java,
                BubbleColors::class.java,
                CallPrivacy::class.java,
                // com.rhdevs.rhpatch.xposed.features.privacy.MessageBlocker::class.java, // Removed SQL Hook
                ActivityController::class.java,
                CustomThemeV2::class.java,
                com.rhdevs.rhpatch.xposed.features.customization.BubbleTickStyles::class.java,
                FloatingBottomBar::class.java,
                ChatLimit::class.java,
                SeparateGroup::class.java,
                ShowOnline::class.java,
                DndMode::class.java,
                FreezeLastSeen::class.java,
                TypingPrivacy::class.java,
                HideChat::class.java,
                HideSeen::class.java,
                HideSeenView::class.java,
                TagMessage::class.java,
                HideTabs::class.java,
                IGStatus::class.java,
                MediaQuality::class.java,
                NewChat::class.java,
                Others::class.java,
                PinnedLimit::class.java,
                CustomTime::class.java,
                ShareLimit::class.java,
                StatusDownload::class.java,
                ViewOnce::class.java,
                CallType::class.java,
                MediaPreview::class.java,
                FilterGroups::class.java,
                Tasker::class.java,
                DeleteStatus::class.java,
                DownloadViewOnce::class.java,
                Channels::class.java,
                DownloadProfile::class.java,
                ChatFilters::class.java,
                GroupAdmin::class.java,
                Stickers::class.java,
                CopyStatus::class.java,
                CopySelectionMessage::class.java,
                TextStatusComposer::class.java,
                ToastViewer::class.java,
                MenuHome::class.java,
                AntiWa::class.java,
                CustomPrivacy::class.java,
                AudioTranscript::class.java,
                GoogleTranslate::class.java,
                ContactVerify::class.java,
                LockedChatsEnhancer::class.java,
                CallRecording::class.java,
                VideoCallRecorder::class.java,
                BackupRestore::class.java,
                JumpFirstMessage::class.java,
                AboutContactPicker::class.java,
                DefaultEmoji::class.java,
                MessageSchedulerHook::class.java,
                AutoReplyHook::class.java,
                QuickTranslateHook::class.java,
                VoiceChangerHook::class.java,
                ScreenSecurityHook::class.java
            )

            XposedBridge.log("Loading Plugins")
            val executorService = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "WAE-HookInstaller").apply {
                    isDaemon = true
                }
            }
            val times = Collections.synchronizedList(ArrayList<String>())

            for (clazz in classes) {
                CompletableFuture.runAsync({
                    val startTime = System.currentTimeMillis()
                    try {
                        val constructor = clazz.getConstructor(
                            ClassLoader::class.java,
                            SharedPreferences::class.java
                        )
                        val plugin = constructor.newInstance(loader, pref) as Feature
                        plugin.doHook()
                    } catch (e: Throwable) {
                        XposedBridge.log(e)
                        val error = ErrorItem().apply {
                            pluginName = clazz.simpleName
                            whatsAppVersion = versionWpp
                            moduleVersion = BuildConfig.VERSION_NAME
                            message = e.message
                            errorDetail = e.stackTrace
                                .filter { s ->
                                    !s.className.startsWith("android") && !s.className.startsWith(
                                        "com.android"
                                    )
                                }
                                .joinToString(prefix = "[", postfix = "]")
                        }
                        list.add(error)
                    }
                    val duration = System.currentTimeMillis() - startTime
                    times.add("* Loaded Plugin ${clazz.simpleName} in ${duration}ms")
                }, executorService)
            }

            executorService.shutdown()
            executorService.awaitTermination(15, TimeUnit.SECONDS)

            if (Feature.DEBUG) {
                val loadedTimes = synchronized(times) { times.toList() }
                loadedTimes.forEach { XposedBridge.log(it) }
            }
        }
    }

    private class ErrorItem {
        var pluginName: String? = null
        var whatsAppVersion: String? = null
        var errorDetail: String? = null
        var moduleVersion: String? = null
        var message: String? = null

        override fun toString(): String {
            return """
                pluginName='$pluginName'
                moduleVersion='$moduleVersion'
                whatsAppVersion='$whatsAppVersion'
                Message=$message
                error='$errorDetail'
            """.trimIndent()
        }
    }
}

