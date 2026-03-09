package com.leclowndu93150.dont_touch_our_structures.command;

import com.leclowndu93150.dont_touch_our_structures.protection.ProtectionResult;
import com.leclowndu93150.dont_touch_our_structures.protection.StructureCache;
import com.leclowndu93150.dont_touch_our_structures.protection.StructureInfo;
import com.leclowndu93150.dont_touch_our_structures.protection.StructureProtectionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

public class DTOSCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dtos")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(DTOSCommand::reloadConfig))
                .then(Commands.literal("check")
                        .executes(DTOSCommand::checkCurrentChunk)
                        .then(Commands.argument("pos", ColumnPosArgument.columnPos())
                                .executes(DTOSCommand::checkSpecificChunk)))
                .then(Commands.literal("debug")
                        .executes(DTOSCommand::debugCurrentChunk)
                        .then(Commands.argument("pos", ColumnPosArgument.columnPos())
                                .executes(DTOSCommand::debugSpecificChunk)))
                .then(Commands.literal("cache")
                        .then(Commands.literal("clear")
                                .executes(DTOSCommand::clearCache))
                        .then(Commands.literal("stats")
                                .executes(DTOSCommand::cacheStats)))
        );
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        StructureProtectionManager.getInstance().reload();
        context.getSource().sendSuccess(() ->
                Component.literal("Configuration reloaded and cache cleared.")
                        .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int checkCurrentChunk(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player when not specifying coordinates."));
            return 0;
        }
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        return performCheck(source, source.getLevel(), chunkPos);
    }

    private static int checkSpecificChunk(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var columnPos = ColumnPosArgument.getColumnPos(context, "pos");
        ChunkPos chunkPos = new ChunkPos(columnPos.x() >> 4, columnPos.z() >> 4);
        return performCheck(source, source.getLevel(), chunkPos);
    }

    private static int performCheck(CommandSourceStack source, ServerLevel level, ChunkPos chunkPos) {
        ServerPlayer player = source.getPlayer();
        ProtectionResult result = StructureProtectionManager.getInstance().checkProtection(level, chunkPos, player);

        MutableComponent message = Component.literal("Protection Check for Chunk [")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(chunkPos.x + ", " + chunkPos.z).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]:\n").withStyle(ChatFormatting.GOLD));

        ChatFormatting statusColor = result.isAllowed() ? ChatFormatting.GREEN : ChatFormatting.RED;
        message.append(Component.literal("  Status: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(result.type().name()).withStyle(statusColor))
                .append("\n");

        if (result.structureInfo() != null) {
            StructureInfo info = result.structureInfo();
            message.append(Component.literal("  Structure: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(info.getFullId()).withStyle(ChatFormatting.YELLOW))
                    .append("\n");
            message.append(Component.literal("  Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(info.getTypeString()).withStyle(ChatFormatting.AQUA))
                    .append("\n");
            message.append(Component.literal("  Terrain Adaptation: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(info.terrainAdaptation().name()).withStyle(ChatFormatting.AQUA))
                    .append("\n");
        }

        if (result.paddingSourceChunk() != null) {
            ChunkPos paddingSource = result.paddingSourceChunk();
            message.append(Component.literal("  Padding Source: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("[" + paddingSource.x + ", " + paddingSource.z + "]").withStyle(ChatFormatting.YELLOW))
                    .append("\n");
        }

        if (result.isProtected()) {
            message.append(Component.literal("  Message: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(result.getDenialMessage(chunkPos)).withStyle(ChatFormatting.RED));
        }

        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int debugCurrentChunk(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player when not specifying coordinates."));
            return 0;
        }
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        return performDebug(source, source.getLevel(), chunkPos);
    }

    private static int debugSpecificChunk(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var columnPos = ColumnPosArgument.getColumnPos(context, "pos");
        ChunkPos chunkPos = new ChunkPos(columnPos.x() >> 4, columnPos.z() >> 4);
        return performDebug(source, source.getLevel(), chunkPos);
    }

    private static int performDebug(CommandSourceStack source, ServerLevel level, ChunkPos chunkPos) {
        List<StructureInfo> structures = StructureProtectionManager.getInstance().getStructuresInChunk(level, chunkPos);

        MutableComponent message = Component.literal("Structures in Chunk [")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(chunkPos.x + ", " + chunkPos.z).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]:\n").withStyle(ChatFormatting.GOLD));

        if (structures.isEmpty()) {
            message.append(Component.literal("  No structures found.").withStyle(ChatFormatting.GRAY));
        } else {
            for (StructureInfo info : structures) {
                message.append(Component.literal("  - ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(info.getFullId()).withStyle(ChatFormatting.YELLOW))
                        .append("\n");
                message.append(Component.literal("      Type: ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(info.getTypeString()).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" | Adaptation: ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(info.terrainAdaptation().name()).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" | Step: ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(info.generationStep()).withStyle(ChatFormatting.AQUA))
                        .append("\n");
            }
        }

        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int clearCache(CommandContext<CommandSourceStack> context) {
        StructureCache cache = StructureProtectionManager.getInstance().getCache();
        int size = cache.size();
        cache.clear();
        context.getSource().sendSuccess(() ->
                Component.literal("Cache cleared. Removed " + size + " entries.")
                        .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int cacheStats(CommandContext<CommandSourceStack> context) {
        StructureCache cache = StructureProtectionManager.getInstance().getCache();
        MutableComponent message = Component.literal("Cache Statistics:\n").withStyle(ChatFormatting.GOLD);
        message.append(Component.literal("  " + cache.getStats()).withStyle(ChatFormatting.WHITE));
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }
}
