package edu.nccu.plsm.watchservice;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.nio.file.WatchEvent;
import java.util.concurrent.TimeUnit;

/**
 *  The number of seconds the service should wait after hearing
 *  about an event from the kernel before passing it along to the
 *  client via its callback. Specifying a larger value may result
 *  in more effective temporal coalescing, resulting in fewer
 *  callbacks and greater overall efficiency.
 */
public final class LatencyWatchEventModifier implements WatchEvent.Modifier, Comparable<LatencyWatchEventModifier> {
    public static final LatencyWatchEventModifier DEFAULT = new LatencyWatchEventModifier(0.5D);

    private final double latency;

    public LatencyWatchEventModifier(double latencyInSeconds) {
        if (latencyInSeconds <= 0) {
            throw new IllegalArgumentException("latency <= 0");
        }
        this.latency = latencyInSeconds;
    }

    public LatencyWatchEventModifier(long time, TimeUnit unit) {
        this(TimeUnit.NANOSECONDS.convert(time, unit) / 1000000000D);
    }

    public LatencyWatchEventModifier(SensitivityWatchEventModifier sensitivity) {
        this(sensitivity.sensitivityValueInSeconds());
    }

    public double getLatencyInSeconds() {
        return latency;
    }

    @Override
    public String name() {
        return "LatencyWatchEventModifier(latency=" + latency + ")";
    }

    @Override
    public int compareTo(LatencyWatchEventModifier o) {
        return 0;
    }

    @Override
    public String toString() {
        return name();
    }

}
