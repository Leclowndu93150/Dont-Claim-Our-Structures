package com.leclowndu93150.dont_touch_our_structures;

import com.leclowndu93150.dont_touch_our_structures.command.DTOSCommand;
import com.leclowndu93150.dont_touch_our_structures.compat.FTBChunks.FTBChunksCompat;
import com.leclowndu93150.dont_touch_our_structures.compat.OPaC.OPaCCompat;
import com.leclowndu93150.dont_touch_our_structures.config.ModConfig;
import com.leclowndu93150.dont_touch_our_structures.protection.StructureProtectionManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DontClaimOurStructures.MODID)
public class DontClaimOurStructures {
    public static final String MODID = "dont_touch_our_structures";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public DontClaimOurStructures(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                ModConfig.SPEC,
                "dont_touch_our_structures-common.toml"
        );
        NeoForge.EVENT_BUS.register(this);
        registerCompatModules();
        LOGGER.info("Don't Claim Our Structures initialized");
    }

    private void registerCompatModules() {
        if (ModList.get().isLoaded("ftbchunks")) {
            LOGGER.info("FTB Chunks detected, registering compatibility");
            FTBChunksCompat.register();
        }
        if (ModList.get().isLoaded("openpartiesandclaims")) {
            LOGGER.info("Open Parties and Claims detected, registering compatibility");
            OPaCCompat.register();
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        StructureProtectionManager.getInstance().init(event.getServer());
        LOGGER.info("Structure protection manager initialized");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        StructureProtectionManager.getInstance().shutdown();
        LOGGER.info("Structure protection manager shut down");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DTOSCommand.register(event.getDispatcher());
    }
}
