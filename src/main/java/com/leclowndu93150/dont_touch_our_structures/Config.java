package com.leclowndu93150.dont_touch_our_structures;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = DontTouchOurStructures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_STRUCTURES = BUILDER
            .comment("A list of structures that can be claimed using FTB Chunks")
            .defineListAllowEmpty("allowed_structures",
                    List.of(),
                    Config::validateStructureName);

    private static final ForgeConfigSpec.BooleanValue AFFECT_SINGLEPLAYER = BUILDER
            .comment("Whether the mod should affect singleplayer worlds")
            .define("affect_singleplayer", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<ResourceLocation> allowedStructures;
    public static boolean affectSingleplayer;

    private static boolean validateStructureName(final Object obj) {
        if (!(obj instanceof String structureName)) return false;
        try {
            new ResourceLocation(structureName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        affectSingleplayer = AFFECT_SINGLEPLAYER.get();
        allowedStructures = ALLOWED_STRUCTURES.get().stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toSet());
    }
}