package com.rhdevs.rhpatch.youtube.extension.shared.spoof.js.nsigsolver.provider;

public class ContentProviderError extends Exception {
    public ContentProviderError(String message) {
        super(message);
    }

    public ContentProviderError(String message, Exception cause) {
        super(message, cause);
    }
}
