package com.bookman.app.ui.apartment;

import com.bookman.app.data.apartment.ApartmentSummary;
import java.util.Collections;
import java.util.List;

public final class ApartmentListState {

    public final boolean loading;
    public final List<ApartmentSummary> items;
    /** At least one load has succeeded. */
    public final boolean loaded;

    ApartmentListState(boolean loading, List<ApartmentSummary> items, boolean loaded) {
        this.loading = loading;
        this.items = Collections.unmodifiableList(items);
        this.loaded = loaded;
    }

    /** REQ-03 empty state: the user has no role in any apartment. */
    public boolean isEmpty() {
        return loaded && !loading && items.isEmpty();
    }
}
