package dev.epicduels.model;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Party {

    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 8;

    private final UUID id;
    private UUID owner;
    private final List<UUID> members = new ArrayList<>();

    public Party(UUID owner) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.members.add(owner);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public boolean isOwner(UUID playerId) {
        return owner.equals(playerId);
    }

    public List<UUID> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public boolean addMember(UUID playerId) {
        if (members.contains(playerId)) return false;
        if (members.size() >= MAX_SIZE) return false;
        return members.add(playerId);
    }

    public boolean removeMember(UUID playerId) {
        return members.remove(playerId);
    }

    public boolean contains(UUID playerId) {
        return members.contains(playerId);
    }

    public int size() {
        return members.size();
    }

    public boolean isFull() {
        return members.size() >= MAX_SIZE;
    }

    public void messageAll(Component message) {
        for (UUID id : members) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }
}
