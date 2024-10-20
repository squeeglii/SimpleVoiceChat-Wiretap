package de.maxhenkel.wiretap.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record MicrophoneComponent(UUID pairUUID) {

    public static final Codec<MicrophoneComponent> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    UUIDUtil.CODEC.fieldOf("pair_id").forGetter(MicrophoneComponent::pairUUID)
            ).apply(builder, MicrophoneComponent::new)
    );


}
