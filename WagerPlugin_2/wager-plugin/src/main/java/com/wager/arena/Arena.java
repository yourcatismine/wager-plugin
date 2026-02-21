package com.wager.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Arena {

    private final String id;
    private Location spawn1;
    private Location spawn2;
    private boolean inUse;
    private String schematicName;

    public Arena(String id) {
        this.id = id;
        this.inUse = false;
    }

    public String getId() { return id; }
    public Location getSpawn1() { return spawn1; }
    public Location getSpawn2() { return spawn2; }
    public boolean isInUse() { return inUse; }
    public String getSchematicName() { return schematicName; }

    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }
    public void setSchematicName(String schematicName) { this.schematicName = schematicName; }

    public boolean isReady() {
        return spawn1 != null && spawn2 != null;
    }

    /**
     * Serialize spawn location to config-friendly string
     */
    public static String serializeLocation(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + "," +
               loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," +
               loc.getYaw() + "," + loc.getPitch();
    }

    /**
     * Deserialize location from config string
     */
    public static Location deserializeLocation(String str) {
        if (str == null || str.isEmpty()) return null;
        String[] parts = str.split(",");
        if (parts.length < 6) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        return new Location(world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5]));
    }
}
