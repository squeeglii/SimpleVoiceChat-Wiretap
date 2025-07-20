package de.maxhenkel.wiretap.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.item.MicrophoneDevice;
import de.maxhenkel.wiretap.item.SpeakerDevice;
import de.maxhenkel.wiretap.item.WiretapDevice;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class HeadUtils {

    public static final String NBT_DEVICE = "wiretap_data";

    public static final String NBT_IS_SPEAKER = "is_speaker";
    public static final String NBT_PAIR_ID = "pair_id";
    public static final String NBT_SPEAKER_RANGE = "speaker_range";

    public static Optional<ItemStack> createMicrophone(UUID id) {
        WiretapDevice deviceData = new MicrophoneDevice(id);
        return createHead(deviceData);
    }

    public static Optional<ItemStack> createSpeaker(UUID id) {
        return HeadUtils.createSpeakerWithForcedRange(id, null);
    }

    public static Optional<ItemStack> createSpeakerWithForcedRange(UUID id, Float range) {
        WiretapDevice deviceData = new SpeakerDevice(id, range);
        return createHead(deviceData);
    }

    public static Optional<ItemStack> createHead(WiretapDevice device) {
        if(device == null) throw new IllegalArgumentException("Device data cannot be null while creating head item");

        // item name -> DeviceType.displayName
        // name -> DeviceType.internalName
        // skinURL -> DeviceType.skinURL
        // deviceId -> WiretapDevice.uuid

        DeviceType type = device.getDeviceType();

        if(type == DeviceType.NON_WIRETAP) {
            Wiretap.LOGGER.warn("Failed to create head - type of device passed in was NON_WIRETAP. Data must be scuffed.", new Throwable("Trace"));
            return Optional.empty();
        }

        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        GameProfile gameProfile = HeadUtils.getGameProfile(device.getPairUUID(), type.getInternalName(), type.getSkinURL());

        MutableComponent nameComponent = Component.literal(type.getDisplayName()).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE));
        MutableComponent loreIdComponent = Component.literal("ID: %s".formatted(device.getPairUUID().toString()))
                                                    .withStyle(style -> style.withItalic(false))
                                                    .withStyle(ChatFormatting.GRAY);

        ItemLore lore;

        if(device instanceof SpeakerDevice speakerDevice && speakerDevice.isUsingRangeOverride()) {
            float range = speakerDevice.getActiveRange();
            MutableComponent speakerRadius = range < 0
                    ? Component.literal("Radius: Infinite")
                    : Component.literal("Radius: %.1f blocks".formatted(range));

            lore = new ItemLore(List.of(
                    loreIdComponent,
                    speakerRadius.withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY)
            ));

        } else {
            lore = new ItemLore(List.of(
                    loreIdComponent
            ));
        }

        device.serialiseIntoItemStack(stack);

        stack.set(DataComponents.LORE, lore);
        stack.set(DataComponents.CUSTOM_NAME, nameComponent);

        ResolvableProfile resolvableProfile = new ResolvableProfile(gameProfile);
        stack.set(DataComponents.PROFILE, resolvableProfile);


        return Optional.of(stack);
    }

    private static final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    private static GameProfile getGameProfile(UUID uuid, String name, String skinUrl) {
        GameProfile gameProfile = new GameProfile(uuid, name);
        PropertyMap properties = gameProfile.getProperties();

        List<Property> textures = new ArrayList<>();

        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textureMap = new HashMap<>();
        textureMap.put(MinecraftProfileTexture.Type.SKIN, new MinecraftProfileTexture(skinUrl, null));

        String json = gson.toJson(new MinecraftTexturesPayload(textureMap));
        String base64Payload = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        textures.add(new Property("textures", base64Payload));
        properties.putAll("textures", textures);

        return gameProfile;
    }

    private static class MinecraftTexturesPayload {

        private final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures;

        public MinecraftTexturesPayload(Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures) {
            this.textures = textures;
        }

        public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures() {
            return textures;
        }
    }

}
