package com.rhdevs.rhpatch.youtube.extension.music.patches.spoof;

import static com.rhdevs.rhpatch.youtube.extension.music.settings.Settings.SPOOF_VIDEO_STREAMS_CLIENT_TYPE;
import static com.rhdevs.rhpatch.youtube.extension.shared.spoof.ClientType.ANDROID_REEL_NO_AUTH;
import static com.rhdevs.rhpatch.youtube.extension.shared.spoof.ClientType.ANDROID_VR_1_64;
import static com.rhdevs.rhpatch.youtube.extension.shared.spoof.ClientType.TV;
import static com.rhdevs.rhpatch.youtube.extension.shared.spoof.ClientType.VISIONOS;

import java.util.List;

import com.rhdevs.rhpatch.youtube.extension.shared.spoof.ClientType;

@SuppressWarnings("unused")
public class SpoofVideoStreamsPatch {

    /**
     * Injection point.
     */
    public static void setClientOrderToUse() {
        List<ClientType> availableClients = List.of(
                TV,
                ANDROID_VR_1_64,
                VISIONOS,
                ANDROID_REEL_NO_AUTH
        );

        com.rhdevs.rhpatch.youtube.extension.shared.spoof.SpoofVideoStreamsPatch.setClientsToUse(
                availableClients, SPOOF_VIDEO_STREAMS_CLIENT_TYPE.get());
    }
}
