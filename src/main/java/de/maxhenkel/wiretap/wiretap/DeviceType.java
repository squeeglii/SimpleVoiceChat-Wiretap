package de.maxhenkel.wiretap.wiretap;

import de.maxhenkel.wiretap.Wiretap;

import java.util.function.Supplier;

public enum DeviceType {

    MICROPHONE("Microphone", "wiretap_mic", Wiretap.SERVER_CONFIG.microphoneSkinUrl::get),
    SPEAKER("Speaker", "wiretap_speaker", Wiretap.SERVER_CONFIG.speakerSkinUrl::get),
    NON_WIRETAP("Broken Wiretap Device", "wiretap_unrecognised", () -> "");

    private final String displayName;
    private final String internalName;
    private final Supplier<String> skinURLGetter;

    DeviceType(String displayName, String internalName, Supplier<String> skinURLGetter) {
        this.displayName = displayName;
        this.internalName = internalName;
        this.skinURLGetter = skinURLGetter;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getInternalName() {
        return this.internalName;
    }

    public String getSkinURL() {
        return this.skinURLGetter.get();
    }
}
