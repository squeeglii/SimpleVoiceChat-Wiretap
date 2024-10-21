package de.maxhenkel.wiretap.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record SpeakerComponent(UUID pairUUID, Float range) {

    public static final float INFINITE_RANGE = -1.0f;

    public static final Codec<SpeakerComponent> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    UUIDUtil.CODEC.fieldOf("pair_id").forGetter(SpeakerComponent::pairUUID),
                    Codec.FLOAT.optionalFieldOf("range", INFINITE_RANGE).forGetter(SpeakerComponent::range)
            ).apply(builder, SpeakerComponent::new)
    );

    public static final StreamCodec<ByteBuf, SpeakerComponent> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SpeakerComponent::pairUUID,
            ByteBufCodecs.FLOAT,
            SpeakerComponent::range,
            SpeakerComponent::new
    );

}
