package com.bookman.app.fakes;

import com.bookman.app.data.session.SessionListener;
import java.util.ArrayList;
import java.util.List;

public class RecordingSessionListener implements SessionListener {

    public final List<String> events = new ArrayList<>();

    @Override
    public void onLoggedIn() {
        events.add("in");
    }

    @Override
    public void onLoggedOut(String previousSessionToken) {
        events.add("out:" + previousSessionToken);
    }
}
