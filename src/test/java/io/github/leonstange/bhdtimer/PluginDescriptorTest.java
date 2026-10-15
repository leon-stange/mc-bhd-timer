package io.github.leonstange.bhdtimer;

import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginDescriptorTest {
    @Test
    void paperCanReadPluginAndCommandMetadata() throws Exception {
        try (var input = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(input);
            PluginDescriptionFile description = new PluginDescriptionFile(input);
            assertEquals("0.0.1", description.getVersion());
            assertEquals("io.github.leonstange.bhdtimer.BhdTimerPlugin", description.getMain());
            assertEquals("26.3", description.getAPIVersion());
            assertEquals("BHD - Timer", description.getPrefix());
            assertTrue(description.getCommands().containsKey("bhd-timer"));
            // A metadata permission would bypass our exact custom denial message.
            assertNull(description.getCommands().get("bhd-timer").get("permission"));
        }
    }
}
