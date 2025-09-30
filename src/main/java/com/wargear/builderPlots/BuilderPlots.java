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
import org.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.core.MultiverseCore;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class BuilderPlots extends JavaPlugin implements Listener {

    private MultiverseCore mv;
    private Map<Player, PermissionAttachment> attachments = new HashMap<>();

    @Override
    public @NotNull Path getDataPath() {
        return super.getDataPath();
    }

    @Override
    public void onEnable() {
        mv = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        Bukkit.getPluginManager().registerEvents(this, this);

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("bau")) {
            String weltName = "bau_" + player.getName();
            World w = Bukkit.getWorld(weltName);

            // Welt existiert nicht
            if (w == null) {
                mv.getMVWorldManager().cloneWorld("bau", weltName);
                w = Bukkit.getWorld(weltName);
            }

            // Teleport
            player.teleport(w.getSpawnLocation());

            // Permissions setzen
            PermissionAttachment att = player.addAttachment(this);
            att.setPermission("bau." + weltName + ".owner", true);
            att.setPermission("worldedit.*", true); // FAWE/WE Rechte
            attachments.put(player, att);

            player.sendMessage("§aWillkommen in deinem Bau!");
            return true;
        }

        if (label.equalsIgnoreCase("schemsave")) {
            if (args.length < 1) {
                player.sendMessage("/schemsave <Typ>");
                return true;
            }
            String type = args[0].toLowerCase();
            // Hier Schematic speichern mit FAWE API
            player.sendMessage("§aSchematic gespeichert mit Typ: " + type);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        World from = e.getFrom();
        World to = p.getWorld();

        // Permissions entfernen beim Verlassen
        if (from.getName().startsWith("bau_")) {
            PermissionAttachment att = attachments.remove(p);
            if (att != null) p.removeAttachment(att);
        }

        // Zugriff nur auf eigene Welt
        if (to.getName().startsWith("bau_") && !to.getName().equals("bau_" + p.getName())) {
            p.teleport(Bukkit.getWorld("world").getSpawnLocation());
            p.sendMessage("§cNur der Owner darf diese Welt betreten!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        PermissionAttachment att = attachments.remove(p);
        if (att != null) p.removeAttachment(att);
    }
}
