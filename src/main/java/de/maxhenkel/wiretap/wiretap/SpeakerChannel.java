package de.maxhenkel.wiretap.wiretap;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.WiretapVoicechatPlugin;
import de.maxhenkel.wiretap.soundeffects.SoundEffect;
import de.maxhenkel.wiretap.soundeffects.SoundEffectManager;
import de.maxhenkel.wiretap.utils.AudioUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class SpeakerChannel implements Supplier<short[]> {

    private WiretapManager wiretapManager;
    private final UUID id;
    private final Map<UUID, List<short[]>> packetBuffer;
    private final DimensionLocation dimensionLocation;
    private final float range; // if > 0, use this for range. Otherwise, use server config.

    private final Map<UUID, OpusDecoder> decoders;
    private final SoundEffect effect;
    @Nullable
    private AudioPlayer audioPlayer;


    public SpeakerChannel(WiretapManager wiretapManager, UUID id, DimensionLocation dimensionLocation, float range) {
        this.wiretapManager = wiretapManager;
        this.id = id;
        this.dimensionLocation = dimensionLocation;
        this.range = range;

        this.packetBuffer = new HashMap<>();
        this.decoders = new HashMap<>();
        this.effect = SoundEffectManager.getSoundEffect();
    }

    public void addPacket(UUID sender, Vec3 senderLocation, byte[] opusEncodedData) {
        DimensionLocation microphoneLocation = wiretapManager.getMicrophoneLocation(id);

        if (microphoneLocation == null) {
            Wiretap.LOGGER.warn("Microphone location not found for {}}", id);
            return;
        }

        List<short[]> microphonePackets = packetBuffer.computeIfAbsent(sender, k -> new ArrayList<>());

        if (microphonePackets.isEmpty()) {
            for (int i = 0; i < Wiretap.SERVER_CONFIG.packetBufferSize.get(); i++) {
                microphonePackets.add(null);
            }
        }

        OpusDecoder decoder = getDecoder(sender);
        if (opusEncodedData == null || opusEncodedData.length <= 0) {
            decoder.resetState();
            return;
        }

        double distance = microphoneLocation.getDistance(senderLocation);
        double volume = AudioUtils.getDistanceVolume(Wiretap.SERVER_CONFIG.microphonePickupRange.get(), distance);

        microphonePackets.add(AudioUtils.setVolume(decoder.decode(opusEncodedData), volume));

        if (audioPlayer == null) {
            getAudioPlayer().startPlaying();
        }
    }

    private OpusDecoder getDecoder(UUID sender) {
        return decoders.computeIfAbsent(sender, k -> WiretapVoicechatPlugin.voicechatServerApi.createDecoder());
    }

    private AudioPlayer getAudioPlayer() {
        if (this.audioPlayer != null) return this.audioPlayer;

        de.maxhenkel.voicechat.api.ServerLevel serverLevel = WiretapVoicechatPlugin.voicechatServerApi.fromServerLevel(dimensionLocation.getLevel());
        Position position = WiretapVoicechatPlugin.voicechatServerApi.createPosition(dimensionLocation.getX() + 0.5D, dimensionLocation.getY() + 0.5D, dimensionLocation.getZ() + 0.5D);

        float rangeIn = this.getOutputChannelRange();
        float audioRange = rangeIn < 0.0f
                ? 60000000.0f
                : rangeIn;

        LocationalAudioChannel channel = WiretapVoicechatPlugin.voicechatServerApi.createLocationalAudioChannel(id, serverLevel, position);
        channel.setCategory(WiretapVoicechatPlugin.WIRETAP_CATEGORY);
        channel.setDistance(audioRange);

        this.audioPlayer = WiretapVoicechatPlugin.voicechatServerApi.createAudioPlayer(channel, WiretapVoicechatPlugin.voicechatServerApi.createEncoder(), this);

        return this.audioPlayer;
    }

    @Nullable
    public short[] generatePacket() {
        List<short[]> packetsToCombine = new ArrayList<>();
        for (Map.Entry<UUID, List<short[]>> packets : packetBuffer.entrySet()) {
            if (packets.getValue().isEmpty()) {
                continue;
            }
            short[] audio = packets.getValue().remove(0);
            packetsToCombine.add(audio);
        }
        packetBuffer.values().removeIf(List::isEmpty);

        if (packetsToCombine.isEmpty()) {
            return null;
        }

        short[] combinedAudio = AudioUtils.combineAudio(packetsToCombine);

        spawnParticle();

        return effect.applyEffect(combinedAudio);
    }

    public void close() {
        decoders.values().forEach(OpusDecoder::close);
        if (audioPlayer != null) {
            audioPlayer.stopPlaying();
        }
    }

    @Override
    public short[] get() {
        short[] audio = generatePacket();
        if (audio == null) {
            if (audioPlayer != null) {
                audioPlayer.stopPlaying();
            }
            audioPlayer = null;
            return null;
        }
        return audio;
    }

    public DimensionLocation getDimensionLocation() {
        return dimensionLocation;
    }

    private long lastParticle = 0L;

    public void spawnParticle() {
        long time = System.currentTimeMillis();
        if (time - lastParticle < 1000L) {
            return;
        }
        lastParticle = time;

        ServerLevel level = dimensionLocation.getLevel();
        Vec3 vec3 = Vec3.atBottomCenterOf(dimensionLocation.getPos()).add(0D, 0.8D, 0D);
        level.players().stream().filter(player -> dimensionLocation.getDistance(player.position()) <= 32D).forEach(player -> {
            level.getServer().execute(() -> {
                float random = (float) level.getRandom().nextInt(4) / 24F;
                level.sendParticles(ParticleTypes.NOTE, vec3.x(), vec3.y(), vec3.z(), 0, random, 0D, 0D, 1D);
            });
        });
    }

    public void onPlayerDisconnect(ServerPlayer serverPlayer) {
        OpusDecoder remove = decoders.remove(serverPlayer.getUUID());
        if (remove != null) {
            remove.close();
        }
    }

    private float getOutputChannelRange() {
        return this.range;
    }
}
