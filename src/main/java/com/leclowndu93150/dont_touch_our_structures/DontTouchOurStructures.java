package com.leclowndu93150.dont_touch_our_structures;

import com.leclowndu93150.dont_touch_our_structures.command.DTOSCommand;
import com.leclowndu93150.dont_touch_our_structures.compat.FTBChunks.FTBChunksCompat;
import com.leclowndu93150.dont_touch_our_structures.compat.OPaC.OPaCCompat;
import com.leclowndu93150.dont_touch_our_structures.config.ModConfig;
import com.leclowndu93150.dont_touch_our_structures.protection.StructureProtectionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DontTouchOurStructures.MODID)
public class DontTouchOurStructures {
    public static final String MODID = "dont_touch_our_structures";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public DontTouchOurStructures() {
        ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                ModConfig.SPEC,
                "dont_touch_our_structures-common.toml"
        );
        MinecraftForge.EVENT_BUS.register(this);
        registerCompatModules();
        LOGGER.info("Don't Touch Our Structures initialized");
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
