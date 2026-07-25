package net.glaced.desertjunglecore.api;

import net.kyori.adventure.text.format.NamedTextColor;
public enum EventTeam {
    DESERT("Desert", NamedTextColor.GOLD, "🏜"), JUNGLE("Jungle", NamedTextColor.GREEN, "🌴");
    private final String display; private final NamedTextColor color; private final String icon;
    EventTeam(String display, NamedTextColor color, String icon) { this.display = display; this.color = color; this.icon = icon; }
    public String display() { return display; } public NamedTextColor color() { return color; } public String icon() { return icon; }
    public static EventTeam parse(String value) { return value == null ? null : switch (value.toLowerCase()) { case "desert" -> DESERT; case "jungle" -> JUNGLE; default -> null; }; }
}
