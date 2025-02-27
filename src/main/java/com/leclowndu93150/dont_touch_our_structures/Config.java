package com.leclowndu93150.dont_touch_our_structures;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = DontClaimOurStructures.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWED_STRUCTURES = BUILDER
            .comment("A list of structures that can be claimed using FTB Chunks")
            .defineListAllowEmpty("allowed_structures",
                    List.of(),
                    Config::validateStructureName);

    private static final ModConfigSpec.BooleanValue AFFECT_SINGLEPLAYER = BUILDER
            .comment("Whether the mod should affect singleplayer worlds")
            .define("affect_singleplayer", false);

    private static final ModConfigSpec.BooleanValue ONLY_SURFACE_STRUCTURES = BUILDER
            .comment("Whether the mod only affect surface structures")
            .define("onlySurfaceStructures", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static Set<ResourceLocation> allowedStructures;
    public static boolean affectSingleplayer;
    public static boolean onlySurfaceStructures;

    private static boolean validateStructureName(final Object obj) {
        if (!(obj instanceof String structureName)) return false;
        try {
            ResourceLocation.tryParse(structureName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        onlySurfaceStructures = ONLY_SURFACE_STRUCTURES.get();
        affectSingleplayer = AFFECT_SINGLEPLAYER.get();
        allowedStructures = ALLOWED_STRUCTURES.get().stream()
                .map(ResourceLocation::tryParse)
                .collect(Collectors.toSet());
    }
}