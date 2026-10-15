package io.github.leonstange.bhdtimer;

import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimerStateTest {
    private final AtomicLong clock = new AtomicLong();
    private final TimerState timer = new TimerState(new TimerState.Snapshot(0, true), clock::get);

    @Test
    void stopAndResumeRetainFractionsWithoutCountingPausedTime() {
        timer.start();
        advance(1_750_000_000L);
        timer.stop();
        advance(60_000_000_000L);
        assertEquals(1_750_000_000L, timer.elapsedNanos());
        timer.start();
        advance(250_000_000L);
        assertEquals("0h 0m 2s", timer.formattedTime());
    }

    @Test
    void repeatedStartDoesNotLoseTimeAndRepeatedStopDoesNotAddTime() {
        timer.start();
        advance(3_000_000_000L);
        timer.start();
        advance(2_000_000_000L);
        timer.stop();
        advance(100_000_000_000L);
        timer.stop();
        assertEquals(5_000_000_000L, timer.elapsedNanos());
    }

    @Test
    void hiddenTimerKeepsRunningAndVisibilityDoesNotResumePausedTimer() {
        timer.start();
        timer.setVisible(false);
        advance(8_000_000_000L);
        assertTrue(timer.running());
        assertEquals("0h 0m 8s", timer.formattedTime());
        timer.stop();
        timer.setVisible(true);
        advance(8_000_000_000L);
        assertFalse(timer.running());
        assertEquals("0h 0m 8s", timer.formattedTime());
    }

    @Test
    void resetStopsAtZeroAndKeepsVisibility() {
        timer.start();
        timer.setVisible(false);
        advance(8_000_000_000L);
        timer.reset();
        advance(8_000_000_000L);
        assertEquals(0, timer.elapsedNanos());
        assertFalse(timer.running());
        assertFalse(timer.visible());
        timer.start();
        advance(1_000_000_000L);
        assertEquals("0h 0m 1s", timer.formattedTime());
    }

    @Test
    void restoresPausedWithoutOfflineTimeAndRetainsHiddenState() {
        timer.start();
        timer.setVisible(false);
        advance(7_500_000_000L);
        TimerState.Snapshot snapshot = timer.snapshot();
        advance(86_400_000_000_000L);
        TimerState restored = new TimerState(snapshot, clock::get);
        assertFalse(restored.running());
        assertFalse(restored.visible());
        assertEquals(7_500_000_000L, restored.elapsedNanos());
        restored.start();
        advance(500_000_000L);
        assertEquals("0h 0m 8s", restored.formattedTime());
    }

    @Test
    void formatsHoursWithoutWrappingAtOneDay() {
        timer.start();
        advance(100_984_000_000_000L);
        assertEquals("28h 3m 4s", timer.formattedTime());
    }

    @Test
    void pausedDisplayIsEntirelyLightRedAndItalic() {
        TextComponent display = (TextComponent) BhdTimerPlugin.display(timer);
        assertEquals("0h 0m 0s - Pausiert", display.content());
        assertEquals(NamedTextColor.RED, display.color());
        assertEquals(TextDecoration.State.TRUE, display.decoration(TextDecoration.ITALIC));
    }

    @Test
    void runningDisplayRemovesPauseAndUsesBoldAqua() {
        timer.start();
        TextComponent display = (TextComponent) BhdTimerPlugin.display(timer);
        assertEquals("0h 0m 0s", display.content());
        assertEquals(NamedTextColor.AQUA, display.color());
        assertEquals(TextDecoration.State.TRUE, display.decoration(TextDecoration.BOLD));
        assertNotEquals(TextDecoration.State.TRUE, display.decoration(TextDecoration.ITALIC));
    }

    private void advance(long nanos) {
        clock.addAndGet(nanos);
    }
}
