package dev.epicduels.command;

import dev.epicduels.EpicDuels;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DuelTabCompleter implements TabCompleter {

    private final EpicDuels plugin;

    public DuelTabCompleter(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("menu", "challenge", "c", "accept", "deny", "cancel", "stats", "queue", "q", "spectate", "spec"));
            if (sender.hasPermission("epicduels.admin")) {
                subs.addAll(Arrays.asList("arena", "kit", "setlobby"));
            }
            return filter(subs, args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "arena" -> {
                    if (sender.hasPermission("epicduels.admin")) {
                        return filter(Arrays.asList("create", "delete", "setspawn1", "setspawn2", "save", "list", "tp", "seticon"), args[1]);
                    }
                }
                case "kit" -> {
                    if (sender.hasPermission("epicduels.admin")) {
                        return filter(Arrays.asList("create", "delete", "list", "edit", "preview", "seticon"), args[1]);
                    }
                }
                case "challenge", "c", "accept", "deny", "spectate", "spec" -> {
                    return filter(getOnlinePlayerNames(sender), args[1]);
                }
                case "stats" -> {
                    return filter(getOnlinePlayerNames(sender), args[1]);
                }
                case "queue", "q" -> {
                    List<String> opts = new ArrayList<>(plugin.getKitManager().getKitNames());
                    opts.add("leave");
                    return filter(opts, args[1]);
                }
            }
        }

        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "arena" -> {
                    String action = args[1].toLowerCase();
                    if (action.equals("delete") || action.equals("tp") || action.equals("seticon")) {
                        List<String> names = new ArrayList<>();
                        plugin.getArenaManager().getAllArenas().forEach(a -> names.add(a.getName()));
                        return filter(names, args[2]);
                    }
                }
                case "kit" -> {
                    String action = args[1].toLowerCase();
                    if (action.equals("delete") || action.equals("edit") || action.equals("preview") || action.equals("seticon")) {
                        return filter(plugin.getKitManager().getKitNames(), args[2]);
                    }
                }
            }
        }

        return completions;
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames(CommandSender sender) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !(sender instanceof Player pl) || !p.equals(pl))
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}
