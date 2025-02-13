package com.leclowndu93150.dont_touch_our_structures;

import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(DontTouchOurStructures.MODID)
public class DontTouchOurStructures {
    public static final String MODID = "dont_touch_our_structures";

    public DontTouchOurStructures() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ClaimedChunkEvent.BEFORE_CLAIM.register(this::onBeforeClaim);
    }

    private CompoundEventResult<ClaimResult> onBeforeClaim(CommandSourceStack source, ClaimedChunk chunk) {
        ServerLevel level = source.getLevel();
        if(source.getServer().isSingleplayer() && !Config.affectSingleplayer){
            return CompoundEventResult.pass();
        }

        ChunkPos chunkPos = new ChunkPos(chunk.getPos().x(), chunk.getPos().z());
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        for (Structure structure : structureRegistry) {
            ResourceLocation structureId = structureRegistry.getKey(structure);
            if (structureId == null || Config.allowedStructures.contains(structureId)) {
                continue;
            }

            if (level.structureManager().getAllStructuresAt(chunkPos.getWorldPosition()).containsKey(structure)) {
                source.sendSystemMessage(Component.literal("Cannot claim this chunk: Contains protected structure '" + structureId + "'"));
                return CompoundEventResult.interruptTrue(ClaimResult.customProblem(
                        "Cannot claim this chunk: Contains protected structure '" + structureId + "'"
                ));
            }
        }

        return CompoundEventResult.pass();
    }
}