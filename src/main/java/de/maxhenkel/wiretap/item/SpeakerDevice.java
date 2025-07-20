package de.maxhenkel.wiretap.item;

import de.maxhenkel.wiretap.Wiretap;
import de.maxhenkel.wiretap.wiretap.DeviceType;

import java.util.UUID;

public class SpeakerDevice extends WiretapDevice {

    protected boolean usingRangeOverride;
    protected float rangeOverride;

    public SpeakerDevice(UUID pairUUID, Float rangeOverride) {
        super(DeviceType.SPEAKER, pairUUID);

        if(rangeOverride != null) {
            this.rangeOverride = rangeOverride;
            this.usingRangeOverride = true;
        } else {
            this.usingRangeOverride = false;
        }
    }

    @Override
    protected Float getSerialisationRange() {
        return this.getActiveRange();
    }

    // If override exists, use that. If not, pass on the config-defined range.
    public float getActiveRange() {
        return this.usingRangeOverride
                ? this.rangeOverride
                : Wiretap.SERVER_CONFIG.speakerAudioRange.get().floatValue();
    }

    public boolean isUsingRangeOverride() {
        return this.usingRangeOverride;
    }
}
