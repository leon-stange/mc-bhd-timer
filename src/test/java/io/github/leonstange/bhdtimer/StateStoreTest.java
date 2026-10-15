package io.github.leonstange.bhdtimer;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class StateStoreTest {
    @TempDir
    Path directory;

    @Test
    void missingFileStartsAtZeroAndVisible() throws Exception {
        assertEquals(new TimerState.Snapshot(0, true), store().load());
    }

    @Test
    void roundTripPreservesSubsecondsAndGlobalVisibility() throws Exception {
        TimerState.Snapshot snapshot = new TimerState.Snapshot(7_123_456_789L, false);
        store().save(snapshot);
        assertEquals(snapshot, store().load());
        assertFalse(Files.exists(directory.resolve("state.yml.tmp")));
    }

    @Test
    void confirmedResetOverwritesSavedTimeWithReadableZero() throws Exception {
        store().save(new TimerState.Snapshot(100_000_000_000L, false));
        store().save(new TimerState.Snapshot(0, false));
        assertEquals(new TimerState.Snapshot(0, false), store().load());
    }

    @Test
    void corruptFileIsRejectedAndKeptUntouched() throws Exception {
        String corrupt = "elapsed-nanos: not-a-number\nvisible: true\n";
        Files.writeString(directory.resolve("state.yml"), corrupt);
        assertThrows(InvalidConfigurationException.class, () -> store().load());
        assertEquals(corrupt, Files.readString(directory.resolve("state.yml")));
    }

    @Test
    void negativeTimeAndMissingVisibilityAreRejected() throws Exception {
        Files.writeString(directory.resolve("state.yml"), "elapsed-nanos: -1\nvisible: true\n");
        assertThrows(InvalidConfigurationException.class, () -> store().load());
        Files.writeString(directory.resolve("state.yml"), "elapsed-nanos: 0\n");
        assertThrows(InvalidConfigurationException.class, () -> store().load());
    }

    private StateStore store() {
        return new StateStore(directory.resolve("state.yml"));
    }
}
