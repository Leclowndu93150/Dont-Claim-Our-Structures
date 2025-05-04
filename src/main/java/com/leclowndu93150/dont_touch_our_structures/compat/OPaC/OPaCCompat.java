package com.leclowndu93150.dont_touch_our_structures.compat.OPaC;

import com.leclowndu93150.dont_touch_our_structures.DontClaimOurStructures;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.tracker.api.IClaimsManagerListenerAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;

import java.util.UUID;

public class OPaCCompat {

    private static boolean registered = false;

    public static void register() {
        NeoForge.EVENT_BUS.register(OPaCCompat.class);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!registered) {
            MinecraftServer server = event.getServer();
            OpenPACServerAPI api = OpenPACServerAPI.get(server);
            api.getServerClaimsManager().getTracker().register(new ProtectedStructureClaimListener(server));
            registered = true;
        }
    }

    private static class ProtectedStructureClaimListener implements IClaimsManagerListenerAPI {
        private final MinecraftServer server;

        public ProtectedStructureClaimListener(MinecraftServer server) {
            this.server = server;
        }

        public static ResourceKey<Level> dimensionToResourceKey(ResourceLocation dimension) {
            return ResourceKey.create(Registries.DIMENSION, dimension);
        }

        @Override
        public void onChunkChange(ResourceLocation dimension, int chunkX, int chunkZ, IPlayerChunkClaimAPI claim) {
            if (claim != null) {
                ServerLevel level = server.getLevel(dimensionToResourceKey(dimension));
                if (level != null) {
                    ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

                    if (DontClaimOurStructures.shouldPreventClaim(level, chunkPos, server.getPlayerList().getPlayer(claim.getPlayerId()))) {
                        UUID playerUUID = claim.getPlayerId();

                        OpenPACServerAPI api = OpenPACServerAPI.get(server);
                        api.getServerClaimsManager().unclaim(dimension, chunkX, chunkZ);

                        ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                        if (player != null) {
                            player.sendSystemMessage(Component.literal("Cannot claim this chunk: Contains protected surface structure"));
                        }
                    }
                }
            }
        }

        @Override
        public void onWholeRegionChange(ResourceLocation dimension, int regionX, int regionZ) {

        }

        @Override
        public void onDimensionChange(ResourceLocation dimension) {

        }
    }
}
