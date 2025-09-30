package com.wargear.builderPlots;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BuilderPlots extends JavaPlugin implements Listener {

    private Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("bau")) {
            String safeName = "bau_" + player.getUniqueId().toString();
            World w = Bukkit.getWorld(safeName);

            if (w == null) {
                String mvCommand = "mv clone bau " + safeName;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), mvCommand);

                // asynchron
                new BukkitRunnable() {
                    int tries = 0;
                    @Override
                    public void run() {
                        World newWorld = Bukkit.getWorld(safeName);
                        if (newWorld != null) {
                            // teleport + permissions
                            player.teleport(newWorld.getSpawnLocation());
                            setBuildPermissions(player, safeName);
                            player.sendMessage("§aWillkommen in deinem Bau!");
                            cancel();
                            return;
                        }
                        tries++;
                        if (tries > 20) { // ~20 * 5 ticks = 100 ticks = 5s timeout
                            player.sendMessage("§cWelt konnte nicht geladen werden. Schau Console/Multiverse.");
                            cancel();
                        }
                    }
                }.runTaskTimer(this, 5L, 5L);
                return true;
            }

            player.teleport(w.getSpawnLocation());
            setBuildPermissions(player, safeName);
            player.sendMessage("§aWillkommen in deinem Bau!");
            return true;
        }

        if (label.equalsIgnoreCase("schemsave")) {
            if (args.length < 1) {
                player.sendMessage("/schemsave <Typ>");
                return true;
            }
            String type = args[0].toLowerCase();
            // TODO: FAWE Schematic save
            player.sendMessage("§aSchematic gespeichert mit Typ: " + type);
            return true;
        }

        return false;
    }

    private void setBuildPermissions(Player player, String worldName) {
        PermissionAttachment att = attachments.remove(player.getUniqueId());
        if (att != null) player.removeAttachment(att);

        PermissionAttachment newAtt = player.addAttachment(this);
        newAtt.setPermission("bau." + worldName + ".owner", true);
        newAtt.setPermission("worldedit.*", true);
        attachments.put(player.getUniqueId(), newAtt);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        World from = e.getFrom();
        World to = p.getWorld();

        // Permissions entfernen beim Verlassen
        if (from != null && from.getName().startsWith("bau_")) {
            PermissionAttachment att = attachments.remove(p.getUniqueId());
            if (att != null) p.removeAttachment(att);
        }

        // Zugriff nur auf eigene Welt
        if (to != null && to.getName().startsWith("bau_") && !to.getName().equals("bau_" + p.getUniqueId().toString())) {
            World fallback = Bukkit.getWorld("world");
            if (fallback != null) p.teleport(fallback.getSpawnLocation());
            p.sendMessage("§cDu hast keinen Zugriff auf diese Welt!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        PermissionAttachment att = attachments.remove(p.getUniqueId());
        if (att != null) p.removeAttachment(att);
    }
}
