package com.leclowndu93150.dont_touch_our_structures.compat.OPaC;

import com.leclowndu93150.dont_touch_our_structures.DontTouchOurStructures;
import com.leclowndu93150.dont_touch_our_structures.config.ModConfig;
import com.leclowndu93150.dont_touch_our_structures.protection.ProtectionResult;
import com.leclowndu93150.dont_touch_our_structures.protection.StructureProtectionManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.tracker.api.IClaimsManagerListenerAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;

import java.util.UUID;

public class OPaCCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger(DontTouchOurStructures.MODID);
    private static boolean listenerRegistered = false;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(OPaCCompat.class);
        LOGGER.info("OPaC compatibility registered");
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!ModConfig.opacEnabled) {
            return;
        }

        if (!ModConfig.opacUseMixin && !listenerRegistered) {
            MinecraftServer server = event.getServer();
            OpenPACServerAPI api = OpenPACServerAPI.get(server);
            api.getServerClaimsManager().getTracker().register(new ProtectedStructureClaimListener(server));
            listenerRegistered = true;
            LOGGER.info("OPaC fallback listener registered (mixin disabled)");
        } else if (ModConfig.opacUseMixin) {
            LOGGER.info("OPaC mixin-based protection active");
        }
    }

    private static class ProtectedStructureClaimListener implements IClaimsManagerListenerAPI {
        private final MinecraftServer server;

        public ProtectedStructureClaimListener(MinecraftServer server) {
            this.server = server;
        }

        private static ResourceKey<Level> dimensionToResourceKey(ResourceLocation dimension) {
            return ResourceKey.create(Registries.DIMENSION, dimension);
        }

        @Override
        public void onChunkChange(ResourceLocation dimension, int chunkX, int chunkZ, IPlayerChunkClaimAPI claim) {
            if (claim == null) {
                return;
            }

            ServerLevel level = server.getLevel(dimensionToResourceKey(dimension));
            if (level == null) {
                return;
            }

            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            UUID playerUUID = claim.getPlayerId();
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);

            ProtectionResult result = StructureProtectionManager.getInstance().checkProtection(level, chunkPos, player);

            if (result.isProtected()) {
                OpenPACServerAPI api = OpenPACServerAPI.get(server);
                api.getServerClaimsManager().unclaim(dimension, chunkX, chunkZ);

                if (player != null) {
                    player.sendSystemMessage(Component.literal(result.getDenialMessage(chunkPos)));
                }

                if (ModConfig.enableDebugLogging) {
                    LOGGER.info("[DTOS] Unclaimed protected chunk [{}, {}] in {} for player {}",
                            chunkX, chunkZ, dimension, playerUUID);
                }
            }
        }

        @Override
        public void onWholeRegionChange(ResourceLocation dimension, int regionX, int regionZ) {
            if (!ModConfig.opacEnabled) {
                return;
            }

            ServerLevel level = server.getLevel(dimensionToResourceKey(dimension));
            if (level == null) {
                return;
            }

            OpenPACServerAPI api = OpenPACServerAPI.get(server);

            int startChunkX = regionX << 5;
            int startChunkZ = regionZ << 5;

            for (int cx = 0; cx < 32; cx++) {
                for (int cz = 0; cz < 32; cz++) {
                    int chunkX = startChunkX + cx;
                    int chunkZ = startChunkZ + cz;

                    var claim = api.getServerClaimsManager().get(dimension, chunkX, chunkZ);
                    if (claim != null) {
                        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                        ServerPlayer player = server.getPlayerList().getPlayer(claim.getPlayerId());

                        ProtectionResult result = StructureProtectionManager.getInstance().checkProtection(level, chunkPos, player);

                        if (result.isProtected()) {
                            api.getServerClaimsManager().unclaim(dimension, chunkX, chunkZ);

                            if (player != null) {
                                player.sendSystemMessage(Component.literal(result.getDenialMessage(chunkPos)));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onDimensionChange(ResourceLocation dimension) {
            if (ModConfig.enableDebugLogging) {
                LOGGER.info("[DTOS] Dimension change detected for {}", dimension);
            }
        }
    }
}
