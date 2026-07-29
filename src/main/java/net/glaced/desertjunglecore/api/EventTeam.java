package net.glaced.desertjunglecore.api;

import net.kyori.adventure.text.format.NamedTextColor;

public enum EventTeam {
    TEAM1("Team 1", NamedTextColor.GOLD, "🐽", "team1"),
    TEAM2("Team 2", NamedTextColor.GREEN, "🐢", "team2");

    private final String display;
    private final NamedTextColor color;
    private final String icon;
    private final String luckPermsGroup;

    EventTeam(String display, NamedTextColor color, String icon, String luckPermsGroup) {
        this.display = display;
        this.color = color;
        this.icon = icon;
        this.luckPermsGroup = luckPermsGroup;
    }

    public String display() { return display; }
    public NamedTextColor color() { return color; }
    public String icon() { return icon; }
    public String luckPermsGroup() { return luckPermsGroup; }

    public static EventTeam parse(String value) {
        return value == null ? null : switch (value.toLowerCase()) {
            case "team1" -> TEAM1;
            case "team2" -> TEAM2;
            default -> null;
        };
    }
}