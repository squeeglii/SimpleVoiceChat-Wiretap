package de.maxhenkel.wiretap.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.ConfigEntry;
import de.maxhenkel.wiretap.Wiretap;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

import java.util.Optional;

public class ServerConfig {

    public final ConfigEntry<Double> microphonePickupRange;
    public final ConfigEntry<Double> speakerAudioRange;
    public final ConfigEntry<Integer> commandPermissionLevel;
    public final ConfigEntry<Integer> packetBufferSize;
    public final ConfigEntry<Boolean> anvilCrafting;
    public final ConfigEntry<String> speakerSoundEffect;
    public final ConfigEntry<String> microphoneSkinUrl;
    public final ConfigEntry<String> speakerSkinUrl;

    public ServerConfig(ConfigBuilder builder) {
        microphonePickupRange = builder.doubleEntry(
                "microphone_pickup_range",
                32D,
                1D,
                512D,
                "The range in which microphones can pick up sounds"
        );
        speakerAudioRange = builder.doubleEntry(
                "speaker_audio_range",
                32D,
                1D,
                512D,
                "The audible range of speakers"
        );
        commandPermissionLevel = builder.integerEntry(
                "command_permission_level",
                2,
                0,
                Integer.MAX_VALUE,
                "The permission level required to use the commands"
        );
        packetBufferSize = builder.integerEntry(
                "packet_buffer_size",
                6,
                1,
                4,
                "The amount of packets to buffer before playing"
        );
        anvilCrafting = builder.booleanEntry(
                "anvil_crafting",
                true,
                "Whether the items can be crafted in the anvil"
        );
        speakerSoundEffect = builder.stringEntry(
                "speaker_sound_effect",
                "old_speaker",
                "The sound effect to apply to the speaker",
                "Valid values are: 'none', 'old_speaker', 'low_quality'"
        );
        microphoneSkinUrl = builder.stringEntry(
                "microphone_skin_url",
                "http://textures.minecraft.net/texture/ccf0a27d246355e4dcbbd7b369d326cfed7aed1ba04e5dd9ba68cdecc4133d33",
                "The skin url for the microphone block"
        );
        speakerSkinUrl = builder.stringEntry(
                "speaker_skin_url",
                "http://textures.minecraft.net/texture/148a8c55891dec76764449f57ba677be3ee88a06921ca93b6cc7c9611a7af",
                "The skin url for the speaker block"
        );
    }

    // 0 (none) --> 4 (owner)
    public Optional<Permission> getMinimumPermission() {
        int level = this.commandPermissionLevel.get();

        return Optional.ofNullable(switch (level) {
            case 0 -> null;
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            case 4 -> Permissions.COMMANDS_OWNER;

            // todo: add a way to block using the command entirely

            default -> {
                Wiretap.LOGGER.warn("Minimum permission level incorrectly configured for wiretap command! Must be between 0-4");
                yield  Permissions.COMMANDS_GAMEMASTER;
            }
        });
    }

}
