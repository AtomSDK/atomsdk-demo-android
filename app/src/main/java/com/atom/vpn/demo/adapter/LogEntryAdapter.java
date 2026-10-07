/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.atom.vpn.demo.R;
import com.atom.vpn.demo.common.logger.LogStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Backs the console on the Logs screen. Rows are {@code item_log_entry.xml}. */
public class LogEntryAdapter extends ArrayAdapter<LogStore.Entry> {

    private final LayoutInflater inflater;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public LogEntryAdapter(@NonNull Context context) {
        super(context, R.layout.item_log_entry, new ArrayList<>());
        this.inflater = LayoutInflater.from(context);
    }

    public void submit(@NonNull List<LogStore.Entry> entries) {
        setNotifyOnChange(false);
        clear();
        addAll(entries);
        notifyDataSetChanged();
    }

    /** One line per entry, in the same "time LEVEL message" shape the console shows. */
    @NonNull
    public String toPlainText(@NonNull List<LogStore.Entry> entries) {
        StringBuilder text = new StringBuilder();
        for (LogStore.Entry entry : entries) {
            text.append(formatTime(entry)).append(' ')
                    .append(label(entry.level)).append(' ')
                    .append(entry.message).append('\n');
        }
        return text.toString();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row = convertView != null
                ? convertView
                : inflater.inflate(R.layout.item_log_entry, parent, false);

        LogStore.Entry entry = getItem(position);
        if (entry == null) {
            return row;
        }

        TextView tvLevel = row.findViewById(R.id.tvLogLevel);
        TextView tvMessage = row.findViewById(R.id.tvLogMessage);

        ((TextView) row.findViewById(R.id.tvLogTime)).setText(formatTime(entry));
        tvLevel.setText(label(entry.level));
        tvMessage.setText(entry.message);

        row.findViewById(R.id.vLevelBar).setBackgroundColor(color(barColor(entry.level)));
        tvLevel.setTextColor(color(labelColor(entry.level)));
        tvMessage.setTextColor(color(messageColor(entry.level)));
        row.setBackgroundColor(entry.level == LogStore.Level.ERROR
                ? color(R.color.log_error_row)
                : android.graphics.Color.TRANSPARENT);

        return row;
    }

    @NonNull
    private String formatTime(@NonNull LogStore.Entry entry) {
        return timeFormat.format(new Date(entry.timeMillis));
    }

    @NonNull
    private static String label(@NonNull LogStore.Level level) {
        // WARN rather than WARNING keeps the column to its familiar logcat width.
        return level.name();
    }

    @ColorRes
    private static int barColor(@NonNull LogStore.Level level) {
        switch (level) {
            case SUCCESS: return R.color.log_success;
            case WARN: return R.color.log_warning;
            case ERROR: return R.color.log_error;
            default: return R.color.log_info;
        }
    }

    @ColorRes
    private static int labelColor(@NonNull LogStore.Level level) {
        switch (level) {
            case SUCCESS: return R.color.log_success;
            case WARN: return R.color.log_warning;
            case ERROR: return R.color.log_error_text;
            default: return R.color.log_text;
        }
    }

    /** Warnings and errors tint the whole line so they stand out; the rest stay neutral. */
    @ColorRes
    private static int messageColor(@NonNull LogStore.Level level) {
        switch (level) {
            case WARN: return R.color.log_warning_text;
            case ERROR: return R.color.log_error_text;
            default: return R.color.log_text;
        }
    }

    private int color(@ColorRes int res) {
        return ContextCompat.getColor(getContext(), res);
    }
}
