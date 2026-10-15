package io.github.leonstange.bhdtimer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

final class ResetConfirmation {
    private static final long WINDOW_NANOS = 5_000_000_000L;
    private final Map<String, Long> requests = new HashMap<>();
    private final LongSupplier clock;

    ResetConfirmation(LongSupplier clock) {
        this.clock = clock;
    }

    boolean confirmOrRequest(String operator) {
        long now = clock.getAsLong();
        requests.values().removeIf(requested -> now - requested > WINDOW_NANOS);
        Long requested = requests.remove(operator);
        if (requested != null) {
            return true;
        }
        requests.put(operator, now);
        return false;
    }

    void clear() {
        requests.clear();
    }

    void forget(String operator) {
        requests.remove(operator);
    }
}
