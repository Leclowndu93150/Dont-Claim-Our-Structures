package com.leclowndu93150.dont_touch_our_structures.protection;

import com.leclowndu93150.dont_touch_our_structures.DontClaimOurStructures;
import com.leclowndu93150.dont_touch_our_structures.config.ModConfig;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

public class StructureProtectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DontClaimOurStructures.MODID);
    private static StructureProtectionManager instance;

    private final StructureCache cache;
    private MinecraftServer server;

    private StructureProtectionManager() {
        this.cache = new StructureCache();
    }

    public static StructureProtectionManager getInstance() {
        if (instance == null) {
            instance = new StructureProtectionManager();
        }
        return instance;
    }

    public void init(MinecraftServer server) {
        this.server = server;
        this.cache.clear();
        debugLog("StructureProtectionManager initialized");
    }

    public void shutdown() {
        this.server = null;
        this.cache.clear();
        debugLog("StructureProtectionManager shut down");
    }

    public void reload() {
        ModConfig.loadConfig();
        cache.clear();
        debugLog("Configuration reloaded and cache cleared");
    }

    public StructureCache getCache() {
        return cache;
    }

    public ProtectionResult checkProtection(ServerLevel level, ChunkPos chunkPos, @Nullable ServerPlayer player) {
        if (level.getServer().isSingleplayer() && !ModConfig.affectSingleplayer) {
            debugLog("Singleplayer bypass for chunk [%d, %d]", chunkPos.x, chunkPos.z);
            return ProtectionResult.singleplayerAllowed();
        }

        if (player != null && ModConfig.bypassWithOp && player.hasPermissions(ModConfig.opBypassLevel)) {
            debugLog("OP bypass for player %s at chunk [%d, %d]", player.getName().getString(), chunkPos.x, chunkPos.z);
            return ProtectionResult.bypassed();
        }

        ResourceLocation dimensionId = level.dimension().location();

        ProtectionResult dimensionResult = checkDimensionFilter(dimensionId);
        if (dimensionResult != null) {
            return dimensionResult;
        }

        ProtectionResult cached = cache.get(dimensionId, chunkPos);
        if (cached != null) {
            debugLog("Cache hit for chunk [%d, %d]: %s", chunkPos.x, chunkPos.z, cached.type());
            return cached;
        }

        ProtectionResult result = performProtectionCheck(level, chunkPos);

        cache.put(dimensionId, chunkPos, result);
        debugLog("Protection check for chunk [%d, %d]: %s", chunkPos.x, chunkPos.z, result.type());

        return result;
    }

    @Nullable
    private ProtectionResult checkDimensionFilter(ResourceLocation dimensionId) {
        switch (ModConfig.dimensionFilterMode) {
            case WHITELIST -> {
                if (!ModConfig.dimensionWhitelist.contains(dimensionId)) {
                    debugLog("Dimension %s not in whitelist, allowing claim", dimensionId);
                    return ProtectionResult.allowed();
                }
            }
            case BLACKLIST -> {
                if (ModConfig.dimensionBlacklist.contains(dimensionId)) {
                    debugLog("Dimension %s in blacklist, allowing claim", dimensionId);
                    return ProtectionResult.allowed();
                }
            }
            case DISABLED -> {}
        }
        return null;
    }

    private ProtectionResult performProtectionCheck(ServerLevel level, ChunkPos chunkPos) {
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        ProtectionResult directResult = checkChunkForStructures(level, chunkPos, structureRegistry);
        if (directResult.isProtected()) {
            return directResult;
        }

        if (ModConfig.enablePadding) {
            ProtectionResult paddingResult = checkPadding(level, chunkPos, structureRegistry);
            if (paddingResult.isProtected()) {
                return paddingResult;
            }
        }

        return ProtectionResult.allowed();
    }

    private ProtectionResult checkChunkForStructures(ServerLevel level, ChunkPos chunkPos, Registry<Structure> structureRegistry) {
        Map<Structure, LongSet> structuresAtPos = level.structureManager().getAllStructuresAt(chunkPos.getWorldPosition());

        for (Map.Entry<Structure, LongSet> entry : structuresAtPos.entrySet()) {
            Structure structure = entry.getKey();
            ResourceLocation structureId = structureRegistry.getKey(structure);

            if (structureId == null) {
                continue;
            }

            if (!checkBiomeFilter(level, chunkPos)) {
                continue;
            }

            if (isStructureProtected(structure, structureId)) {
                StructureInfo info = new StructureInfo(
                        structureId,
                        structure.terrainAdaptation(),
                        getGenerationStepName(structure),
                        chunkPos
                );
                return ProtectionResult.protectedStructure(info);
            }
        }

        return ProtectionResult.allowed();
    }

    private boolean isStructureProtected(Structure structure, ResourceLocation structureId) {
        boolean isSurface = structure.terrainAdaptation() != TerrainAdjustment.NONE;

        if (isSurface && !ModConfig.protectSurfaceStructures) {
            return false;
        }
        if (!isSurface && !ModConfig.protectUndergroundStructures) {
            return false;
        }

        switch (ModConfig.structureFilterMode) {
            case WHITELIST -> {
                return ModConfig.structureWhitelist.contains(structureId);
            }
            case BLACKLIST -> {
                return !ModConfig.structureBlacklist.contains(structureId);
            }
            case DISABLED -> {
                return true;
            }
        }

        return false;
    }

    private boolean checkBiomeFilter(ServerLevel level, ChunkPos chunkPos) {
        if (ModConfig.biomeFilterMode == ModConfig.FilterMode.DISABLED) {
            return true;
        }

        Holder<Biome> biomeHolder = level.getBiome(chunkPos.getMiddleBlockPosition(64));
        ResourceLocation biomeId = biomeHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);

        if (biomeId == null) {
            return true;
        }

        switch (ModConfig.biomeFilterMode) {
            case WHITELIST -> {
                return ModConfig.biomeWhitelist.contains(biomeId);
            }
            case BLACKLIST -> {
                return !ModConfig.biomeBlacklist.contains(biomeId);
            }
            default -> {
                return true;
            }
        }
    }

    private ProtectionResult checkPadding(ServerLevel level, ChunkPos targetChunk, Registry<Structure> structureRegistry) {
        int maxPadding = ModConfig.defaultPaddingChunks;

        for (int padding : ModConfig.customStructurePadding.values()) {
            maxPadding = Math.max(maxPadding, padding);
        }

        if (maxPadding <= 0) {
            return ProtectionResult.allowed();
        }

        for (int xOffset = -maxPadding; xOffset <= maxPadding; xOffset++) {
            for (int zOffset = -maxPadding; zOffset <= maxPadding; zOffset++) {
                if (xOffset == 0 && zOffset == 0) {
                    continue;
                }

                ChunkPos structureChunk = new ChunkPos(targetChunk.x + xOffset, targetChunk.z + zOffset);
                Map<Structure, LongSet> structuresAtPos = level.structureManager().getAllStructuresAt(structureChunk.getWorldPosition());

                for (Map.Entry<Structure, LongSet> entry : structuresAtPos.entrySet()) {
                    Structure structure = entry.getKey();
                    ResourceLocation structureId = structureRegistry.getKey(structure);

                    if (structureId == null) {
                        continue;
                    }

                    if (isStructureProtected(structure, structureId)) {
                        int padding = ModConfig.customStructurePadding.getOrDefault(structureId, ModConfig.defaultPaddingChunks);
                        int chunkDistance = Math.max(Math.abs(xOffset), Math.abs(zOffset));

                        if (chunkDistance <= padding) {
                            StructureInfo info = new StructureInfo(
                                    structureId,
                                    structure.terrainAdaptation(),
                                    getGenerationStepName(structure),
                                    structureChunk
                            );
                            return ProtectionResult.protectedPadding(info, structureChunk);
                        }
                    }
                }
            }
        }

        return ProtectionResult.allowed();
    }

    private String getGenerationStepName(Structure structure) {
        return structure.step().name();
    }

    public List<StructureInfo> getStructuresInChunk(ServerLevel level, ChunkPos chunkPos) {
        List<StructureInfo> structures = new ArrayList<>();
        Registry<Structure> structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        Map<Structure, LongSet> structuresAtPos = level.structureManager().getAllStructuresAt(chunkPos.getWorldPosition());

        for (Map.Entry<Structure, LongSet> entry : structuresAtPos.entrySet()) {
            Structure structure = entry.getKey();
            ResourceLocation structureId = structureRegistry.getKey(structure);

            if (structureId != null) {
                structures.add(new StructureInfo(
                        structureId,
                        structure.terrainAdaptation(),
                        getGenerationStepName(structure),
                        chunkPos
                ));
            }
        }

        return structures;
    }

    private void debugLog(String message, Object... args) {
        if (ModConfig.enableDebugLogging) {
            LOGGER.info("[DTOS Debug] " + String.format(message, args));
        }
    }

    public boolean shouldPreventClaim(ServerLevel level, ChunkPos chunkPos, @Nullable ServerPlayer player) {
        return checkProtection(level, chunkPos, player).isProtected();
    }

    public String getDenialMessage(ServerLevel level, ChunkPos chunkPos, @Nullable ServerPlayer player) {
        ProtectionResult result = checkProtection(level, chunkPos, player);
        return result.getDenialMessage(chunkPos);
    }
}
