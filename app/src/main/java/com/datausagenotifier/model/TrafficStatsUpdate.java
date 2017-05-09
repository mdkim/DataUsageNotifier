package com.datausagenotifier.model;

import android.text.SpannableStringBuilder;

import com.datausagenotifier.util.Span;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrafficStatsUpdate {

    private List<TrafficStatsUid> statsUidList = new ArrayList<>();
    private long rxtxCount;
    private long dur;

    public void addStatsUid(TrafficStatsUid statsUid) {
        statsUidList.add(statsUid);
    }

    public void setRxTxCount(int rxtxCount) {
        this.rxtxCount = rxtxCount;
    }

    public CharSequence getContentTitle() {
        return rxtxCount + "+ apps sent/received data";
    }

    public SpannableStringBuilder formatSpannable() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append("[" + dur + " ms]\n", Span.TINY_SPAN(), 0);

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
}
