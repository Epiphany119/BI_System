package com.yupi.bi.analysis.domain.service;

import com.yupi.bi.analysis.domain.enums.ChartTaskMode;

public final class ChartTaskRouter {
    private ChartTaskRouter() { }

    public static ChartTaskMode route(long fileSizeBytes, long syncThresholdBytes, long maxFileSizeBytes) {
        if (fileSizeBytes < 0 || fileSizeBytes > maxFileSizeBytes) {
            throw new IllegalArgumentException("invalid file size");
        }
        return fileSizeBytes <= syncThresholdBytes ? ChartTaskMode.SYNC : ChartTaskMode.ASYNC;
    }
}
