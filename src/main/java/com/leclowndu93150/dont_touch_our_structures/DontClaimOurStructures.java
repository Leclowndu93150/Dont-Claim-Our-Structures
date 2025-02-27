package com.leclowndu93150.dont_touch_our_structures;

import com.leclowndu93150.dont_touch_our_structures.compat.FTBChunks.FTBChunksCompat;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
@Mod(DontClaimOurStructures.MODID)
public class DontClaimOurStructures {
    public static final String MODID = "dont_touch_our_structures";

    public DontClaimOurStructures(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if(ModList.get().isLoaded("ftbchunks")){
            FTBChunksCompat.register();
        }
    }

    public static boolean shouldPreventClaim(ServerLevel level, ChunkPos chunkPos) {
        if(level.getLevel().getServer().isSingleplayer() && !Config.affectSingleplayer) {
            return false;
        }

        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        for (Structure structure : structureRegistry) {
            ResourceLocation structureId = structureRegistry.getKey(structure);
            if (structureId == null || Config.allowedStructures.contains(structureId)) {
                continue;
            }

            if ((!Config.onlySurfaceStructures || structure.terrainAdaptation() != TerrainAdjustment.NONE) &&
                    level.structureManager().getAllStructuresAt(chunkPos.getWorldPosition()).containsKey(structure)) {
                return true;
            }
        }

        return false;
    }
}
