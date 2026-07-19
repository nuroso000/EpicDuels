package dev.epicduels.i18n;

import dev.epicduels.EpicDuels;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Central access to all player-facing messages. Templates live in
 * lang/messages_&lt;code&gt;.yml (MiniMessage format) and are chosen via the
 * config key {@code language}. Lookup order: selected language file in the
 * data folder → bundled English resource → the key itself (with a warning).
 *
 * Player names and other free text MUST be inserted via
 * {@link #unparsed(String, Object)} resolvers, never concatenated into the
 * template (names could contain MiniMessage tags).
 */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static EpicDuels plugin;
    private static YamlConfiguration lang;
    private static YamlConfiguration bundledEnglish;

    private Messages() {
    }

    public static void init(EpicDuels pluginInstance) {
        plugin = pluginInstance;
        // Extract bundled language files on first run (never overwrite edits)
        for (String code : new String[]{"en", "de"}) {
            if (!new File(plugin.getDataFolder(), "lang/messages_" + code + ".yml").exists()) {
                plugin.saveResource("lang/messages_" + code + ".yml", false);
            }
        }
        bundledEnglish = loadBundled("en");
        reload();
    }

    /** (Re-)reads the language selected by the config key {@code language}. */
    public static void reload() {
        String code = plugin.getConfig().getString("language", "en").toLowerCase();
        File file = new File(plugin.getDataFolder(), "lang/messages_" + code + ".yml");
        if (!file.exists()) {
            if (plugin.getResource("lang/messages_" + code + ".yml") != null) {
                plugin.saveResource("lang/messages_" + code + ".yml", false);
            } else {
                plugin.getLogger().warning("Unknown language '" + code + "' — falling back to English.");
                file = new File(plugin.getDataFolder(), "lang/messages_en.yml");
            }
        }
        lang = YamlConfiguration.loadConfiguration(file);
    }

    private static YamlConfiguration loadBundled(String code) {
        InputStream in = plugin.getResource("lang/messages_" + code + ".yml");
        if (in == null) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** Raw MiniMessage template for a key (for manual composition). */
    public static String raw(String key) {
        String value = lang.getString(key);
        if (value == null) value = bundledEnglish.getString(key);
        if (value == null) {
            plugin.getLogger().warning("Missing message key: " + key);
            return key;
        }
        return value;
    }

    /** Deserialized message component. */
    public static Component get(String key, TagResolver... resolvers) {
        return MM.deserialize(raw(key), resolvers);
    }

    /** Sends the message to the audience. */
    public static void send(Audience audience, String key, TagResolver... resolvers) {
        audience.sendMessage(get(key, resolvers));
    }

    /**
     * Like {@link #get}, but additionally replaces {@code {token}} markers in
     * the raw template BEFORE MiniMessage parsing. Needed for values inside
     * tag arguments (e.g. <click:run_command:'/duel accept {player}'>), where
     * resolver placeholders are not evaluated. Values are tag-escaped.
     */
    public static Component format(String key, java.util.Map<String, String> tokens, TagResolver... resolvers) {
        String template = raw(key);
        for (java.util.Map.Entry<String, String> e : tokens.entrySet()) {
            template = template.replace("{" + e.getKey() + "}", MM.escapeTags(e.getValue()));
        }
        return MM.deserialize(template, resolvers);
    }

    /** Placeholder for free text (player names, kit names, …) — never parsed. */
    public static TagResolver unparsed(String name, Object value) {
        return Placeholder.unparsed(name, String.valueOf(value));
    }

    /** Placeholder for an already-built component. */
    public static TagResolver component(String name, Component value) {
        return Placeholder.component(name, value);
    }
}
