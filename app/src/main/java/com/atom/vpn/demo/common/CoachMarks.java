/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */
package com.atom.vpn.demo.common;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;

import com.atom.vpn.demo.R;
import com.tooltip.Tooltip;

/**
 * Hold-to-explain coach marks: holding a tagged view for {@link #HOLD_MS} shows a tooltip that
 * explains the SDK call behind it and how to use it.
 * <p>
 * Tag a view with {@link #set}; {@link com.atom.vpn.demo.common.base.BaseSampleActivity} feeds
 * every touch through {@link Detector}. The hold is detected at the window rather than with a
 * per-view OnTouchListener for two reasons: a disabled view never calls its touch listener, and
 * the actions that most need explaining (Pause while disconnected, say) are disabled exactly
 * when someone is trying to learn them; and it leaves every view's own touch handling alone.
 * <p>
 * Once the coach mark shows, the rest of the gesture is cancelled for the views underneath, so
 * the hold never also clicks, toggles or scrolls.
 */
public final class CoachMarks {

    /** How long a view must be held before its coach mark appears. */
    public static final long HOLD_MS = 2000L;
    private static final long AUTO_DISMISS_MS = 8000L;

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final Runnable autoDismiss = CoachMarks::dismiss;

    @Nullable
    private static Tooltip showing;

    private CoachMarks() {
    }

    /** Gives {@code view} a coach mark. Safe to call again on a recycled view. */
    public static void set(@Nullable View view, @StringRes int text) {
        if (view == null) {
            return;
        }
        view.setTag(R.id.coach_mark_text, text);
        if (view.getTag(R.id.coach_mark_a11y) == null) {
            // A 2 second hold is undiscoverable with a screen reader, so the same explanation is
            // exposed as an explicit accessibility action.
            view.setTag(R.id.coach_mark_a11y, Boolean.TRUE);
            ViewCompat.addAccessibilityAction(view,
                    view.getContext().getString(R.string.cd_explain_sdk_call),
                    (v, arguments) -> show(v));
        }
    }

    /** Same coach mark on several views of one screen. */
    public static void set(@NonNull View root, @StringRes int text, int... ids) {
        for (int id : ids) {
            set(root.findViewById(id), text);
        }
    }

    /** Explanations for the bottom nav shared by the Dashboard, Locations, More and Logs screens. */
    public static void setBottomNav(@NonNull View root) {
        set(root.findViewById(R.id.navDashboard), R.string.coach_nav_dashboard);
        set(root.findViewById(R.id.navConnection), R.string.coach_nav_connection);
        set(root.findViewById(R.id.navLocations), R.string.coach_nav_locations);
        set(root.findViewById(R.id.navMore), R.string.coach_nav_more);
    }

    /** Shows the coach mark tagged on {@code anchor}. Returns false if it has none. */
    public static boolean show(@NonNull View anchor) {
        Object text = anchor.getTag(R.id.coach_mark_text);
        if (!(text instanceof Integer)) {
            return false;
        }

        dismiss();

        // Above the anchor unless that would run off the top of the screen.
        Rect visible = new Rect();
        anchor.getGlobalVisibleRect(visible);
        int screenHeight = anchor.getResources().getDisplayMetrics().heightPixels;
        int gravity = visible.centerY() < screenHeight / 3 ? Gravity.BOTTOM : Gravity.TOP;

        Tooltip tooltip = new Tooltip.Builder(anchor, R.style.CoachMarkTooltipStyle)
                .setText(anchor.getContext().getString((Integer) text))
                .setGravity(gravity)
                .setCancelable(true)
                .setDismissOnClick(true)
                .build();
        tooltip.show();
        showing = tooltip;

        handler.postDelayed(autoDismiss, AUTO_DISMISS_MS);

        // A popup outliving its anchor's window leaks it, so it goes with the screen.
        anchor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                v.removeOnAttachStateChangeListener(this);
                if (showing == tooltip) {
                    dismiss();
                }
            }
        });
        return true;
    }

    public static void dismiss() {
        handler.removeCallbacks(autoDismiss);
        if (showing != null && showing.isShowing()) {
            showing.dismiss();
        }
        showing = null;
    }

    /**
     * The deepest view under the screen point that carries a coach mark, or null. Disabled views
     * count; only views actually visible on screen (not clipped away by a scroll) do.
     */
    @Nullable
    private static View findTagged(@NonNull View view, int x, int y, @NonNull Rect scratch) {
        if (view.getVisibility() != View.VISIBLE
                || !view.getGlobalVisibleRect(scratch)
                || !scratch.contains(x, y)) {
            return null;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            // Last child draws on top, so it wins where siblings overlap.
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View hit = findTagged(group.getChildAt(i), x, y, scratch);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return view.getTag(R.id.coach_mark_text) != null ? view : null;
    }

    /**
     * Watches one window's touch stream for a hold on a tagged view. The activity calls
     * {@link #onTouch} first and skips its normal dispatch when it returns true.
     */
    public static final class Detector {

        public interface Dispatcher {
            void dispatch(@NonNull MotionEvent event);
        }

        private final View root;
        private final Dispatcher dispatcher;
        private final int touchSlop;
        private final Rect scratch = new Rect();

        @Nullable
        private View target;
        private float downX, downY;
        /** The coach mark fired: swallow the rest of this gesture. */
        private boolean consumed;
        @Nullable
        private MotionEvent lastEvent;

        private final Runnable fire = this::fire;

        /**
         * @param root       the window's decor view.
         * @param dispatcher the activity's normal dispatch, used to send the views underneath an
         *                   ACTION_CANCEL once the coach mark takes the gesture.
         */
        public Detector(@NonNull View root, @NonNull Dispatcher dispatcher) {
            this.root = root;
            this.dispatcher = dispatcher;
            this.touchSlop = ViewConfiguration.get(root.getContext()).getScaledTouchSlop();
        }

        /** Returns true when the event belongs to a coach mark and must not reach the views. */
        public boolean onTouch(@NonNull MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    reset();
                    downX = event.getRawX();
                    downY = event.getRawY();
                    target = findTagged(root, (int) downX, (int) downY, scratch);
                    if (target != null) {
                        remember(event);
                        handler.postDelayed(fire, HOLD_MS);
                    }
                    return false;

                case MotionEvent.ACTION_MOVE:
                    if (consumed) {
                        return true;
                    }
                    remember(event);
                    if (Math.abs(event.getRawX() - downX) > touchSlop
                            || Math.abs(event.getRawY() - downY) > touchSlop) {
                        // A drag or scroll, not a hold.
                        cancelPending();
                    }
                    return false;

                case MotionEvent.ACTION_POINTER_DOWN:
                    if (consumed) {
                        return true;
                    }
                    cancelPending();
                    return false;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    boolean swallow = consumed;
                    reset();
                    return swallow;

                default:
                    return consumed;
            }
        }

        private void fire() {
            View anchor = target;
            target = null;
            if (anchor == null || !anchor.isAttachedToWindow() || !show(anchor)) {
                return;
            }
            consumed = true;
            // Cancel the press underneath so releasing does not click, toggle or scroll.
            if (lastEvent != null) {
                MotionEvent cancel = MotionEvent.obtain(lastEvent);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                dispatcher.dispatch(cancel);
                cancel.recycle();
            }
        }

        private void remember(@NonNull MotionEvent event) {
            if (lastEvent != null) {
                lastEvent.recycle();
            }
            lastEvent = MotionEvent.obtain(event);
        }

        private void cancelPending() {
            handler.removeCallbacks(fire);
            target = null;
        }

        private void reset() {
            cancelPending();
            consumed = false;
            if (lastEvent != null) {
                lastEvent.recycle();
                lastEvent = null;
            }
        }

        /** Call when the window goes away so a pending hold does not fire on a dead screen. */
        public void release() {
            reset();
        }
    }
}
