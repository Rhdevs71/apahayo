package com.rhdevs.rhpatch.youtube.extension.youtube.patches.spans;

import android.text.SpannableString;

import com.rhdevs.rhpatch.youtube.extension.shared.patches.spans.SpanFilter;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.spans.SpanType;
import com.rhdevs.rhpatch.youtube.extension.shared.patches.spans.StringSpanFilterGroup;
import com.rhdevs.rhpatch.youtube.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public final class SanitizeVideoSubtitleFilter extends SpanFilter {

    public SanitizeVideoSubtitleFilter() {
        addCallbacks(
                new StringSpanFilterGroup(
                        Settings.SANITIZE_VIDEO_SUBTITLE,
                        "|video_subtitle."
                )
        );
    }

    @Override
    public boolean skip(String conversionContext, SpannableString spannableString, Object span,
                        int start, int end, int flags, boolean isWord, SpanType spanType, StringSpanFilterGroup matchedGroup) {
        if (isWord) {
            if (spanType == SpanType.IMAGE) {
                hideImageSpan(spannableString, start, end, flags);
                return true;
            } else if (spanType == SpanType.CUSTOM_CHARACTER_STYLE) {
                hideSpan(spannableString, start, end, flags);
                return true;
            }
        }
        return false;
    }
}
