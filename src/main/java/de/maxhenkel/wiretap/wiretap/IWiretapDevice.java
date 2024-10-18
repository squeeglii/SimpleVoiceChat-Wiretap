package de.maxhenkel.wiretap.wiretap;

import java.util.UUID;

public interface IWiretapDevice {

    DeviceType wiretap$getDeviceType();
    UUID wiretap$getPairId();

}
