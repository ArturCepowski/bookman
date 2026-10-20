package com.bookman.sms;

/** REQ-13: every SMS goes through this interface so the provider can be replaced and faked in tests. */
public interface SmsSender {

    /** @param phoneNumber E.164, e.g. +48600100200 */
    void send(String phoneNumber, String text) throws SmsSendException;
}
