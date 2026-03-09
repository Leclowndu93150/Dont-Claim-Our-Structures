package com.leclowndu93150.dont_touch_our_structures.compat.FTBChunks;

import com.leclowndu93150.dont_touch_our_structures.DontClaimOurStructures;
import com.leclowndu93150.dont_touch_our_structures.config.ModConfig;
import com.leclowndu93150.dont_touch_our_structures.protection.ProtectionResult;
import com.leclowndu93150.dont_touch_our_structures.protection.StructureProtectionManager;
import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FTBChunksCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger(DontClaimOurStructures.MODID);

    public static void register() {
        ClaimedChunkEvent.BEFORE_CLAIM.register(FTBChunksCompat::onBeforeClaim);
        LOGGER.info("FTB Chunks compatibility registered");
    }

    private static CompoundEventResult<ClaimResult> onBeforeClaim(CommandSourceStack source, ClaimedChunk chunk) {
        if (!ModConfig.ftbChunksEnabled) {
            return CompoundEventResult.pass();
        }

        if (source.getLevel() == null) {
            return CompoundEventResult.pass();
        }

        ChunkPos chunkPos = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());
        ServerPlayer player = source.getPlayer();

        ProtectionResult result = StructureProtectionManager.getInstance()
                .checkProtection(source.getLevel(), chunkPos, player);

        if (result.isProtected()) {
            String message = result.getDenialMessage(chunkPos);
            source.sendSystemMessage(Component.literal(message));

            if (ModConfig.enableDebugLogging) {
                LOGGER.info("[DTOS] Blocked FTB Chunks claim at [{}, {}] - {}",
                        chunkPos.x, chunkPos.z, result.type());
            }

            return CompoundEventResult.interruptTrue(ClaimResult.customProblem(message));
        }

        return CompoundEventResult.pass();
    }
}
