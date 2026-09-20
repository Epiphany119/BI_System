package com.yupi.springbootinit.charttask.domain;

/** Server-side routing policy. The threshold is configurable for later load testing. */
public final class ChartTaskRouter {
    private ChartTaskRouter() { }

    public static ChartTaskMode route(long fileSizeBytes, long syncThresholdBytes, long maxFileSizeBytes) {
        if (fileSizeBytes < 0) {
            throw new IllegalArgumentException("file size cannot be negative");
        }
        if (fileSizeBytes > maxFileSizeBytes) {
            throw new IllegalArgumentException("file size exceeds maxFileSizeBytes");
        }
        return fileSizeBytes <= syncThresholdBytes ? ChartTaskMode.SYNC : ChartTaskMode.ASYNC;
    }
}
