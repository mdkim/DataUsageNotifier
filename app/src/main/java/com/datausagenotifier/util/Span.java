package com.datausagenotifier.util;

import android.graphics.Typeface;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

public class Span {

    public static StyleSpan BOLD_SPAN() {
        return new StyleSpan(Typeface.BOLD);
    }

    public static RelativeSizeSpan TINY_SPAN() {
        return new RelativeSizeSpan(.85f);
    }

    public static class BOLD_SPAN_FACTORY implements SpanFactory {
        @Override
        public StyleSpan getInstance() {
            return BOLD_SPAN();
        }
    }
    public static class TINY_SPAN_FACTORY implements SpanFactory {
        @Override
        public RelativeSizeSpan getInstance() {
            return TINY_SPAN();
        }
    }
}
