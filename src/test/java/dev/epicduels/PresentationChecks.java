package dev.epicduels;

import dev.epicduels.gui.PresentationText;
import dev.epicduels.manager.MenuLayout;
import java.util.Map;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Standalone regression checks, run with the project's resolved dependency classpath. */
public final class PresentationChecks {
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(MenuLayout.allowed(false, false, 54, 10, 48), "expanded main menu allows movement");
        check(!MenuLayout.allowed(false, true, 54, 10, 11), "paginated content stays fixed");
        check(!MenuLayout.allowed(false, true, 54, 49, 10), "navigation cannot overwrite content");
        check(MenuLayout.allowed(false, true, 54, 49, 48), "navigation can move in control row");
        check(!MenuLayout.allowed(true, false, 54, 53, 40), "editor controls cannot overwrite kit");
        check(MenuLayout.allowed(true, false, 54, 53, 41), "editor save can move inside controls");
        check(!MenuLayout.allowed(false, false, 27, 10, -1), "negative destination rejected");
        check(!MenuLayout.allowed(false, false, 27, 10, 27), "upper bound rejected");
        check(!MenuLayout.valid(false, false, 27, Map.of(10, 16, 13, 16)), "duplicate destinations rejected");
        check(MenuLayout.valid(false, false, 27, Map.of(10, 13, 13, 10)), "button swaps accepted");
        check(!MenuLayout.valid(true, false, 54, Map.of(53, 54)), "editor upper bound rejected");
        var plain = PlainTextComponentSerializer.plainText();
        check(plain.serialize(PresentationText.render("<gold><player></gold>", Map.of("player", "<red>Injected")))
                .equals("<red>Injected"), "runtime names must remain literal");
        check(plain.serialize(PresentationText.render("", Map.of())).isEmpty(), "blank lines preserved");
        check(plain.serialize(PresentationText.render("&aLegacy", Map.of())).equals("Legacy"), "legacy format supported");
        check(plain.serialize(PresentationText.render("&a{player}", Map.of("player", "&cLiteral")))
                .equals("&cLiteral"), "legacy runtime names must remain literal");
        System.out.println("Presentation regression checks passed (15 assertions).");
    }
}
