package de.maxhenkel.wiretap.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record SpeakerComponent(UUID pairUUID, Float range) {

    public static final float INFINITE_RANGE = -1.0f;

    public static final Codec<SpeakerComponent> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    UUIDUtil.CODEC.fieldOf("pair_id").forGetter(SpeakerComponent::pairUUID),
                    Codec.FLOAT.optionalFieldOf("range", INFINITE_RANGE).forGetter(SpeakerComponent::range)
            ).apply(builder, SpeakerComponent::new)
    );

}
