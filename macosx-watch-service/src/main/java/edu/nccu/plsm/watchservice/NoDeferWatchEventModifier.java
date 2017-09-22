package edu.nccu.plsm.watchservice;

import java.nio.file.WatchEvent;

/**
 * Affects the meaning of the {@link LatencyWatchEventModifier latency}.
 * If you specify this flag and more than latency seconds have elapsed
 * since the last event, your app will receive the event immediately.
 * The delivery of the event resets the latency timer and any further
 * events will be delivered after latency seconds have elapsed. This flag
 * is useful for apps that are interactive and want to react immediately
 * to changes but avoid getting swamped by notifications when changes
 * are occurring in rapid succession. If you do not specify this flag,
 * then when an event occurs after a period of no events, the latency
 * timer is started. Any events that occur during the next latency seconds
 * will be delivered as one group (including that first event). The
 * delivery of the group of events resets the latency timer and any
 * further events will be delivered after latency seconds. This is the
 * default behavior and is more appropriate for background, daemon or
 * batch processing apps.
 */
public enum NoDeferWatchEventModifier implements WatchEvent.Modifier {
    INSTANCE
}
