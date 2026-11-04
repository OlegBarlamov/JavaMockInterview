package com.example.slotbooking.client.stub;

/** Simulates the network latency of the real (remote) services. */
final class StubLatency {

    private StubLatency() {
    }

    static void simulate(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
