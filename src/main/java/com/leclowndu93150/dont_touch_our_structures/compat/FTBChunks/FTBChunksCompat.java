package com.leclowndu93150.dont_touch_our_structures.compat.FTBChunks;

import com.leclowndu93150.dont_touch_our_structures.DontTouchOurStructures;
import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

public class FTBChunksCompat {

    public static void register(){
        ClaimedChunkEvent.BEFORE_CLAIM.register(FTBChunksCompat::onBeforeClaim);
    }

    private static CompoundEventResult<ClaimResult> onBeforeClaim(CommandSourceStack source, ClaimedChunk chunk) {
        ChunkPos chunkPos = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());

        if(DontTouchOurStructures.shouldPreventClaim(source.getLevel(), chunkPos, source.getPlayer())) {
            source.sendSystemMessage(Component.literal("Cannot claim this chunk: Contains protected surface structure"));
            return CompoundEventResult.interruptTrue(ClaimResult.customProblem(
                    "Cannot claim this chunk: Contains protected surface structure"
            ));
        }

        return CompoundEventResult.pass();
    }

}
