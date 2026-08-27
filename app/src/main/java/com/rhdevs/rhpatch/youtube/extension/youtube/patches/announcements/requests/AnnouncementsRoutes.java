package com.rhdevs.rhpatch.youtube.extension.youtube.patches.announcements.requests;

import static com.rhdevs.rhpatch.youtube.extension.shared.requests.Route.Method.GET;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.rhdevs.rhpatch.youtube.extension.shared.requests.Requester;
import com.rhdevs.rhpatch.youtube.extension.shared.requests.Route;

public class AnnouncementsRoutes {
    // FIXME
    private static final String ANNOUNCEMENTS_PROVIDER = "https://api.morphi.app/v1";
    public static final Route GET_LATEST_ANNOUNCEMENT_IDS = new Route(GET, "/announcements/latest/id?tag=\uD83C\uDF9E\uFE0F%20YouTube");
    public static final Route GET_LATEST_ANNOUNCEMENTS = new Route(GET, "/announcements/latest?tag=\uD83C\uDF9E\uFE0F%20YouTube");

    private AnnouncementsRoutes() {
    }

    public static HttpURLConnection getAnnouncementsConnectionFromRoute(Route route, String... params) throws IOException {
        return Requester.getConnectionFromRoute(ANNOUNCEMENTS_PROVIDER, route, params);
    }
}