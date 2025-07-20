package de.maxhenkel.wiretap.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.maxhenkel.wiretap.utils.HeadUtils;
import de.maxhenkel.wiretap.wiretap.DeviceType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public class MicrophoneDevice extends WiretapDevice {

    public static final Codec<WiretapDevice> MICROPHONE_CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    UUIDUtil.CODEC.fieldOf("pair_id").forGetter(WiretapDevice::getPairUUID),
                    Codec.BOOL.fieldOf("is_speaker").forGetter(WiretapDevice::isSpeaker)
            ).apply(builder, WiretapDevice::createDeviceData)
    );

    public MicrophoneDevice(UUID pairUUID) {
        super(DeviceType.MICROPHONE, pairUUID);
    }

    @Override
    public Codec<WiretapDevice> getSerialisationCodec() {
        return MICROPHONE_CODEC;
    }
}
