package com.leclowndu93150.dont_touch_our_structures.protection;

import com.leclowndu93150.dont_touch_our_structures.config.ModConfig;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;

public record ProtectionResult(
        Type type,
        @Nullable StructureInfo structureInfo,
        @Nullable ChunkPos paddingSourceChunk
) {
    public enum Type {
        ALLOWED(true, "Chunk is not protected"),
        PROTECTED_STRUCTURE(false, "Contains protected structure"),
        PROTECTED_PADDING(false, "Within protected structure padding"),
        DIMENSION_BLOCKED(false, "Dimension is not protected"),
        BIOME_BLOCKED(false, "Biome is not protected"),
        BYPASSED(true, "Player has bypass permission"),
        SINGLEPLAYER_ALLOWED(true, "Singleplayer mode - protection disabled");

        private final boolean allowed;
        private final String defaultMessage;

        Type(boolean allowed, String defaultMessage) {
            this.allowed = allowed;
            this.defaultMessage = defaultMessage;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    public static ProtectionResult allowed() {
        return new ProtectionResult(Type.ALLOWED, null, null);
    }

    public static ProtectionResult bypassed() {
        return new ProtectionResult(Type.BYPASSED, null, null);
    }

    public static ProtectionResult singleplayerAllowed() {
        return new ProtectionResult(Type.SINGLEPLAYER_ALLOWED, null, null);
    }

    public static ProtectionResult protectedStructure(StructureInfo info) {
        return new ProtectionResult(Type.PROTECTED_STRUCTURE, info, null);
    }

    public static ProtectionResult protectedPadding(StructureInfo info, ChunkPos paddingSourceChunk) {
        return new ProtectionResult(Type.PROTECTED_PADDING, info, paddingSourceChunk);
    }

    public static ProtectionResult dimensionBlocked() {
        return new ProtectionResult(Type.DIMENSION_BLOCKED, null, null);
    }

    public static ProtectionResult biomeBlocked() {
        return new ProtectionResult(Type.BIOME_BLOCKED, null, null);
    }

    public boolean isAllowed() {
        return type.isAllowed();
    }

    public boolean isProtected() {
        return !type.isAllowed();
    }

    public String getDenialMessage(ChunkPos targetChunk) {
        if (!ModConfig.customDenialMessage.isEmpty()) {
            String message = ModConfig.customDenialMessage;
            if (structureInfo != null) {
                message = message.replace("{structure_name}", structureInfo.getDisplayName());
                message = message.replace("{structure_type}", structureInfo.getTypeString());
            } else {
                message = message.replace("{structure_name}", "unknown");
                message = message.replace("{structure_type}", "unknown");
            }
            message = message.replace("{chunk_x}", String.valueOf(targetChunk.x));
            message = message.replace("{chunk_z}", String.valueOf(targetChunk.z));
            return message;
        }

        StringBuilder message = new StringBuilder("Cannot claim this chunk: ");

        switch (type) {
            case PROTECTED_STRUCTURE -> {
                if (structureInfo != null) {
                    message.append("Contains protected ");
                    if (ModConfig.includeStructureType) {
                        message.append(structureInfo.getTypeString()).append(" ");
                    }
                    message.append("structure");
                    if (ModConfig.includeStructureName) {
                        message.append(" (").append(structureInfo.getDisplayName()).append(")");
                    }
                } else {
                    message.append("Contains protected structure");
                }
            }
            case PROTECTED_PADDING -> {
                message.append("Within padding zone of protected structure");
                if (structureInfo != null && ModConfig.includeStructureName) {
                    message.append(" (").append(structureInfo.getDisplayName()).append(")");
                }
            }
            case DIMENSION_BLOCKED -> message.append("Protection not active in this dimension");
            case BIOME_BLOCKED -> message.append("Protection not active in this biome");
            default -> message.append(type.getDefaultMessage());
        }

        if (ModConfig.includeCoordinates) {
            message.append(" [Chunk: ").append(targetChunk.x).append(", ").append(targetChunk.z).append("]");
        }

        return message.toString();
    }
}
