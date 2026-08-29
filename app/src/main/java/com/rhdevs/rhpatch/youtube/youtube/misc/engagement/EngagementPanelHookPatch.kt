package com.rhdevs.rhpatch.youtube.youtube.misc.engagement


import com.rhdevs.rhpatch.youtube.extension.youtube.shared.EngagementPanel
import com.rhdevs.rhpatch.youtube.youtube.shared.EngagementPanelControllerFingerprint
import com.rhdevs.rhpatch.patch

typealias EngagementPanelIdHook = (String?) -> Boolean

private val engagementPanelIdHooks = mutableListOf<EngagementPanelIdHook>()

val EngagementPanelHook = patch(
    description = "Hook to get the current engagement panel state.",
) {
    val panelId = ThreadLocal<String?>()
    ::panelInitFingerprint.hookMethod {
        after {
            panelId.set(it.args[0] as String?)
        }
    }
    EngagementPanelControllerFingerprint.hookMethod {
        after { param ->
            val id = panelId.get()
            engagementPanelIdHooks.forEach { hook ->
                if (hook(id)) {
                    param.result = null
                    return@after
                }
            }

            EngagementPanel.open(id)
            panelId.remove()
        }
    }

    EngagementPanelUpdateFingerprint.hookMethod {
        before {
            EngagementPanel.close()
        }
    }
}

fun addEngagementPanelIdHook(hook: EngagementPanelIdHook) {
    engagementPanelIdHooks.add(hook)
}
