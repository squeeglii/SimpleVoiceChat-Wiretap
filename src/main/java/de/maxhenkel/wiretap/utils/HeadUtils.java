package de.maxhenkel.wiretap.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.item.WiretapDataComponents;
import de.maxhenkel.wiretap.item.component.MicrophoneComponent;
import de.maxhenkel.wiretap.item.component.SpeakerComponent;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import de.maxhenkel.wiretap.wiretap.IRangeOverridable;
import de.maxhenkel.wiretap.wiretap.IWiretapDevice;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class HeadUtils {

    public static final String NBT_DEVICE = "wiretap_data";

    public static final String NBT_IS_SPEAKER = "is_speaker";
    public static final String NBT_PAIR_ID = "pair_id";
    public static final String NBT_SPEAKER_RANGE = "speaker_range";

    public static final String MICROPHONE = "wiretap_mic";
    public static final String SPEAKER = "wiretap_speaker";

    public static ItemStack createMicrophone(UUID id) {
        ItemStack stack = createHead("Microphone", id, MICROPHONE, Wiretap.SERVER_CONFIG.microphoneSkinUrl.get());
        stack.set(WiretapDataComponents.MICROPHONE, new MicrophoneComponent(id));
        return stack;
    }

    public static ItemStack createSpeaker(UUID id) {
        return HeadUtils.createSpeaker(id, null);
    }

    public static ItemStack createSpeaker(UUID id, Float range) {
        ItemStack stack = createHead("Speaker", id, SPEAKER, Wiretap.SERVER_CONFIG.speakerSkinUrl.get());
        stack.set(WiretapDataComponents.SPEAKER, new SpeakerComponent(id, range));
        return stack;
    }

    public static ItemStack createHead(String itemName, UUID deviceId, String name, String skinUrl) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);

        GameProfile gameProfile = getGameProfile(deviceId, name, skinUrl);

        MutableComponent loreComponent = Component.literal("ID: %s".formatted(deviceId.toString())).withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY);
        MutableComponent nameComponent = Component.literal(itemName).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.WHITE));

        ItemLore lore = new ItemLore(List.of(loreComponent));

        stack.set(DataComponents.LORE, lore);
        stack.set(DataComponents.ITEM_NAME, nameComponent);
        stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE); // ???

        ResolvableProfile resolvableProfile = new ResolvableProfile(gameProfile);
        stack.set(DataComponents.PROFILE, resolvableProfile);

        return stack;
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
