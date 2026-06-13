package me.purpleeast.mods.ppe_essentials;

import java.util.List;
import java.util.Set;

public final class PpeConfig {
    static final String FILE_NAME = "ppe_essentials-common.toml";
    private static final String DEFAULT_COMMENT_PREFIX = " Default: ";

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "en_us", "zh_cn", "zh_tw", "zh_hk", "ru_ru", "de_de", "fr_fr", "ja_jp", "ko_kr"
    );

    private static final ValueDefinition FALLBACK_LANGUAGE = stringValue(
            "fallbackLanguage",
            "en_us",
            SUPPORTED_LANGUAGES,
            " Fallback language used by the server when the player's client language is unsupported.",
            " This prevents clients without the mod installed from seeing untranslated localization keys.",
            " Supported values: en_us, zh_cn, zh_tw, zh_hk, ru_ru, de_de, fr_fr, ja_jp, ko_kr"
    );
    private static final ValueDefinition MESSAGE_PREFIX_ENABLED = booleanValue(
            "messagePrefixEnabled",
            false,
            " Whether mod messages should include the [PPE] prefix."
    );
    private static final ValueDefinition FIRST_JOIN_NOTICE = booleanValue(
            "firstJoinNotice",
            true,
            " Whether PPE Essentials should show a one-time command notice when a player first joins the server."
    );
    private static final ValueDefinition PREVENT_CREEPER_BLOCK_DAMAGE = booleanValue(
            "preventCreeperBlockDamage",
            true,
            " Whether creepers are prevented from breaking blocks with explosions."
    );
    private static final ValueDefinition PREVENT_ENDERMAN_BLOCK_DAMAGE = booleanValue(
            "preventEndermanBlockDamage",
            true,
            " Whether endermen are prevented from picking up or placing blocks."
    );
    private static final ValueDefinition PREVENT_RAVAGER_BLOCK_DAMAGE = booleanValue(
            "preventRavagerBlockDamage",
            true,
            " Whether ravagers are prevented from breaking leaves and crops."
    );
    private static final ValueDefinition RTP_COOLDOWN_SECONDS = intValue(
            "rtpCooldownSeconds",
            30,
            0,
            86400,
            " Cooldown in seconds for /rtp. Use 0 to disable the cooldown."
    );
    private static final ValueDefinition RTP_MIN_DISTANCE = intValue(
            "rtpMinDistance",
            2000,
            0,
            30000000,
            " Minimum distance from the current position for /rtp in normal dimensions."
    );
    private static final ValueDefinition RTP_MAX_DISTANCE = intValue(
            "rtpMaxDistance",
            5000,
            1,
            30000000,
            " Maximum distance from the current position for /rtp in normal dimensions."
    );
    private static final ValueDefinition RTP_NETHER_MIN_DISTANCE = intValue(
            "rtpNetherMinDistance",
            600,
            0,
            30000000,
            " Minimum distance from the current position for /rtp in the Nether."
    );
    private static final ValueDefinition RTP_NETHER_MAX_DISTANCE = intValue(
            "rtpNetherMaxDistance",
            1500,
            1,
            30000000,
            " Maximum distance from the current position for /rtp in the Nether."
    );
    private static final ValueDefinition TELEPORT_REQUEST_TIMEOUT_SECONDS = intValue(
            "teleportRequestTimeoutSeconds",
            60,
            1,
            86400,
            " How long TPA and TPAhere requests stay valid, in seconds."
    );
    private static final ValueDefinition ALLOW_SELF_TELEPORT_REQUESTS = booleanValue(
            "allowSelfTeleportRequests",
            true,
            " Whether players may send /tpa and /tpahere requests to themselves."
    );

    private static final List<ValueDefinition> VALUES = List.of(
            FALLBACK_LANGUAGE,
            MESSAGE_PREFIX_ENABLED,
            FIRST_JOIN_NOTICE,
            PREVENT_CREEPER_BLOCK_DAMAGE,
            PREVENT_ENDERMAN_BLOCK_DAMAGE,
            PREVENT_RAVAGER_BLOCK_DAMAGE,
            RTP_COOLDOWN_SECONDS,
            RTP_MIN_DISTANCE,
            RTP_MAX_DISTANCE,
            RTP_NETHER_MIN_DISTANCE,
            RTP_NETHER_MAX_DISTANCE,
            TELEPORT_REQUEST_TIMEOUT_SECONDS,
            ALLOW_SELF_TELEPORT_REQUESTS
    );

    private static final List<String> COMMAND_COMMENTS = List.of(
            " Command settings.",
            " enabled: whether the command is registered when the server starts. Changes require a server restart.",
            " permissionLevel: required OP permission level. Use 0 to allow everyone. Changes apply without restart."
    );

    private static final List<CommandDefinition> COMMANDS = List.of(
            command("tpa", true, 0),
            command("tpaa", true, 0),
            command("tpad", true, 0),
            command("tpaauto", true, 0),
            command("tpahere", true, 0),
            command("tpaherea", true, 0),
            command("tpahered", true, 0),
            command("rtp", true, 0),
            command("spawn", true, 0),
            command("back", true, 0),
            command("dback", true, 0),
            command("tback", true, 0),
            command("sethome", true, 0),
            command("delhome", true, 0),
            command("home", true, 0),
            command("suicide", true, 0),
            command("trash", true, 0),
            command("ppe-ess", true, 0),
            command("ppe-ess-reset", true, 4),
            command("warp", true, 0),
            command("setwarp", true, 4),
            command("delwarp", true, 4),
            command("repeat", true, 4),
            command("heal", true, 4),
            command("fly", true, 4),
            command("god", true, 4)
    );

    private PpeConfig() {
    }

    public static void load() {
        PpeConfigBackend.load();
    }

    public static String fallbackLanguage() {
        return PpeConfigBackend.stringValue(FALLBACK_LANGUAGE);
    }

    public static boolean messagePrefixEnabled() {
        return PpeConfigBackend.booleanValue(MESSAGE_PREFIX_ENABLED);
    }

    public static boolean firstJoinNotice() {
        return PpeConfigBackend.booleanValue(FIRST_JOIN_NOTICE);
    }

    public static boolean preventCreeperBlockDamage() {
        return PpeConfigBackend.booleanValue(PREVENT_CREEPER_BLOCK_DAMAGE);
    }

    public static boolean preventEndermanBlockDamage() {
        return PpeConfigBackend.booleanValue(PREVENT_ENDERMAN_BLOCK_DAMAGE);
    }

    public static boolean preventRavagerBlockDamage() {
        return PpeConfigBackend.booleanValue(PREVENT_RAVAGER_BLOCK_DAMAGE);
    }

    public static int rtpCooldownSeconds() {
        return PpeConfigBackend.intValue(RTP_COOLDOWN_SECONDS);
    }

    public static int rtpMinDistance() {
        return PpeConfigBackend.intValue(RTP_MIN_DISTANCE);
    }

    public static int rtpMaxDistance() {
        return Math.max(PpeConfigBackend.intValue(RTP_MAX_DISTANCE), rtpMinDistance() + 1);
    }

    public static int rtpNetherMinDistance() {
        return PpeConfigBackend.intValue(RTP_NETHER_MIN_DISTANCE);
    }

    public static int rtpNetherMaxDistance() {
        return Math.max(PpeConfigBackend.intValue(RTP_NETHER_MAX_DISTANCE), rtpNetherMinDistance() + 1);
    }

    public static int teleportRequestTimeoutSeconds() {
        return PpeConfigBackend.intValue(TELEPORT_REQUEST_TIMEOUT_SECONDS);
    }

    public static boolean allowSelfTeleportRequests() {
        return PpeConfigBackend.booleanValue(ALLOW_SELF_TELEPORT_REQUESTS);
    }

    public static boolean commandEnabled(String command) {
        return PpeConfigBackend.commandEnabled(command);
    }

    public static int commandPermission(String command) {
        return PpeConfigBackend.commandPermission(command);
    }

    static List<ValueDefinition> values() {
        return VALUES;
    }

    static List<String> commandComments() {
        return COMMAND_COMMENTS;
    }

    static List<CommandDefinition> commands() {
        return COMMANDS;
    }

    private static ValueDefinition stringValue(
            String key,
            String defaultValue,
            Set<String> allowedValues,
            String... comments
    ) {
        return new ValueDefinition(
                key,
                ValueType.STRING,
                defaultValue,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                Set.copyOf(allowedValues),
                List.of(comments)
        );
    }

    private static ValueDefinition booleanValue(String key, boolean defaultValue, String... comments) {
        return new ValueDefinition(
                key,
                ValueType.BOOLEAN,
                defaultValue,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                Set.of(),
                List.of(comments)
        );
    }

    private static ValueDefinition intValue(String key, int defaultValue, int min, int max, String... comments) {
        return new ValueDefinition(
                key,
                ValueType.INTEGER,
                defaultValue,
                min,
                max,
                Set.of(),
                List.of(comments)
        );
    }

    private static CommandDefinition command(String name, boolean enabled, int permissionLevel) {
        return new CommandDefinition(name, enabled, permissionLevel);
    }

    private static String defaultComment(Object value) {
        return DEFAULT_COMMENT_PREFIX + formatValue(value);
    }

    private static String formatValue(Object value) {
        return value instanceof String stringValue ? "\"" + stringValue + "\"" : value.toString();
    }

    enum ValueType {
        STRING,
        BOOLEAN,
        INTEGER
    }

    record ValueDefinition(
            String key,
            ValueType type,
            Object defaultValue,
            int min,
            int max,
            Set<String> allowedValues,
            List<String> comments
    ) {
        List<String> commentsWithDefaultValue() {
            return java.util.stream.Stream.concat(
                    comments.stream(),
                    java.util.stream.Stream.of(defaultComment(defaultValue))
            ).toList();
        }

        boolean accepts(Object value) {
            return switch (type) {
                case STRING -> value instanceof String stringValue
                        && (allowedValues.isEmpty() || allowedValues.contains(stringValue));
                case BOOLEAN -> value instanceof Boolean;
                case INTEGER -> value instanceof Integer intValue && intValue >= min && intValue <= max;
            };
        }
    }

    record CommandDefinition(String name, boolean enabled, int permissionLevel) {
        List<String> enabledComments() {
            return List.of(defaultComment(enabled));
        }

        List<String> permissionLevelComments() {
            return List.of(defaultComment(permissionLevel));
        }
    }
}
