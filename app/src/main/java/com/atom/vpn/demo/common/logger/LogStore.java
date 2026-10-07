/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.common.logger;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * App-wide, in-memory log of SDK events for the Logs screen. Entries outlive any one screen, so
 * the log already has history when it is opened. Every read and write happens on the main
 * thread; {@link #add} hops there itself because SDK callbacks can arrive on any thread.
 *
 * <p>Pausing is app-wide: while paused, entries from anywhere in the app are dropped rather than
 * queued, and every screen sees the same state.</p>
 */
public final class LogStore {

    public enum Level { INFO, SUCCESS, WARN, ERROR }

    public static final class Entry {
        public final long timeMillis;
        @NonNull public final Level level;
        @NonNull public final String message;

        Entry(long timeMillis, @NonNull Level level, @NonNull String message) {
            this.timeMillis = timeMillis;
            this.level = level;
            this.message = message;
        }
    }

    public interface Listener {
        void onLogsChanged();
    }

    /** Oldest entries are dropped past this, so a long session cannot grow the list forever. */
    private static final int MAX_ENTRIES = 500;

    /**
     * The SDK fires several overloads for one event (e.g. onConnected() and
     * onConnected(ConnectionDetails)); an identical entry inside this window is that echo.
     */
    private static final long DUPLICATE_WINDOW_MS = 1000;

    private static final LogStore INSTANCE = new LogStore();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Entry> entries = new ArrayList<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    /** Volatile because {@link #add} checks it on the caller's thread, before hopping to main. */
    private volatile boolean paused;

    private LogStore() {
    }

    @NonNull
    public static LogStore get() {
        return INSTANCE;
    }

    public void add(@NonNull Level level, @NonNull String message) {
        if (paused) {
            return;
        }
        long now = System.currentTimeMillis();
        if (Looper.myLooper() == Looper.getMainLooper()) {
            append(now, level, message);
        } else {
            mainHandler.post(() -> append(now, level, message));
        }
    }

    private void append(long timeMillis, @NonNull Level level, @NonNull String message) {
        if (!entries.isEmpty()) {
            Entry last = entries.get(entries.size() - 1);
            if (last.level == level && last.message.equals(message)
                    && timeMillis - last.timeMillis < DUPLICATE_WINDOW_MS) {
                return;
            }
        }

        entries.add(new Entry(timeMillis, level, message));
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
        notifyListeners();
    }

    public boolean isPaused() {
        return paused;
    }

    /** Must be called on the main thread; listeners are told so every screen can reflect it. */
    public void setPaused(boolean paused) {
        if (this.paused == paused) {
            return;
        }
        this.paused = paused;
        notifyListeners();
    }

    public void clear() {
        entries.clear();
        notifyListeners();
    }

    /** A copy, oldest first, so callers can filter it without touching the live list. */
    @NonNull
    public List<Entry> snapshot() {
        return new ArrayList<>(entries);
    }

    public int count(@NonNull Level level) {
        int count = 0;
        for (Entry entry : entries) {
            if (entry.level == level) {
                count++;
            }
        }
        return count;
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onLogsChanged();
        }
    }
}
