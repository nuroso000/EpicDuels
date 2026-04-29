package dev.epicduels.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PartyTabCompleter implements TabCompleter {

    private static final List<String> SUBS = List.of(
            "create", "invite", "accept", "deny", "leave", "disband", "list", "start");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite") || sub.equals("accept") || sub.equals("deny") || sub.equals("decline")) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player s && s.equals(p)) continue;
                    names.add(p.getName());
                }
                return filter(names, args[1]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : options) if (s.toLowerCase().startsWith(lower)) out.add(s);
        return out;
    }
}
