package de.maxhenkel.wiretap.item;

import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.item.component.MicrophoneComponent;
import de.maxhenkel.wiretap.item.component.SpeakerComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class WiretapDataComponents {

    public static final DataComponentType<SpeakerComponent> SPEAKER = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(Wiretap.MODID, "speaker"),
            DataComponentType.<SpeakerComponent>builder().persistent(SpeakerComponent.CODEC).networkSynchronized(SpeakerComponent.STREAM_CODEC).cacheEncoding().build()
    );

    public static final DataComponentType<MicrophoneComponent> MICROPHONE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(Wiretap.MODID, "microphone"),
            DataComponentType.<MicrophoneComponent>builder().persistent(MicrophoneComponent.CODEC).networkSynchronized(MicrophoneComponent.STREAM_CODEC).cacheEncoding().build()
    );

    public static void init() {
        Wiretap.LOGGER.info("Registering data components");
    }

}
