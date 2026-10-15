package io.github.leonstange.bhdtimer;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResetConfirmationTest {
    private final AtomicLong clock = new AtomicLong();
    private final ResetConfirmation confirmations = new ResetConfirmation(clock::get);

    @Test
    void requiresTwoCallsBySameOperatorWithinFiveSeconds() {
        assertFalse(confirmations.confirmOrRequest("alice"));
        assertFalse(confirmations.confirmOrRequest("bob"));
        clock.set(4_999_999_999L);
        assertTrue(confirmations.confirmOrRequest("alice"));
        assertFalse(confirmations.confirmOrRequest("alice"));
    }

    @Test
    void allowsConfirmationAtFiveSeconds() {
        assertFalse(confirmations.confirmOrRequest("alice"));
        clock.set(5_000_000_000L);
        assertTrue(confirmations.confirmOrRequest("alice"));
    }

    @Test
    void expiredCallStartsNewWindowWithoutConfirming() {
        assertFalse(confirmations.confirmOrRequest("alice"));
        clock.set(5_000_000_001L);
        assertFalse(confirmations.confirmOrRequest("alice"));
        clock.addAndGet(1_000_000_000L);
        assertTrue(confirmations.confirmOrRequest("alice"));
    }

    @Test
    void clearedOrDisconnectedOperatorMustRequestAgain() {
        confirmations.confirmOrRequest("alice");
        confirmations.clear();
        assertFalse(confirmations.confirmOrRequest("alice"));
        confirmations.forget("alice");
        assertFalse(confirmations.confirmOrRequest("alice"));
    }
}
