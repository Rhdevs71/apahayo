/*
 * Copyright 2026 Morphe.
 * https://github.com/Morphecom/rhdevs/rhpatch/youtube-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package com.rhdevs.rhpatch.youtube.extension.shared.patches;

import static com.rhdevs.rhpatch.youtube.extension.shared.patches.ConversionContext.ELEMENT_IDENTIFIER_COMPONENT;
import static com.rhdevs.rhpatch.youtube.extension.shared.patches.ConversionContext.ELEMENT_IDENTIFIER_LAZILY;

import java.util.List;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.shared.Utils;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.components.ContextInterface;

@SuppressWarnings("unused")
public class TreeNodeElementPatch {

    public interface LithoGetBufferContainerInterface {
        // Method is added during patching.
        Object patch_getContainer();
    }

    /**
     * Injection point.
     */
    public static void onTreeNodeResultLoaded(ContextInterface contextInterface, List<Object> treeNodeResultList) {
        try {
            if (treeNodeResultList == null || treeNodeResultList.isEmpty()) {
                return;
            }
            String firstElement = treeNodeResultList.get(0).toString();
            if (ELEMENT_IDENTIFIER_COMPONENT.equals(firstElement)) {
                String path = contextInterface.patch_getPathBuilder().toString();
                onComponentLoaded(path, treeNodeResultList);
            } else if (ELEMENT_IDENTIFIER_LAZILY.equals(firstElement)) {
                String identifier = contextInterface.patch_getIdentifier();
                if (Utils.isNotEmpty(identifier)) {
                    onLazilyConvertedElementLoaded(identifier, treeNodeResultList);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onTreeNodeResultLoaded failure", ex);
        }
    }

    private static void onComponentLoaded(String path, List<Object> treeNodeResultList) {
        // Code added during patching.
    }

    private static void onLazilyConvertedElementLoaded(String identifier, List<Object> treeNodeResultList) {
        // Code added during patching.
    }
}
