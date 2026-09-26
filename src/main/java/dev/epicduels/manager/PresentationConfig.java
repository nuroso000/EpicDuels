package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;

/** A broken YAML file never replaces the last successfully loaded configuration. */
public final class PresentationConfig {
    private PresentationConfig() { }
    public static YamlConfiguration load(EpicDuels plugin, String name, YamlConfiguration previous) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);
        YamlConfiguration next = new YamlConfiguration();
        try { next.load(file); return next; }
        catch (Exception error) {
            plugin.getLogger().warning(name + ": could not load YAML: " + error.getMessage() + "; retaining previous/default settings.");
            return previous == null ? new YamlConfiguration() : previous;
        }
    }
}
