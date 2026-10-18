package dev.zelevar;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

/** Описание одного нового зелья. Основа везде - Мутное зелье (Awkward). */
public record PotionDef(String id, String name, int rgb, PotionEffectType effect,
                        int amplifier, int seconds, Material ingredient, boolean splash) {

    public static List<PotionDef> all() {
        return List.of(
            new PotionDef("haste", "Зелье спешки", 0xFFD84A, PotionEffectType.HASTE, 1, 180, Material.AMETHYST_SHARD, false),
            new PotionDef("levitation", "Зелье левитации", 0xCEFFFF, PotionEffectType.LEVITATION, 0, 20, Material.SHULKER_SHELL, false),
            new PotionDef("absorption", "Зелье поглощения", 0xF2C94C, PotionEffectType.ABSORPTION, 2, 120, Material.GOLDEN_APPLE, false),
            new PotionDef("health_boost", "Зелье здоровяка", 0xF82423, PotionEffectType.HEALTH_BOOST, 1, 180, Material.ENCHANTED_GOLDEN_APPLE, false),
            new PotionDef("saturation", "Зелье сытости", 0xE0A050, PotionEffectType.SATURATION, 0, 30, Material.BAKED_POTATO, false),
            new PotionDef("resistance", "Зелье стойкости", 0x8A8A9A, PotionEffectType.RESISTANCE, 1, 90, Material.ARMADILLO_SCUTE, false),
            new PotionDef("dolphin", "Зелье дельфина", 0x5CB8E6, PotionEffectType.DOLPHINS_GRACE, 0, 180, Material.PRISMARINE_CRYSTALS, false),
            new PotionDef("conduit", "Сила источника", 0x1FA7C9, PotionEffectType.CONDUIT_POWER, 0, 180, Material.HEART_OF_THE_SEA, false),
            new PotionDef("hero", "Зелье героя", 0x44C26B, PotionEffectType.HERO_OF_THE_VILLAGE, 0, 180, Material.EMERALD, false),
            new PotionDef("glowing", "Зелье свечения", 0xF6E27A, PotionEffectType.GLOWING, 0, 180, Material.GLOW_BERRIES, false),
            new PotionDef("blindness", "Зелье слепоты", 0x1B1B2F, PotionEffectType.BLINDNESS, 0, 20, Material.GLOW_INK_SAC, true),
            new PotionDef("wither", "Зелье иссушения", 0x3B3B3B, PotionEffectType.WITHER, 0, 15, Material.WITHER_ROSE, true),
            new PotionDef("hunger", "Зелье голода", 0x7A8B3A, PotionEffectType.HUNGER, 0, 30, Material.ROTTEN_FLESH, true),
            new PotionDef("nausea", "Зелье тошноты", 0x6F8F2F, PotionEffectType.NAUSEA, 0, 15, Material.RED_MUSHROOM, true),
            new PotionDef("fatigue", "Зелье усталости", 0x6B7B8C, PotionEffectType.MINING_FATIGUE, 1, 30, Material.SPONGE, true),
            new PotionDef("darkness", "Зелье мрака", 0x0B0B14, PotionEffectType.DARKNESS, 0, 30, Material.INK_SAC, true)
        );
    }
}
