package com.bookman.reservation;

/** Runs imports in the background (scheduled runs and the first import after registration). */
public interface ImportRunner {

    void submit(long apartmentId);
}
