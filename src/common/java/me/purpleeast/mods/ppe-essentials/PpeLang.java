package me.purpleeast.mods.ppe_essentials;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves player-facing messages using the client's language with a configured fallback.
 */
public final class PpeLang {
    private static final Component PREFIX = Component.empty()
            .append(coloredLiteral("[", 0xAAAAAA))
            .append(coloredLiteral("P", 0xF378F3))
            .append(coloredLiteral("P", 0xF655C8))
            .append(coloredLiteral("E", 0xF9329D))
            .append(coloredLiteral("] ", 0xAAAAAA));

    private static final Pattern ARGUMENT = Pattern.compile("%([1-9][0-9]*)\\$s");

    private static final Map<String, Map<String, Component>> LANGUAGES = Map.of(
            "zh_cn", load("zh_cn"),
            "zh_tw", load("zh_tw"),
            "zh_hk", load("zh_tw"),
            "en_us", load("en_us"),
            "ru_ru", load("ru_ru"),
            "de_de", load("de_de"),
            "fr_fr", load("fr_fr"),
            "ja_jp", load("ja_jp"),
            "ko_kr", load("ko_kr")
    );

    private PpeLang() {
    }

    public static MutableComponent component(ServerPlayer player, String key, Object... args) {
        Component template = LANGUAGES.get(languageFor(player)).get(key);
        if (template == null) {
            template = LANGUAGES.get("en_us").get(key);
        }
        if (template == null) {
            return Component.literal(key);
        }
        return args.length == 0 ? template.copy() : insertArguments(template, args);
    }

    public static MutableComponent prefixedComponent(ServerPlayer player, String key, Object... args) {
        return prefixed(component(player, key, args));
    }

    public static MutableComponent prefixed(Component component) {
        return PpeConfig.messagePrefixEnabled() ? PREFIX.copy().append(component) : component.copy();
    }

    private static String languageFor(ServerPlayer player) {
        if (player != null) {
            String clientLanguage = player.clientInformation().language();
            if (LANGUAGES.containsKey(clientLanguage)) {
                return clientLanguage;
            }
        }

        String configuredLanguage = PpeConfig.fallbackLanguage();
        if (LANGUAGES.containsKey(configuredLanguage)) {
            return configuredLanguage;
        }

        return "en_us";
    }

    private static MutableComponent insertArguments(Component template, Object[] args) {
        MutableComponent message = template.plainCopy();
        if (template.getContents() instanceof PlainTextContents literal) {
            String text = ARGUMENT.matcher(literal.text()).replaceAll(match -> {
                int index = Integer.parseInt(match.group(1)) - 1;
                if (index >= args.length) {
                    return Matcher.quoteReplacement(match.group());
                }
                Object argument = args[index];
                String value = argument instanceof Component component ? component.getString() : String.valueOf(argument);
                return Matcher.quoteReplacement(value);
            });
            message = Component.literal(text);
        }
        message.setStyle(template.getStyle());
        for (Component sibling : template.getSiblings()) {
            message.append(insertArguments(sibling, args));
        }
        return message;
    }

    private static MutableComponent coloredLiteral(String text, int color) {
        return Component.literal(text).withStyle(style -> style.withColor(color));
    }

    private static Map<String, Component> load(String language) {
        String path = "/assets/" + PpeEssentials.MODID + "/messages/" + language + ".json";
        try (InputStream stream = PpeLang.class.getResourceAsStream(path)) {
            if (stream == null) {
                PpeEssentials.LOGGER.warn("Missing PPE Essentials language file: {}", path);
                return Map.of();
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Component> values = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();
                ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .ifSuccess(value -> values.put(key, value))
                        .ifError(error -> PpeEssentials.LOGGER.warn("Invalid message {} in {}: {}", key, path, error.message()));
            }
            return Map.copyOf(values);
        } catch (Exception exception) {
            PpeEssentials.LOGGER.warn("Failed to load PPE Essentials language file: {}", path, exception);
            return Map.of();
        }
    }
}
