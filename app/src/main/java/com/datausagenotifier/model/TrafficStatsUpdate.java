package com.datausagenotifier.model;

import android.content.Context;
import android.text.SpannableStringBuilder;

import com.datausagenotifier.util.Span;
import com.datausagenotifier.util.TextUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrafficStatsUpdate {

    private List<TrafficStatsUid> statsUidList = new ArrayList<>();
    private long rxtxCount;
    private long unknownRxBytes;
    private long unknownTxBytes;
    private long dur;
    private boolean isFirstPass;

    public void addStatsUid(TrafficStatsUid statsUid) {
        statsUidList.add(statsUid);
    }

    public void setRxTxCount(int rxtxCount) {
        this.rxtxCount = rxtxCount;
    }

    public CharSequence getContentTitle() {
        return rxtxCount + "+ apps sent/received data";
    }

    public SpannableStringBuilder formatSpannable(Context ctx) {

        StringBuilder metalog = new StringBuilder();
        metalog.append("[").append(dur).append(" ms]");
        if (this.unknownRxBytes > 0 || this.unknownTxBytes > 0) {
            metalog.append(" (Unknown apps");
            if (this.unknownRxBytes > 0) {
                metalog.append(" received ")
                        .append(TextUtil.formatBytesPerSec(ctx, this.unknownRxBytes));
            }
            if (this.unknownTxBytes > 0) {
                metalog.append(" sent " )
                        .append(TextUtil.formatBytesPerSec(ctx, this.unknownTxBytes));
            }
            metalog.append(")");
        }
        metalog.append("\n");

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(metalog, Span.TINY_SPAN(), 0);
        if (statsUidList.size() == 0) {
            ssb.append("(Unknown apps sent/received data)\n");
            return ssb;
        }

        this.sortByTotalBytes();

        for (TrafficStatsUid statsUid : this.statsUidList) {
            statsUid.appendStatsUidTo(ssb);
        }
        return ssb;
    }

    private void sortByTotalBytes() {
        Collections.sort(statsUidList, new Comparator<TrafficStatsUid>() {
            public int compare(TrafficStatsUid o1, TrafficStatsUid o2) {
                return Long.valueOf(o2.rxBytes + o2.txBytes).compareTo(o1.rxBytes + o1.txBytes);
            }
        });
    }

    public void setDurationMs(long dur) {
        this.dur = dur;
    }

    public void setUnknownRxBytes(long unknownRxBytes) {
        this.unknownRxBytes = unknownRxBytes;
    }

    public void setUnknownTxBytes(long unknownTxBytes) {
        this.unknownTxBytes = unknownTxBytes;
    }

    public void setIsFirstPass(boolean isFirstPass) {
        this.isFirstPass = isFirstPass;
    }

    public boolean isFirstPass() {
        return this.isFirstPass;
    }
}
