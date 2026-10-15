package io.github.leonstange.bhdtimer;

import java.util.function.LongSupplier;

final class TimerState {
    private final LongSupplier clock;
    private long elapsedNanos;
    private long startedAt;
    private boolean running;
    private boolean visible;

    TimerState(Snapshot saved, LongSupplier clock) {
        this.elapsedNanos = saved.elapsedNanos();
        this.visible = saved.visible();
        this.clock = clock;
    }

    void start() {
        if (!running) {
            startedAt = clock.getAsLong();
            running = true;
        }
    }

    void stop() {
        elapsedNanos = elapsedNanos();
        running = false;
    }

    void reset() {
        running = false;
        elapsedNanos = 0;
    }

    long elapsedNanos() {
        return running ? elapsedNanos + (clock.getAsLong() - startedAt) : elapsedNanos;
    }

    String formattedTime() {
        long seconds = elapsedNanos() / 1_000_000_000L;
        return "%dh %dm %ds".formatted(seconds / 3600, seconds / 60 % 60, seconds % 60);
    }

    boolean running() {
        return running;
    }

    boolean visible() {
        return visible;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
    }

    Snapshot snapshot() {
        return new Snapshot(elapsedNanos(), visible);
    }

    record Snapshot(long elapsedNanos, boolean visible) {
        Snapshot {
            if (elapsedNanos < 0) {
                throw new IllegalArgumentException("Der gespeicherte Zeitstand darf nicht negativ sein.");
            }
        }
    }
}
