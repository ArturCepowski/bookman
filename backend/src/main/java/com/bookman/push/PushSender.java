package com.bookman.push;

/**
 * Q-24 (replaces SmsSender, REQ-13): every reminder goes through this interface so the channel can be replaced
 * and faked in tests.
 */
public interface PushSender {

    void send(String deviceToken, String title, String body) throws PushSendException;
}
