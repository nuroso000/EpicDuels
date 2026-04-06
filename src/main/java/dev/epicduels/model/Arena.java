package dev.epicduels.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class Arena {

    private final String name;
    private @Nullable Location spawn1;
    private @Nullable Location spawn2;
    private boolean ready;
    private @Nullable Material icon;

    public Arena(String name) {
        this.name = name;
        this.ready = false;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return "arena_template_" + name;
    }

    public @Nullable Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(@Nullable Location spawn1) {
        this.spawn1 = spawn1;
    }

    public @Nullable Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(@Nullable Location spawn2) {
        this.spawn2 = spawn2;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public @Nullable Material getIcon() {
        return icon;
    }

    public void setIcon(@Nullable Material icon) {
        this.icon = icon;
    }

    public Material getDisplayIcon() {
        return icon != null ? icon : Material.GRASS_BLOCK;
    }
}
