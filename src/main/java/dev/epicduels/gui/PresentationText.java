package dev.epicduels.gui;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Operator templates are trusted; runtime values are always literal text. */
public final class PresentationText {
    private PresentationText() { }
    public static Component render(String text, Map<String, String> values) {
        TagResolver.Builder tags = TagResolver.builder();
        values.forEach((key, value) -> tags.resolver(Placeholder.unparsed(key, value)));
        if (!text.contains("<")) {
            Component result = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
            for (var entry : values.entrySet()) {
                result = result.replaceText(net.kyori.adventure.text.TextReplacementConfig.builder()
                        .matchLiteral("{" + entry.getKey() + "}").replacement(Component.text(entry.getValue())).build());
            }
            return result;
        }
        return MiniMessage.miniMessage().deserialize(text, tags.build());
    }
}
