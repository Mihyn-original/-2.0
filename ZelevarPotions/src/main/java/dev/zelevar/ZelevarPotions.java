package dev.zelevar;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import io.papermc.paper.potion.PotionMix;

public final class ZelevarPotions extends JavaPlugin implements Listener {

    private NamespacedKey potionKey;
    private final List<NamespacedKey> mixKeys = new ArrayList<>();
    private final Set<Material> customIngredients = EnumSet.noneOf(Material.class);
    private String teamName;
    private FrogCarry frogCarry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        potionKey = new NamespacedKey(this, "zelevar_potion");
        teamName = getConfig().getString("team.name", "zelevar");

        ensureTeam();
        registerMixes();

        getServer().getPluginManager().registerEvents(this, this);
        var cmd = getCommand("zelevar");
        if (cmd != null) {
            var handler = new TeamCommand(this::team, "зельевар");
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        // Функция переноски лягушек (отдельная команда)
        frogCarry = new FrogCarry(this);
        getServer().getPluginManager().registerEvents(frogCarry, this);
        var frogCmd = getCommand("frogteam");
        if (frogCmd != null) {
            var handler = new TeamCommand(frogCarry::team, "лягушатник");
            frogCmd.setExecutor(handler);
            frogCmd.setTabCompleter(handler);
        }
    }

    @Override
    public void onDisable() {
        for (NamespacedKey k : mixKeys) Bukkit.getPotionBrewer().removePotionMix(k);
        mixKeys.clear();
    }

    /** Создаёт scoreboard-команду автоматически, если её ещё нет. */
    public Team team() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = sb.getTeam(teamName);
        if (t == null) {
            t = sb.registerNewTeam(teamName);
            getLogger().info("Создана команда '" + teamName + "'");
        }
        t.displayName(Component.text(getConfig().getString("team.display-name", "зельевар")));
        return t;
    }

    private void ensureTeam() { team(); }

    private void registerMixes() {
        for (PotionDef d : PotionDef.all()) {
            if (!getConfig().getBoolean("potions." + d.id() + ".enabled", true)) continue;
            int sec = getConfig().getInt("potions." + d.id() + ".duration-seconds", d.seconds());

            NamespacedKey key = new NamespacedKey(this, d.id());
            PotionMix mix = new PotionMix(
                key,
                buildPotion(d, sec * 20),
                PotionMix.createPredicateChoice(this::isAwkward),   // основа: Мутное зелье
                new RecipeChoice.MaterialChoice(d.ingredient())
            );
            Bukkit.getPotionBrewer().addPotionMix(mix);
            mixKeys.add(key);
            customIngredients.add(d.ingredient());
        }
        getLogger().info("Зарегистрировано рецептов: " + mixKeys.size());
    }

    private boolean isAwkward(ItemStack s) {
        return s != null && s.getType() == Material.POTION
            && s.getItemMeta() instanceof PotionMeta pm
            && pm.getBasePotionType() == PotionType.AWKWARD
            && !pm.hasCustomEffects();
    }

    private ItemStack buildPotion(PotionDef d, int ticks) {
        ItemStack item = new ItemStack(d.splash() ? Material.SPLASH_POTION : Material.POTION);
        PotionMeta m = (PotionMeta) item.getItemMeta();
        m.displayName(Component.text(d.name(), TextColor.color(d.rgb()))
            .decoration(TextDecoration.ITALIC, false));
        m.setColor(Color.fromRGB(d.rgb()));
        m.addCustomEffect(new PotionEffect(d.effect(), ticks, d.amplifier()), true);
        m.lore(List.of(Component.text("Варят только зельевары", TextColor.color(0xAA66FF))
            .decoration(TextDecoration.ITALIC, false)));
        m.getPersistentDataContainer().set(potionKey, PersistentDataType.STRING, d.id());
        item.setItemMeta(m);
        return item;
    }

    // ---------- Доступ по команде ----------

    private boolean allowed(Player p) {
        if (p.hasPermission("zelevar.bypass")) return true;
        Team t = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
        return t != null && t.getName().equals(teamName);
    }

    private boolean isCustom(ItemStack s) {
        return s != null && customIngredients.contains(s.getType());
    }

    private void deny(Player p) {
        String raw = getConfig().getString("messages.no-access", "&cНет доступа.");
        p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
    }

    private static final int INGREDIENT_SLOT = 3;

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory() instanceof BrewerInventory)) return;
        if (allowed(p)) return;

        boolean block;
        switch (e.getClick()) {
            case NUMBER_KEY -> block = e.getRawSlot() == INGREDIENT_SLOT
                && isCustom(p.getInventory().getItem(e.getHotbarButton()));
            case SWAP_OFFHAND -> block = e.getRawSlot() == INGREDIENT_SLOT
                && isCustom(p.getInventory().getItemInOffHand());
            case SHIFT_LEFT, SHIFT_RIGHT -> block = e.getRawSlot() >= e.getView().getTopInventory().getSize()
                && isCustom(e.getCurrentItem());
            default -> block = e.getRawSlot() == INGREDIENT_SLOT && isCustom(e.getCursor());
        }
        if (block) {
            e.setCancelled(true);
            deny(p);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory() instanceof BrewerInventory)) return;
        if (allowed(p)) return;
        if (e.getRawSlots().contains(INGREDIENT_SLOT) && isCustom(e.getOldCursor())) {
            e.setCancelled(true);
            deny(p);
        }
    }

    /** Воронки не должны обходить ограничение. */
    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) {
        if (e.getDestination() instanceof BrewerInventory && isCustom(e.getItem())) {
            e.setCancelled(true);
        }
    }
}
