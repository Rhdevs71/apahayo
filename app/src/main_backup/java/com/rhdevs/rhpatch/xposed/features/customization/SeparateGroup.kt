package com.rhdevs.rhpatch.xposed.features.customization

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.widget.BaseAdapter
import com.rhdevs.rhpatch.xposed.core.Feature
import com.rhdevs.rhpatch.xposed.core.components.FMessageWpp
import com.rhdevs.rhpatch.xposed.core.components.WaContactWpp
import com.rhdevs.rhpatch.xposed.core.db.MessageStore
import com.rhdevs.rhpatch.xposed.core.devkit.Unobfuscator
import com.rhdevs.rhpatch.xposed.core.devkit.UnobfuscatorCache
import com.rhdevs.rhpatch.xposed.utils.ReflectionUtils
import com.rhdevs.rhpatch.xposed.utils.Utils
import com.rhdevs.rhpatch.xposed.utils.WaeCoroutineExceptionHandler
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import android.content.SharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.luckypray.dexkit.query.enums.StringMatchType
import java.util.function.Predicate
import java.util.regex.Pattern

class SeparateGroup(loader: ClassLoader, preferences:SharedPreferences) :
    Feature(loader, preferences) {

    companion object {
        const val CHATS = 200
        const val STATUS = 300
        const val GROUPS = 500

        @JvmField
        var tabs = ArrayList<Int>()
        var tabInstances = HashMap<Int, Any>()

        fun resolveUserJid(chat: Any): FMessageWpp.UserJid? {
            try {
                val clazz = chat.javaClass
                val waContactField = ReflectionUtils.getFieldByExtendType(clazz, WaContactWpp.TYPE)
                if (waContactField != null) {
                    val waContactObj = waContactField.get(chat)
                    if (waContactObj != null) {
                        val waContact = WaContactWpp(waContactObj)
                        val userJid = waContact.userJid
                        if (!userJid.isNull) {
                            return userJid
                        }
                    }
                }
                val userJidField =
                    ReflectionUtils.getFieldByExtendType(clazz, FMessageWpp.UserJid.TYPE_JID)
                if (userJidField != null) {
                    val jidObject = userJidField.get(chat)
                    val userJid = FMessageWpp.UserJid(jidObject)
                    if (!userJid.isNull) {
                        return userJid
                    }
                }

                var jid = XposedHelpers.getObjectField(chat, "A00")
                if (jid == null) {
                    jid = XposedHelpers.getObjectField(chat, "A01")
                }
                if (jid != null) {
                    val userJid = FMessageWpp.UserJid(jid)
                    if (!userJid.isNull) {
                        return userJid
                    }
                }

                return null
            } catch (_: Throwable) {
                return null
            }
        }
    }

    override fun doHook() {
        val bottomNavigationViewCls = Unobfuscator.findFirstClassUsingName(
            classLoader,
            StringMatchType.EndsWith,
            ".BottomNavigationView"
        )
        XposedHelpers.findAndHookMethod(
            bottomNavigationViewCls,
            "getMaxItemCount",
            XC_MethodReplacement.returnConstant(99)
        )

        val groupsEnabled = prefs.getBoolean("separategroups", false)
        val foldersEnabled = prefs.getBoolean("custom_folders_enabled", false)
        if (!groupsEnabled && !foldersEnabled) return

        // Modifying tab list order
        hookTabList()

        // Setting group icon
        hookTabIcon()

        // Setting up fragments
        hookTabInstance()

        // Setting group tab name
        hookTabName()

        // Setting tab count
        hookTabCount()
    }

    override fun getPluginName(): String {
        return "Separate Group"
    }

    private fun hookTabCount() {
        val enableCountMethod = Unobfuscator.loadEnableCountTabMethod(classLoader)
        val badgeWrapperConstructor = Unobfuscator.loadEnableCountTabBadgeWrapper(classLoader)
        val badgeItemConstructor = Unobfuscator.loadEnableCountTabBadgeItem(classLoader)
        val emptyBadgeClass = Unobfuscator.loadEnableCountTabEmptyBadgeClass(classLoader)

        XposedBridge.hookMethod(enableCountMethod, object : XC_MethodHook() {
            @SuppressLint("Range", "Recycle")
            override fun beforeHookedMethod(param: MethodHookParam) {
                val indexTab = param.args[2] as Int
                if (indexTab != tabs.indexOf(CHATS)) return

                param.result = null

                CoroutineScope(Dispatchers.IO + WaeCoroutineExceptionHandler).launch {
                    val unseenChatCounts = getUnseenChatCounts()
                    withContext(Dispatchers.Main) {
                        if (tabs.contains(CHATS) && tabInstances.containsKey(CHATS)) {
                            val chatsBadge = if (unseenChatCounts.chatCount <= 0) {
                                XposedHelpers.getStaticObjectField(emptyBadgeClass, "A00")
                            } else {
                                val params = ReflectionUtils.initArray(badgeWrapperConstructor.parameterTypes)
                                params[0] = badgeItemConstructor.newInstance(unseenChatCounts.chatCount)
                                badgeWrapperConstructor.newInstance(*params)
                            }
                            XposedBridge.invokeOriginalMethod(
                                param.method,
                                param.thisObject,
                                arrayOf(param.args[0], chatsBadge, tabs.indexOf(CHATS))
                            )
                        }
                        if (tabs.contains(GROUPS) && tabInstances.containsKey(GROUPS)) {
                            val chatsBadge = if (unseenChatCounts.groupCount <= 0) {
                                XposedHelpers.getStaticObjectField(emptyBadgeClass, "A00")
                            } else {
                                val params = ReflectionUtils.initArray(badgeWrapperConstructor.parameterTypes)
                                params[0] = badgeItemConstructor.newInstance(unseenChatCounts.groupCount)
                                badgeWrapperConstructor.newInstance(*params)
                            }
                            XposedBridge.invokeOriginalMethod(
                                param.method,
                                param.thisObject,
                                arrayOf(param.args[0], chatsBadge, tabs.indexOf(GROUPS))
                            )
                        }
                    }
                }
            }
        })
    }

    fun getUnseenChatCounts(): UnseenChatCounts {
        val db = MessageStore.getInstance().getDatabase()
            ?: return UnseenChatCounts(chatCount = 0, groupCount = 0)

        val sql = """
        SELECT
            COALESCE(SUM(CASE WHEN jid.server = ? THEN 0 ELSE 1 END), 0) AS chat_count,
            COALESCE(SUM(CASE WHEN jid.server = ? THEN 1 ELSE 0 END), 0) AS group_count
        FROM chat
        INNER JOIN jid ON jid._id = chat.jid_row_id
        WHERE chat.unseen_message_count <> 0
          AND chat.archived = 0
          AND chat.chat_lock = 0
          AND (chat.group_type = 0 OR chat.group_type = 6)
        """.trimIndent()

        db.rawQuery(sql, arrayOf("g.us", "g.us")).use { cursor ->
            if (!cursor.moveToFirst()) {
                return UnseenChatCounts(chatCount = 0, groupCount = 0)
            }

            return UnseenChatCounts(
                chatCount = cursor.getInt(cursor.getColumnIndexOrThrow("chat_count")),
                groupCount = cursor.getInt(cursor.getColumnIndexOrThrow("group_count"))
            )
        }
    }

    private fun hookTabIcon() {
        val iconTabMethod = Unobfuscator.loadIconTabMethod(classLoader)
        val menuAddAndroidX = Unobfuscator.loadAddMenuAndroidX(classLoader)

        XposedBridge.hookMethod(iconTabMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val hooked = XposedBridge.hookMethod(menuAddAndroidX, object : XC_MethodHook() {
                    override fun afterHookedMethod(innerParam: MethodHookParam) {
                        val tabId = innerParam.args[1] as? Int ?: return
                        if (tabId == GROUPS) {
                            val menuItem = innerParam.result as MenuItem
                            menuItem.setIcon(Utils.getID("home_tab_communities_selector", "drawable"))
                        } else if (tabId >= 1000) {
                            // Custom folders use folder drawable
                            val menuItem = innerParam.result as MenuItem
                            menuItem.setIcon(Utils.getID("ic_restore", "drawable")) // Folder-like icon
                        }
                    }
                })
                param.setObjectExtra("hooked", hooked)
            }

            @SuppressLint("ResourceType")
            override fun afterHookedMethod(param: MethodHookParam) {
                val hooked = param.getObjectExtra("hooked") as? Unhook
                hooked?.unhook()
            }
        })
    }

    @SuppressLint("ResourceType")
    private fun hookTabName() {
        val tabNameMethod = Unobfuscator.loadTabNameMethod(classLoader)
        XposedBridge.hookMethod(tabNameMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val tabId = param.args[0] as Int
                if (tabId == GROUPS) {
                    param.result = UnobfuscatorCache.getInstance().getString("groups")
                } else if (tabId >= 1000) {
                    val index = tabId - 1000
                    val foldersJson = prefs.getString("custom_folders", "[]") ?: "[]"
                    try {
                        val array = JSONArray(foldersJson)
                        if (index < array.length()) {
                            param.result = array.getJSONObject(index).optString("name", "Folder")
                        }
                    } catch (_: Exception) {
                        param.result = "Folder"
                    }
                }
            }
        })
    }

    private fun hookTabInstance() {
        val cFragClass = Unobfuscator.findFirstClassUsingName(
            classLoader,
            StringMatchType.EndsWith,
            ".ConversationsFragment"
        )

        val getTabMethod = Unobfuscator.loadGetTabMethod(classLoader)
        val methodTabInstance = Unobfuscator.loadTabFragmentMethod(classLoader)
        val recreateFragmentMethod = Unobfuscator.loadRecreateFragmentConstructor(classLoader)
        val pattern = Pattern.compile("android:switcher:\\d+:(\\d+)")
        val fragmentClass = Unobfuscator.loadFragmentClass(classLoader)

        XposedBridge.hookMethod(recreateFragmentMethod, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val string: String
                val arg0 = param.args[0]
                if (arg0 is Bundle) {
                    @Suppress("DEPRECATION")
                    val state = arg0.getParcelable<android.os.Parcelable>("state") ?: return
                    string = state.toString()
                } else {
                    string = param.args[2].toString()
                }
                val matcher = pattern.matcher(string)
                if (matcher.find()) {
                    val tabId = matcher.group(1)?.toIntOrNull() ?: return
                    if (tabId == GROUPS || tabId == CHATS || tabId >= 1000) {
                        val fragmentField = ReflectionUtils.getFieldByType(
                            param.thisObject.javaClass,
                            fragmentClass
                        )
                        val convFragment =
                            ReflectionUtils.getObjectField(fragmentField, param.thisObject)
                        tabInstances.remove(tabId)
                        if (convFragment != null) {
                            tabInstances[tabId] = convFragment
                        }
                    }
                }
            }
        })

        XposedBridge.hookMethod(getTabMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val tabId = tabs[param.args[0] as Int]
                if (tabId == GROUPS || tabId == CHATS || tabId >= 1000) {
                    val convFragment = cFragClass.declaredConstructors.first {
                        it.parameterCount == 0
                    }.newInstance()
                    param.result = convFragment
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val tabId = tabs[param.args[0] as Int]
                tabInstances.remove(tabId)
                param.result?.let { tabInstances[tabId] = it }
            }
        })

        XposedBridge.hookMethod(methodTabInstance, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val chatsList = param.result as List<*>
                val resultList = filterChat(param.thisObject, chatsList)
                param.result = resultList
            }
        })

        val fabintMethod = Unobfuscator.loadFabMethod(classLoader)

        XposedBridge.hookMethod(fabintMethod, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (tabInstances[GROUPS] == param.thisObject) {
                    param.result = GROUPS
                } else {
                    for (entry in tabInstances.entries) {
                        if (entry.value == param.thisObject && entry.key >= 1000) {
                            param.result = entry.key
                            break
                        }
                    }
                }
            }
        })

        val publishResultsMethod = Unobfuscator.loadGetFiltersMethod(classLoader)

        XposedBridge.hookMethod(publishResultsMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val filters = param.args[1]
                val chatsList = XposedHelpers.getObjectField(filters, "values") as List<*>
                val baseField = ReflectionUtils.getFieldByExtendType(
                    publishResultsMethod.declaringClass,
                    BaseAdapter::class.java
                ) ?: return
                val convField = ReflectionUtils.getFieldByType(baseField.type, cFragClass)
                val thiz = convField!!.get(baseField.get(param.thisObject)) ?: return
                val resultList = filterChat(thiz, chatsList)
                XposedHelpers.setObjectField(filters, "values", resultList)
                XposedHelpers.setIntField(filters, "count", resultList.size)
            }
        })
    }

    private fun filterChat(thiz: Any, chatsList: List<*>): List<*> {
        val tabChat = tabInstances[CHATS]
        val tabGroup = tabInstances[GROUPS]

        var activeFolderTabId: Int? = null
        for (entry in tabInstances.entries) {
            if (entry.value == thiz && entry.key >= 1000) {
                activeFolderTabId = entry.key
                break
            }
        }

        if (tabChat != thiz && tabGroup != thiz && activeFolderTabId == null) {
            return chatsList
        }

        val foldersEnabled = prefs.getBoolean("custom_folders_enabled", false)

        val editableChatList = ArrayListFilter(
            { userJid ->
                when {
                    foldersEnabled && activeFolderTabId != null -> {
                        val folderIndex = activeFolderTabId - 1000
                        val foldersJson = prefs.getString("custom_folders", "[]") ?: "[]"
                        var folderContacts = ""
                        try {
                            val array = JSONArray(foldersJson)
                            if (folderIndex < array.length()) {
                                folderContacts = array.getJSONObject(folderIndex).optString("contacts", "")
                            }
                        } catch (_: Exception) {}
                        val targetList = folderContacts.split(",").map { it.trim() }
                        targetList.contains(userJid.phoneRawString ?: "")
                    }
                    tabGroup == thiz -> userJid.isGroup || userJid.isBroadcast
                    else -> userJid.isContact
                }
            },
            false
        )
        @Suppress("UNCHECKED_CAST")
        editableChatList.addAll(chatsList as Collection<Any>)
        return editableChatList
    }

    private fun hookTabList() {
        val onCreateTabList = Unobfuscator.loadTabListMethod(classLoader)

        XposedBridge.hookMethod(onCreateTabList, object : XC_MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun afterHookedMethod(param: MethodHookParam) {
                val resultTabs = param.result as? ArrayList<Int> ?: return
                tabs = resultTabs

                val groupsEnabled = prefs.getBoolean("separategroups", false)
                if (groupsEnabled && !tabs.contains(GROUPS)) {
                    tabs.add(if (tabs.isEmpty()) 0 else 1, GROUPS)
                }

                val foldersEnabled = prefs.getBoolean("custom_folders_enabled", false)
                if (foldersEnabled) {
                    val foldersJson = prefs.getString("custom_folders", "[]") ?: "[]"
                    try {
                        val array = JSONArray(foldersJson)
                        for (i in 0 until array.length()) {
                            val folderTabId = 1000 + i
                            if (!tabs.contains(folderTabId)) {
                                tabs.add(folderTabId)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        })
    }

    class ArrayListFilter(
        private val filter: Predicate<FMessageWpp.UserJid>,
        private val includeWhenUnresolved: Boolean,
    ) : ArrayList<Any>() {

        fun addAllFromList(elements: List<*>) {
            for (chat in elements) {
                if (chat != null && shouldInclude(chat)) {
                    super.add(chat)
                }
            }
        }

        override fun add(index: Int, element: Any) {
            if (shouldInclude(element)) {
                super.add(index, element)
            }
        }

        override fun add(element: Any): Boolean {
            if (shouldInclude(element)) {
                return super.add(element)
            }
            return true
        }

        override fun addAll(elements: Collection<Any>): Boolean {
            for (chat in elements) {
                if (shouldInclude(chat)) {
                    super.add(chat)
                }
            }
            return true
        }

        private fun shouldInclude(chat: Any): Boolean {
            val userJid = resolveUserJid(chat) ?: return includeWhenUnresolved
            return filter.test(userJid)
        }
    }

    data class UnseenChatCounts(
        val chatCount: Int,
        val groupCount: Int
    )
}
