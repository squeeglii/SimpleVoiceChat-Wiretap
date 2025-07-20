package de.maxhenkel.wiretap.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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
        CustomData data = item.get(DataComponents.CUSTOM_DATA);
        CustomData newData = this.saveToNewCustomData(data);
        item.set(DataComponents.CUSTOM_DATA, newData);
    }

    public CustomData saveToNewCustomData() {
        return this.saveToNewCustomData(null);
    }

    public CustomData saveToNewCustomData(CustomData mergeWith) {
        CompoundTag workingTag = mergeWith == null
                ? new CompoundTag()
                : mergeWith.copyTag();

        workingTag.store(HeadUtils.NBT_DEVICE, this.getSerialisationCodec(), this);

        return CustomData.of(workingTag);
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
