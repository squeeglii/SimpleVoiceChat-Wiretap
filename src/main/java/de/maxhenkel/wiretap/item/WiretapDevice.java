package de.maxhenkel.wiretap.item;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.UUID;

public abstract class WiretapDevice {

    public static final float INFINITE_RANGE = -1.0f;

    public static final Codec<WiretapDevice> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    UUIDUtil.CODEC.fieldOf("pair_id").forGetter(WiretapDevice::getPairUUID),
                    Codec.BOOL.fieldOf("is_speaker").forGetter(WiretapDevice::isSpeaker),
                    Codec.FLOAT.optionalFieldOf("speaker_range", INFINITE_RANGE).forGetter(WiretapDevice::getSerialisationRange)
            ).apply(builder, WiretapDevice::createDeviceData)
    );

    private final DeviceType deviceType;
    private final UUID pairUUID;

    protected WiretapDevice(DeviceType deviceType, UUID pairUUID) {
        this.deviceType = deviceType;
        this.pairUUID = pairUUID;
    }

    public UUID getPairUUID() {
        return this.pairUUID;
    }

    public DeviceType getDeviceType() {
        return this.deviceType;
    }

    public boolean isSpeaker() {
        return this.deviceType == DeviceType.SPEAKER;
    }

    protected Float getSerialisationRange() {
        return null;
    }

    public Codec<WiretapDevice> getSerialisationCodec() {
        return CODEC;
    }

    public void serialiseIntoItemStack(ItemStack item) {
        // 1.20.1 -- instead of saving to item directly with tags, save to BLOCK ENTITY TAG inside of item.
        // this /should/ copy all the item data inside that section to the block entity.

        CompoundTag blockEntityData = this.saveToNewCustomData(null); // this makes a new tag anyway.
        PlayerHeadItem.setBlockEntityData(item, BlockEntityType.SKULL, blockEntityData);
    }

    public CompoundTag saveToNewCustomData() {
        return this.saveToNewCustomData(null);
    }

    public CompoundTag saveToNewCustomData(CompoundTag mergeWith) {
        CompoundTag workingTag = mergeWith == null
                ? new CompoundTag()
                : mergeWith.copy();

        workingTag.put(HeadUtils.NBT_DEVICE, this.getSerialisationCodec().encodeStart(NbtOps.INSTANCE, this).getOrThrow(false, Wiretap.LOGGER::error));

        return workingTag;
    }

    public static WiretapDevice createDeviceData(UUID pairUUID, boolean isSpeaker) {
        return createDeviceData(pairUUID, isSpeaker, null);
    }

    public static WiretapDevice createDeviceData(UUID pairUUID, boolean isSpeaker, Float range) {
        return isSpeaker
                ? new SpeakerDevice(pairUUID, range)
                : new MicrophoneDevice(pairUUID);
    }

    @Override
    public String toString() {
        return "WiretapDevice{" +
                "deviceType=" + deviceType +
                ", pairUUID=" + pairUUID +
                ", range?=" + this.getSerialisationRange() +
                '}';
    }
}
