package io.github.leonstange.bhdtimer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

final class StateStore {
    private final Path file;

    StateStore(Path file) {
        this.file = file;
    }

    TimerState.Snapshot load() throws IOException, InvalidConfigurationException {
        if (!Files.exists(file)) {
            return new TimerState.Snapshot(0, true);
        }
        YamlConfiguration data = new YamlConfiguration();
        data.load(file.toFile());
        Object elapsed = data.get("elapsed-nanos");
        if (!(elapsed instanceof Long || elapsed instanceof Integer)
                || data.getLong("elapsed-nanos") < 0 || !data.isBoolean("visible")) {
            throw new InvalidConfigurationException("Ungültiger Zeitstand oder Sichtbarkeitsstatus in " + file);
        }
        return new TimerState.Snapshot(data.getLong("elapsed-nanos"), data.getBoolean("visible"));
    }

    void save(TimerState.Snapshot snapshot) throws IOException {
        Files.createDirectories(file.getParent());
        YamlConfiguration data = new YamlConfiguration();
        data.set("elapsed-nanos", snapshot.elapsedNanos());
        data.set("visible", snapshot.visible());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, data.saveToString(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
