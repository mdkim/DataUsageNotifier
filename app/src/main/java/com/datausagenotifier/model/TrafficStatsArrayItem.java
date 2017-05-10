package com.datausagenotifier.model;

import android.text.SpannableString;

public class TrafficStatsArrayItem extends SpannableString {

    private boolean isFirstPass;

    public TrafficStatsArrayItem(CharSequence source, boolean isFirstPass) {
        super(source);
        this.isFirstPass = isFirstPass;
    }

    public boolean isFirstPass() {
        return this.isFirstPass;
    }
}
