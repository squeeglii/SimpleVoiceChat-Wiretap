package de.maxhenkel.wiretap;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.wiretap.command.WiretapCommands;
import de.maxhenkel.wiretap.config.ServerConfig;
import de.maxhenkel.wiretap.events.LifecycleEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Wiretap implements ModInitializer {

    public static final String MODID = "wiretap";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static ServerConfig SERVER_CONFIG;

    @Override
    public void onInitialize() {
        SERVER_CONFIG = ConfigBuilder.build(FabricLoader.getInstance().getConfigDir().resolve(MODID).resolve("wiretap-server.properties"), ServerConfig::new);

        ServerLifecycleEvents.SERVER_STOPPING.register(LifecycleEvents::onServerStopping);
        CommandRegistrationCallback.EVENT.register(WiretapCommands::register);
        ServerPlayConnectionEvents.DISCONNECT.register(LifecycleEvents::onDisconnect);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
