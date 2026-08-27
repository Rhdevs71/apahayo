package com.rhdevs.rhpatch.youtube.extension.youtube.patches;

import static com.rhdevs.rhpatch.youtube.extension.shared.StringRef.sf;

import com.rhdevs.rhpatch.youtube.extension.shared.Logger;
import com.rhdevs.rhpatch.youtube.extension.shared.Utils;

@SuppressWarnings("unused")
public class AccountCredentialsInvalidTextPatch {

    /**
     * Injection point.
     */
    public static String getOfflineNetworkErrorString(String original) {
        try {
            if (Utils.isNetworkConnected()) {
                Logger.printDebug(() -> "Network appears to be online, but app is showing offline error");
                return '\n' + sf("microg_offline_account_login_error").toString();
            }

            Logger.printDebug(() -> "Network is offline");
        } catch (Exception ex) {
            Logger.printException(() -> "getOfflineNetworkErrorString failure", ex);
        }

        return original;
    }
}
