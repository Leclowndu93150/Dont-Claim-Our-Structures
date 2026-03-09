package com.leclowndu93150.dont_touch_our_structures.mixin.opac;

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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.pac.common.claims.ClaimsManager;
import xaero.pac.common.claims.player.PlayerChunkClaim;
import xaero.pac.common.claims.result.api.AreaClaimResult;
import xaero.pac.common.claims.result.api.ClaimResult;
import xaero.pac.common.server.claims.ServerClaimsManager;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mixin(value = ServerClaimsManager.class, remap = false)
public abstract class ServerClaimsManagerMixin {

    @Inject(
            method = "tryToClaimTyped(Lnet/minecraft/resources/ResourceLocation;Ljava/util/UUID;IIIIIZ)Lxaero/pac/common/claims/result/api/ClaimResult;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void dtos_interceptSingleClaim(
            ResourceLocation dimension, UUID playerId, int subConfigIndex,
            int fromX, int fromZ, int x, int z, boolean replace,
            CallbackInfoReturnable<ClaimResult<PlayerChunkClaim>> cir
    ) {
        if (!ModConfig.opacEnabled || !ModConfig.opacUseMixin) {
            return;
        }

        MinecraftServer server = getServer();
        if (server == null) {
            return;
        }

        ServerLevel level = server.getLevel(dimensionToResourceKey(dimension));
        if (level == null) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(x, z);
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);

        ProtectionResult result = StructureProtectionManager.getInstance().checkProtection(level, chunkPos, player);

        if (result.isProtected()) {
            if (player != null) {
                player.sendSystemMessage(Component.literal(result.getDenialMessage(chunkPos)));
            }
            cir.setReturnValue(new ClaimResult<>(null, ClaimResult.Type.ALREADY_CLAIMED));
        }
    }

    @Inject(
            method = "tryClaimActionOverArea",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void dtos_interceptAreaClaim(
            ResourceLocation dimension, UUID playerId, int subConfigIndex, int fromX, int fromZ, int left, int top, int right, int bottom, ClaimsManager.Action action, boolean replace, CallbackInfoReturnable<AreaClaimResult> cir
    ) {
        if (!ModConfig.opacEnabled || !ModConfig.opacUseMixin) {
            return;
        }

        if (!action.toString().equals("CLAIM")) {
            return;
        }

        MinecraftServer server = getServer();
        if (server == null) {
            return;
        }

        ServerLevel level = server.getLevel(dimensionToResourceKey(dimension));
        if (level == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);

        Set<ClaimResult.Type> blockedResults = new HashSet<>();
        boolean hasProtectedChunks = false;
        String lastDenialMessage = null;

        for (int cx = left; cx <= right; cx++) {
            for (int cz = top; cz <= bottom; cz++) {
                ChunkPos chunkPos = new ChunkPos(cx, cz);
                ProtectionResult result = StructureProtectionManager.getInstance().checkProtection(level, chunkPos, player);

                if (result.isProtected()) {
                    hasProtectedChunks = true;
                    lastDenialMessage = result.getDenialMessage(chunkPos);
                    blockedResults.add(ClaimResult.Type.ALREADY_CLAIMED);
                }
            }
        }

        if (hasProtectedChunks) {
            if (player != null && lastDenialMessage != null) {
                player.sendSystemMessage(Component.literal(lastDenialMessage));
            }
            cir.setReturnValue(new AreaClaimResult(blockedResults, left, top, right, bottom));
        }
    }

    private MinecraftServer getServer() {
        try {
            Field managerField = ServerClaimsManager.class.getSuperclass().getDeclaredField("playerClaimInfoManager");
            managerField.setAccessible(true);
            Object playerInfoManager = managerField.get(this);

            if (playerInfoManager != null) {
                Field serverField = playerInfoManager.getClass().getDeclaredField("server");
                serverField.setAccessible(true);
                return (MinecraftServer) serverField.get(playerInfoManager);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ResourceKey<Level> dimensionToResourceKey(ResourceLocation dimension) {
        return ResourceKey.create(Registries.DIMENSION, dimension);
    }
}
