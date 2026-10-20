package com.bookman.app.common;

import androidx.annotation.Nullable;

/** One-shot LiveData payload (navigation, snackbar) that is not re-delivered after rotation. */
public final class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    @Nullable
    public T consume() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    public T peek() {
        return content;
    }
}
