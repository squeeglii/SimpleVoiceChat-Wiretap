package de.maxhenkel.wiretap.wiretap;

import de.maxhenkel.wiretap.item.WiretapDevice;

import java.util.Optional;
import java.util.UUID;

public interface IWiretapDeviceHolder {

    DeviceType wiretap$getDeviceType();
    UUID wiretap$getPairId();

    Optional<WiretapDevice> wiretap$getDeviceData();

}
