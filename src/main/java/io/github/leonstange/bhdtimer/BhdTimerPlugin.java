package io.github.leonstange.bhdtimer;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class BhdTimerPlugin extends JavaPlugin implements Listener {
    private static final List<String> SUBCOMMANDS = List.of("start", "stop", "reset", "hide", "show", "version");
    private final ResetConfirmation confirmations = new ResetConfirmation(System::nanoTime);
    private TimerState timer;
    private StateStore store;
    private ExecutorService saves;
    private int updates;

    @Override
    public void onEnable() {
        store = new StateStore(getDataFolder().toPath().resolve("state.yml"));
        try {
            timer = new TimerState(store.load(), System::nanoTime);
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Statusdatei konnte nicht geladen werden. Sie wird nicht überschrieben.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saves = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("bhd-timer-save").factory());
        PluginCommand command = Objects.requireNonNull(getCommand("bhd-timer"));
        command.setExecutor(this);
        command.setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, () -> {
            updateDisplay();
            if (++updates % 5 == 0) {
                saveState();
            }
        }, 1L, 20L);
        saveState();
    }

    @Override
    public void onDisable() {
        if (saves == null) {
            return;
        }
        getServer().getScheduler().cancelTasks(this);
        timer.stop();
        saveState();
        saves.shutdown();
        try {
            if (!saves.awaitTermination(10, TimeUnit.SECONDS)) {
                getLogger().severe("Speichern dauert zu lange; der letzte Zeitstand ist möglicherweise noch nicht gesichert.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            getLogger().warning("Warten auf das Speichern wurde unterbrochen.");
        }
        if (timer.visible()) {
            getServer().getOnlinePlayers().forEach(player -> player.sendActionBar(Component.empty()));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("BHD-Timer: keine Berechtigung."));
            return true;
        }
        if (args.length != 1) {
            message(sender, "Verwendung: /bhd-timer <start|stop|reset|hide|show|version>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "version" -> {
                message(sender, "Version v" + getPluginMeta().getVersion());
                return true;
            }
            case "start" -> {
                timer.start();
                confirmations.clear();
                message(sender, "Timer gestartet.");
            }
            case "stop" -> {
                timer.stop();
                confirmations.clear();
                message(sender, "Timer pausiert.");
            }
            case "reset" -> {
                if (!confirmations.confirmOrRequest(operatorKey(sender))) {
                    message(sender, "Zum Zurücksetzen /bhd-timer reset innerhalb von 5 Sekunden erneut eingeben.");
                    return true;
                }
                timer.reset();
                confirmations.clear();
                message(sender, "Timer zurückgesetzt und pausiert.");
            }
            case "hide" -> {
                if (timer.visible()) {
                    getServer().getOnlinePlayers().forEach(player -> player.sendActionBar(Component.empty()));
                }
                timer.setVisible(false);
                message(sender, "Timer für alle ausgeblendet.");
            }
            case "show" -> {
                timer.setVisible(true);
                message(sender, "Timer für alle eingeblendet.");
            }
            default -> {
                message(sender, "Verwendung: /bhd-timer <start|stop|reset|hide|show|version>");
                return true;
            }
        }
        saveState();
        updateDisplay();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp() || args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (timer.visible()) {
            event.getPlayer().sendActionBar(display(timer));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        confirmations.forget(operatorKey(event.getPlayer()));
    }

    static Component display(TimerState timer) {
        if (timer.running()) {
            return Component.text(timer.formattedTime(), NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD);
        }
        return Component.text(timer.formattedTime() + " - Pausiert", NamedTextColor.RED)
                .decorate(TextDecoration.ITALIC);
    }

    private void updateDisplay() {
        if (timer.visible()) {
            Component text = display(timer);
            getServer().getOnlinePlayers().forEach(player -> player.sendActionBar(text));
        }
    }

    private void saveState() {
        TimerState.Snapshot snapshot = timer.snapshot();
        saves.execute(() -> {
            try {
                store.save(snapshot);
            } catch (IOException exception) {
                getLogger().log(Level.SEVERE, "Timerstand konnte nicht gespeichert werden.", exception);
            }
        });
    }

    private static String operatorKey(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId().toString() : "console:" + sender.getName();
    }

    private static void message(CommandSender sender, String text) {
        sender.sendMessage(Component.text("BHD-Timer: " + text));
    }
}
