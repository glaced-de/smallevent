package net.glaced.desertjunglecore.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

public final class MessageService {

    private static final TextColor DESERT = TextColor.color(0xF2C572);
    private static final TextColor JUNGLE = TextColor.color(0x4CD07D);

    private static final Component PREFIX = Component.text()
            .append(gradient("[GLACED] ", DESERT, JUNGLE, true))
            .build();

    private MessageService() {}

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(
                Component.text()
                        .append(PREFIX)
                        .append(Component.text(message))
                        .build()
        );
    }

    public static Component prefix() {
        return PREFIX;
    }

    private static Component gradient(String text, TextColor from, TextColor to, boolean bold) {
        int length = text.length();
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < length; i++) {
            float ratio = length == 1 ? 0f : (float) i / (length - 1);
            TextColor color = TextColor.lerp(ratio, from, to);
            Style style = bold ? Style.style(color, TextDecoration.BOLD) : Style.style(color);
            builder.append(Component.text(String.valueOf(text.charAt(i)), style));
        }

        return builder.build();
    }
}
