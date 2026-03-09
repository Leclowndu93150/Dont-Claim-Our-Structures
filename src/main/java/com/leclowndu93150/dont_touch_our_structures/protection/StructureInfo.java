package com.leclowndu93150.dont_touch_our_structures.protection;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

public record StructureInfo(
        ResourceLocation structureId,
        TerrainAdjustment terrainAdaptation,
        String generationStep,
        ChunkPos chunkPos
) {
    public boolean isSurface() {
        return terrainAdaptation != TerrainAdjustment.NONE;
    }

    public String getTypeString() {
        return isSurface() ? "surface" : "underground";
    }

    public String getDisplayName() {
        return structureId.getPath().replace("_", " ");
    }

    public String getFullId() {
        return structureId.toString();
    }
}
