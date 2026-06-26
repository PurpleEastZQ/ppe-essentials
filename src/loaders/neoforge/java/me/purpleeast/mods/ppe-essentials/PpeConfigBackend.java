package me.purpleeast.mods.ppe_essentials;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

final class PpeConfigBackend {
    private static final Map<String, ModConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();
    private static final Map<String, CommandValue> COMMANDS = new LinkedHashMap<>();

    static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (PpeConfig.ValueDefinition definition : PpeConfig.values()) {
            String[] comments = (definition.type() == PpeConfig.ValueType.INTEGER
                    ? definition.comments()
                    : definition.commentsWithDefaultValue()).toArray(String[]::new);
            builder.comment(comments);
            VALUES.put(definition.key(), define(builder, definition));
        }

        builder.comment(PpeConfig.commandComments().toArray(String[]::new)).push("commands");
        for (PpeConfig.CommandDefinition command : PpeConfig.commands()) {
            builder.push(command.name());
            COMMANDS.put(command.name(), new CommandValue(
                    builder.comment(command.enabledComments().toArray(String[]::new))
                            .define("enabled", command.enabled()),
                    builder.defineInRange("permissionLevel", command.permissionLevel(), 0, 4)
            ));
            builder.pop();
        }
        builder.pop();
        SPEC = builder.build();
    }

    private PpeConfigBackend() {
    }

    static void load() {
    }

    static String stringValue(PpeConfig.ValueDefinition definition) {
        return (String) VALUES.get(definition.key()).get();
    }

    static boolean booleanValue(PpeConfig.ValueDefinition definition) {
        return (boolean) VALUES.get(definition.key()).get();
    }

    static int intValue(PpeConfig.ValueDefinition definition) {
        return (int) VALUES.get(definition.key()).get();
    }

    static boolean commandEnabled(String command) {
        CommandValue value = COMMANDS.get(command);
        return value == null || value.enabled().get();
    }

    static int commandPermission(String command) {
        CommandValue value = COMMANDS.get(command);
        return value == null ? 0 : value.permissionLevel().get();
    }

    private static ModConfigSpec.ConfigValue<?> define(
            ModConfigSpec.Builder builder,
            PpeConfig.ValueDefinition definition
    ) {
        return switch (definition.type()) {
            case STRING -> builder.define(
                    definition.key(),
                    definition.defaultValue(),
                    definition::accepts
            );
            case BOOLEAN -> builder.define(
                    definition.key(),
                    (boolean) definition.defaultValue()
            );
            case INTEGER -> builder.defineInRange(
                    definition.key(),
                    (int) definition.defaultValue(),
                    definition.min(),
                    definition.max()
            );
        };
    }

    private record CommandValue(
            ModConfigSpec.BooleanValue enabled,
            ModConfigSpec.IntValue permissionLevel
    ) {
    }
}
