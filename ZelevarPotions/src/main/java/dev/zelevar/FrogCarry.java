package dev.zelevar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Shift + ПКМ по лягушке - взять на голову. Shift + ПКМ - поставить обратно. */
public final class FrogCarry implements Listener {
    private final JavaPlugin plugin;
    private final String teamName;
    private final Map<UUID, Long> lastAction = new HashMap<>();

    public FrogCarry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.teamName = plugin.getConfig().getString("frog.team.name", "frogman");
        team(); // автосоздание команды
        startHintTask();
    }

    /** Подсказка над хотбаром, когда игрок смотрит на свободную лягушку. */
    private void startHintTask() {
        Component hint = LegacyComponentSerializer.legacyAmpersand().deserialize(
            plugin.getConfig().getString("frog.messages.hint", "&eВзять лягушку: Шифт + ПКМ"));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!allowed(p) || carried(p) != null) continue;
                Entity target = p.getTargetEntity(4);
                if (target instanceof Frog frog && frog.getVehicle() == null) {
                    p.sendActionBar(hint);
                }
            }
        }, 10L, 10L);
    }

    public Team team() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = sb.getTeam(teamName);
        if (t == null) {
            t = sb.registerNewTeam(teamName);
            plugin.getLogger().info("Создана команда '" + teamName + "'");
        }
        t.displayName(Component.text(plugin.getConfig().getString("frog.team.display-name", "лягушатник")));
        return t;
    }

    private boolean allowed(Player p) {
        if (p.hasPermission("frogcarry.bypass")) return true;
        Team t = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
        return t != null && t.getName().equals(teamName);
    }

    private Frog carried(Player p) {
        for (Entity e : p.getPassengers()) if (e instanceof Frog f) return f;
        return null;
    }

    private void msg(Player p, String key, String def) {
        p.sendMessage(LegacyComponentSerializer.legacyAmpersand()
            .deserialize(plugin.getConfig().getString("frog.messages." + key, def)));
    }

    /** Защита от двойного срабатывания одного клика. */
    private boolean tooFast(Player p) {
        long now = System.currentTimeMillis();
        Long last = lastAction.put(p.getUniqueId(), now);
        return last != null && now - last < 250;
    }

    private void place(Player p, Frog frog, Location where) {
        p.removePassenger(frog);
        frog.teleport(where);
        msg(p, "placed", "&aВы поставили лягушку на землю");
    }

    // Shift + ПКМ по лягушке: взять (или поставить, если клик попал в свою лягушку)
    @EventHandler(ignoreCancelled = true)
    public void onEntityClick(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Frog frog)) return;
        Player p = e.getPlayer();
        if (!p.isSneaking() || !allowed(p)) return;

        e.setCancelled(true);
        if (tooFast(p)) return;

        Frog mine = carried(p);
        if (mine != null) {
            if (mine.equals(frog)) place(p, mine, p.getLocation());
            else msg(p, "already", "&cВы уже несёте лягушку");
            return;
        }
        if (frog.getVehicle() != null) return; // её уже несёт кто-то другой
        if (p.addPassenger(frog)) msg(p, "picked", "&aВы взяли лягушку на голову");
    }

    // Shift + ПКМ по блоку или в воздух: поставить лягушку
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;
        Frog frog = carried(p);
        if (frog == null) return;

        e.setCancelled(true);
        if (tooFast(p)) return;

        Location loc = p.getLocation();
        Block b = e.getClickedBlock();
        if (b != null) {
            Block target = b.getRelative(e.getBlockFace());
            if (target.isPassable()) loc = target.getLocation().add(0.5, 0, 0.5);
        }
        loc.setYaw(p.getLocation().getYaw());
        place(p, frog, loc);
    }

    // При телепорте Minecraft сбрасывает пассажиров - возвращаем лягушку на голову
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        Frog frog = carried(p);
        if (frog == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || !frog.isValid() || p.getPassengers().contains(frog)) return;
            frog.teleport(p.getLocation());
            p.addPassenger(frog);
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Frog frog = carried(e.getPlayer());
        if (frog != null) e.getPlayer().removePassenger(frog);
        lastAction.remove(e.getPlayer().getUniqueId());
    }
}
