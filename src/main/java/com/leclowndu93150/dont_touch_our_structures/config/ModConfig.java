package com.leclowndu93150.dont_touch_our_structures.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = "dont_touch_our_structures", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue AFFECT_SINGLEPLAYER = BUILDER
            .comment("Whether the mod should affect singleplayer worlds")
            .define("general.affect_singleplayer", false);

    private static final ForgeConfigSpec.BooleanValue BYPASS_WITH_OP = BUILDER
            .comment("Whether players with OP permission level can bypass restrictions")
            .define("general.bypass_with_op", true);

    private static final ForgeConfigSpec.IntValue OP_BYPASS_LEVEL = BUILDER
            .comment("Minimum OP level required to bypass restrictions (if bypass_with_op is true)")
            .defineInRange("general.op_bypass_level", 2, 1, 4);

    private static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING = BUILDER
            .comment("Enable debug logging for troubleshooting")
            .define("general.enable_debug_logging", false);

    private static final ForgeConfigSpec.BooleanValue PROTECT_SURFACE_STRUCTURES = BUILDER
            .comment("Whether to protect surface structures (those with terrain adaptation like BURY, BEARD_THIN, BEARD_BOX)")
            .define("structure_types.protect_surface", true);

    private static final ForgeConfigSpec.BooleanValue PROTECT_UNDERGROUND_STRUCTURES = BUILDER
            .comment("Whether to protect underground structures (those with NONE terrain adaptation)")
            .define("structure_types.protect_underground", false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TERRAIN_ADAPTATIONS_TO_PROTECT = BUILDER
            .comment("List of terrain adaptations to consider as 'surface' structures",
                    "Valid values: NONE, BURY, BEARD_THIN, BEARD_BOX")
            .defineListAllowEmpty("structure_types.terrain_adaptations_to_protect",
                    List.of("BURY", "BEARD_THIN", "BEARD_BOX"), ModConfig::validateTerrainAdaptation);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> GENERATION_STEPS_TO_PROTECT = BUILDER
            .comment("List of generation steps to protect",
                    "Valid values: RAW_GENERATION, LAKES, LOCAL_MODIFICATIONS, UNDERGROUND_STRUCTURES,",
                    "SURFACE_STRUCTURES, STRONGHOLDS, UNDERGROUND_ORES, UNDERGROUND_DECORATION,",
                    "FLUID_SPRINGS, VEGETAL_DECORATION, TOP_LAYER_MODIFICATION")
            .defineListAllowEmpty("structure_types.generation_steps_to_protect",
                    List.of("SURFACE_STRUCTURES"), ModConfig::validateGenerationStep);

    private static final ForgeConfigSpec.EnumValue<FilterMode> STRUCTURE_FILTER_MODE = BUILDER
            .comment("How to use the structure lists:",
                    "WHITELIST - only listed structures are protected",
                    "BLACKLIST - all structures except listed ones are protected",
                    "DISABLED - ignore the structure list entirely")
            .defineEnum("structures.filter_mode", FilterMode.BLACKLIST);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_WHITELIST = BUILDER
            .comment("Structures to protect when filter_mode is WHITELIST")
            .defineListAllowEmpty("structures.whitelist", List.of(), ModConfig::validateResourceLocation);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLACKLIST = BUILDER
            .comment("Structures to exclude from protection when filter_mode is BLACKLIST")
            .defineListAllowEmpty("structures.blacklist", List.of(), ModConfig::validateResourceLocation);

    private static final ForgeConfigSpec.EnumValue<FilterMode> DIMENSION_FILTER_MODE = BUILDER
            .comment("How to filter dimensions:",
                    "WHITELIST - only protect structures in listed dimensions",
                    "BLACKLIST - protect structures in all dimensions except listed ones",
                    "DISABLED - no dimension filtering")
            .defineEnum("dimensions.filter_mode", FilterMode.DISABLED);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_WHITELIST = BUILDER
            .comment("Dimensions where protection is active (when filter_mode is WHITELIST)",
                    "Example: [\"minecraft:overworld\", \"minecraft:the_nether\"]")
            .defineListAllowEmpty("dimensions.whitelist", List.of("minecraft:overworld"), ModConfig::validateResourceLocation);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_BLACKLIST = BUILDER
            .comment("Dimensions where protection is disabled (when filter_mode is BLACKLIST)")
            .defineListAllowEmpty("dimensions.blacklist", List.of(), ModConfig::validateResourceLocation);

    private static final ForgeConfigSpec.EnumValue<FilterMode> BIOME_FILTER_MODE = BUILDER
            .comment("How to filter biomes:",
                    "WHITELIST - only protect structures in listed biomes",
                    "BLACKLIST - protect structures in all biomes except listed ones",
                    "DISABLED - no biome filtering")
            .defineEnum("biomes.filter_mode", FilterMode.DISABLED);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BIOME_WHITELIST = BUILDER
            .comment("Biomes where protection is active (when filter_mode is WHITELIST)")
            .defineListAllowEmpty("biomes.whitelist", List.of(), ModConfig::validateResourceLocation);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BIOME_BLACKLIST = BUILDER
            .comment("Biomes where protection is disabled (when filter_mode is BLACKLIST)")
            .defineListAllowEmpty("biomes.blacklist", List.of(), ModConfig::validateResourceLocation);

    private static final ForgeConfigSpec.BooleanValue ENABLE_PADDING = BUILDER
            .comment("Whether to add padding around structures where claiming is also prevented")
            .define("padding.enable", false);

    private static final ForgeConfigSpec.IntValue DEFAULT_PADDING_CHUNKS = BUILDER
            .comment("Default number of chunks to pad around protected structures (0-16)")
            .defineInRange("padding.default_chunks", 1, 0, 16);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_STRUCTURE_PADDING = BUILDER
            .comment("Custom padding for specific structures (Format: 'modid:structure_id=paddingValue')",
                    "Example: [\"minecraft:village_plains=2\", \"minecraft:monument=3\"]")
            .defineListAllowEmpty("padding.custom_structure_padding", List.of(), ModConfig::validateCustomPadding);

    private static final ForgeConfigSpec.ConfigValue<String> CUSTOM_DENIAL_MESSAGE = BUILDER
            .comment("Custom denial message. Leave empty to auto-generate based on structure info.",
                    "Placeholders: {structure_name}, {structure_type}, {chunk_x}, {chunk_z}")
            .define("messages.custom_denial_message", "");

    private static final ForgeConfigSpec.BooleanValue INCLUDE_STRUCTURE_NAME = BUILDER
            .comment("Include the structure name in auto-generated denial messages")
            .define("messages.include_structure_name", true);

    private static final ForgeConfigSpec.BooleanValue INCLUDE_STRUCTURE_TYPE = BUILDER
            .comment("Include the structure type (surface/underground) in auto-generated denial messages")
            .define("messages.include_structure_type", true);

    private static final ForgeConfigSpec.BooleanValue INCLUDE_COORDINATES = BUILDER
            .comment("Include chunk coordinates in auto-generated denial messages")
            .define("messages.include_coordinates", false);

    private static final ForgeConfigSpec.BooleanValue ENABLE_CACHE = BUILDER
            .comment("Enable caching of protection checks for better performance")
            .define("performance.enable_cache", true);

    private static final ForgeConfigSpec.IntValue CACHE_TTL_SECONDS = BUILDER
            .comment("How long cached entries are valid (in seconds)")
            .defineInRange("performance.cache_ttl_seconds", 300, 10, 3600);

    private static final ForgeConfigSpec.IntValue CACHE_MAX_ENTRIES = BUILDER
            .comment("Maximum number of entries in the cache")
            .defineInRange("performance.cache_max_entries", 1000, 100, 10000);

    private static final ForgeConfigSpec.BooleanValue FTB_CHUNKS_ENABLED = BUILDER
            .comment("Enable FTB Chunks integration")
            .define("compat.ftb_chunks_enabled", true);

    private static final ForgeConfigSpec.BooleanValue OPAC_ENABLED = BUILDER
            .comment("Enable Open Parties and Claims integration")
            .define("compat.opac_enabled", true);

    private static final ForgeConfigSpec.BooleanValue OPAC_USE_MIXIN = BUILDER
            .comment("Use mixin injection for OPaC (prevents claims at source, no flicker)",
                    "If false, falls back to reactive listener approach")
            .define("compat.opac_use_mixin", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public enum FilterMode {
        WHITELIST,
        BLACKLIST,
        DISABLED
    }

    public static boolean affectSingleplayer;
    public static boolean bypassWithOp;
    public static int opBypassLevel;
    public static boolean enableDebugLogging;

    public static boolean protectSurfaceStructures;
    public static boolean protectUndergroundStructures;
    public static Set<String> terrainAdaptationsToProtect;
    public static Set<String> generationStepsToProtect;

    public static FilterMode structureFilterMode;
    public static Set<ResourceLocation> structureWhitelist;
    public static Set<ResourceLocation> structureBlacklist;

    public static FilterMode dimensionFilterMode;
    public static Set<ResourceLocation> dimensionWhitelist;
    public static Set<ResourceLocation> dimensionBlacklist;

    public static FilterMode biomeFilterMode;
    public static Set<ResourceLocation> biomeWhitelist;
    public static Set<ResourceLocation> biomeBlacklist;

    public static boolean enablePadding;
    public static int defaultPaddingChunks;
    public static Map<ResourceLocation, Integer> customStructurePadding;

    public static String customDenialMessage;
    public static boolean includeStructureName;
    public static boolean includeStructureType;
    public static boolean includeCoordinates;

    public static boolean enableCache;
    public static int cacheTtlSeconds;
    public static int cacheMaxEntries;

    public static boolean ftbChunksEnabled;
    public static boolean opacEnabled;
    public static boolean opacUseMixin;

    private static boolean validateResourceLocation(final Object obj) {
        if (!(obj instanceof String str)) return false;
        try {
            new ResourceLocation(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateTerrainAdaptation(final Object obj) {
        if (!(obj instanceof String str)) return false;
        return str.equals("NONE") || str.equals("BURY") || str.equals("BEARD_THIN") || str.equals("BEARD_BOX");
    }

    private static boolean validateGenerationStep(final Object obj) {
        if (!(obj instanceof String str)) return false;
        return str.equals("RAW_GENERATION") || str.equals("LAKES") || str.equals("LOCAL_MODIFICATIONS") ||
                str.equals("UNDERGROUND_STRUCTURES") || str.equals("SURFACE_STRUCTURES") || str.equals("STRONGHOLDS") ||
                str.equals("UNDERGROUND_ORES") || str.equals("UNDERGROUND_DECORATION") || str.equals("FLUID_SPRINGS") ||
                str.equals("VEGETAL_DECORATION") || str.equals("TOP_LAYER_MODIFICATION");
    }

    private static boolean validateCustomPadding(final Object obj) {
        if (!(obj instanceof String entry)) return false;
        try {
            String[] parts = entry.split("=");
            if (parts.length != 2) return false;
            new ResourceLocation(parts[0]);
            int padding = Integer.parseInt(parts[1]);
            return padding >= 0 && padding <= 16;
        } catch (Exception e) {
            return false;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        loadConfig();
    }

    public static void loadConfig() {
        affectSingleplayer = AFFECT_SINGLEPLAYER.get();
        bypassWithOp = BYPASS_WITH_OP.get();
        opBypassLevel = OP_BYPASS_LEVEL.get();
        enableDebugLogging = ENABLE_DEBUG_LOGGING.get();

        protectSurfaceStructures = PROTECT_SURFACE_STRUCTURES.get();
        protectUndergroundStructures = PROTECT_UNDERGROUND_STRUCTURES.get();
        terrainAdaptationsToProtect = new HashSet<>(TERRAIN_ADAPTATIONS_TO_PROTECT.get());
        generationStepsToProtect = new HashSet<>(GENERATION_STEPS_TO_PROTECT.get());

        structureFilterMode = STRUCTURE_FILTER_MODE.get();
        structureWhitelist = STRUCTURE_WHITELIST.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());
        structureBlacklist = STRUCTURE_BLACKLIST.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());

        dimensionFilterMode = DIMENSION_FILTER_MODE.get();
        dimensionWhitelist = DIMENSION_WHITELIST.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());
        dimensionBlacklist = DIMENSION_BLACKLIST.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());

        biomeFilterMode = BIOME_FILTER_MODE.get();
        biomeWhitelist = BIOME_WHITELIST.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());
        biomeBlacklist = BIOME_BLACKLIST.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());

        enablePadding = ENABLE_PADDING.get();
        defaultPaddingChunks = DEFAULT_PADDING_CHUNKS.get();
        customStructurePadding = new HashMap<>();
        CUSTOM_STRUCTURE_PADDING.get().forEach(entry -> {
            String[] parts = entry.split("=");
            ResourceLocation structureId = new ResourceLocation(parts[0]);
            int padding = Integer.parseInt(parts[1]);
            customStructurePadding.put(structureId, padding);
        });

        customDenialMessage = CUSTOM_DENIAL_MESSAGE.get();
        includeStructureName = INCLUDE_STRUCTURE_NAME.get();
        includeStructureType = INCLUDE_STRUCTURE_TYPE.get();
        includeCoordinates = INCLUDE_COORDINATES.get();

        enableCache = ENABLE_CACHE.get();
        cacheTtlSeconds = CACHE_TTL_SECONDS.get();
        cacheMaxEntries = CACHE_MAX_ENTRIES.get();

        ftbChunksEnabled = FTB_CHUNKS_ENABLED.get();
        opacEnabled = OPAC_ENABLED.get();
        opacUseMixin = OPAC_USE_MIXIN.get();
    }
}
