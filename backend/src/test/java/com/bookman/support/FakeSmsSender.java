package com.bookman.support;

import com.bookman.sms.SmsSendException;
import com.bookman.sms.SmsSender;
import java.util.ArrayList;
import java.util.List;

/** Records SMS instead of calling a provider (CLAUDE.md: no external services in tests). */
public class FakeSmsSender implements SmsSender {

    public record Sms(String phoneNumber, String text) {
    }

    private final List<Sms> sent = new ArrayList<>();
    private int failuresLeft;
    private int attempts;

    public synchronized void failNext(int times) {
        failuresLeft = times;
    }

    public synchronized List<Sms> sent() {
        return new ArrayList<>(sent);
    }

    public synchronized int attempts() {
        return attempts;
    }

    public synchronized void reset() {
        sent.clear();
        failuresLeft = 0;
        attempts = 0;
    }

    @Override
    public synchronized void send(String phoneNumber, String text) throws SmsSendException {
        attempts++;
        if (failuresLeft > 0) {
            failuresLeft--;
            throw new SmsSendException("fake provider failure");
        }
        sent.add(new Sms(phoneNumber, text));
    }
}
