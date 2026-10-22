package com.bookman.support;

import com.bookman.push.PushSendException;
import com.bookman.push.PushSender;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Records notifications instead of calling Firebase (CLAUDE.md: no external services in tests). */
public class FakePushSender implements PushSender {

    public record Push(String deviceToken, String title, String body) {
    }

    private final List<Push> sent = new ArrayList<>();
    private final Set<String> invalidTokens = new HashSet<>();
    private int failuresLeft;
    private int attempts;

    public synchronized void failNext(int times) {
        failuresLeft = times;
    }

    public synchronized void markInvalid(String token) {
        invalidTokens.add(token);
    }

    public synchronized List<Push> sent() {
        return new ArrayList<>(sent);
    }

    public synchronized int attempts() {
        return attempts;
    }

    public synchronized void reset() {
        sent.clear();
        invalidTokens.clear();
        failuresLeft = 0;
        attempts = 0;
    }

    @Override
    public synchronized void send(String deviceToken, String title, String body) throws PushSendException {
        attempts++;
        if (invalidTokens.contains(deviceToken)) {
            throw new PushSendException("UNREGISTERED", true);
        }
        if (failuresLeft > 0) {
            failuresLeft--;
            throw new PushSendException("fake FCM failure", false);
        }
        sent.add(new Push(deviceToken, title, body));
    }
}
