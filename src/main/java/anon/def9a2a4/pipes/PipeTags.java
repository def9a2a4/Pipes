package anon.def9a2a4.pipes;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/**
 * Utility class for PersistentDataContainer used to identify pipe displayx entities.
 *
 * Tag format: pipe:{variant_id}:{x}_{y}_{z}_{facing}
 */
public final class PipeTags {
    public static final NamespacedKey PIPE_TAG_KEY = new NamespacedKey("pipe", "tag");

    public static final String DIRECTIONAL_SUFFIX = "_dir";
    public static final String HEAD_DISPLAY_SUFFIX = "_head";

    private PipeTags() {}

    /**
     * Create a tag for a pipe at the given location with the given facing direction and variant.
     */
    public static String createTag(Location location, BlockFace facing, PipeVariant variant) {
        return variant.getId() + ":" +
                location.getBlockX() + "_" +
                location.getBlockY() + "_" +
                location.getBlockZ() + "_" +
                facing.name();
    }

    /**
     * Create a tag for a directional display entity (corner pipes only).
     */
    public static String createDirectionalTag(Location location, BlockFace facing, PipeVariant variant) {
        return createTag(location, facing, variant) + DIRECTIONAL_SUFFIX;
    }

    /**
     * Create a tag for an extra head display entity used by down-facing corner pipes.
     */
    public static String createHeadDisplayTag(Location location, BlockFace facing, PipeVariant variant) {
        return createTag(location, facing, variant) + HEAD_DISPLAY_SUFFIX;
    }

    /**
     * Check if a tag is for a directional display entity.
     */
    public static boolean isDirectionalTag(String tag) {
        return tag != null && tag.endsWith(DIRECTIONAL_SUFFIX);
    }

    /**
     * Check if a tag is for a simulated head display entity.
     */
    public static boolean isHeadDisplayTag(String tag) {
        return tag != null && tag.endsWith(HEAD_DISPLAY_SUFFIX);
    }

    /**
     * Check if an entity has any pipe tag.
     */
    public static boolean isPipeEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(PIPE_TAG_KEY, PersistentDataType.STRING);
    }

    /**
     * Extract the pipe tag from an entity's tags.
     */
    public static String getPipeTag(Entity entity) {
        return entity.getPersistentDataContainer().get(PIPE_TAG_KEY, PersistentDataType.STRING);
    }

    /**
     * Add a pipe tag to an entity, replacing any existing pipe tag.
     */
    public static void addPipeTag(Entity entity, String newTag) {
        entity.getPersistentDataContainer().set(PIPE_TAG_KEY, PersistentDataType.STRING, newTag);
    }

    /**
     * Parse the variant ID from a tag.
     * Returns null if the tag doesn't match the expected format.
     */
    public static String parseVariantId(String tag) {
        if (tag == null) return null;

        String workingTag = stripDisplaySuffix(tag);

        // Format: {variant_id}:{data}
        int colonIdx = workingTag.indexOf(':');
        if (colonIdx > 0) {
            return workingTag.substring(0, colonIdx);
        }

        return null;
    }

    /**
     * Get the data portion of a tag (coordinates and facing).
     */
    private static String getTagData(String tag) {
        if (tag == null) return null;

        String workingTag = stripDisplaySuffix(tag);

        // Format: {variant_id}:{data}
        int colonIdx = workingTag.indexOf(':');
        if (colonIdx > 0 && colonIdx < workingTag.length() - 1) {
            return workingTag.substring(colonIdx + 1);
        }

        return null;
    }

    /**
     * Parse location from a pipe tag.
     * Returns null if parsing fails.
     */
    public static Location parseLocation(String tag, World world) {
        String data = getTagData(tag);
        if (data == null) return null;

        String[] parts = data.split("_");
        if (parts.length != 4) return null;

        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse facing direction from a pipe tag.
     * Returns null if parsing fails.
     */
    public static BlockFace parseFacing(String tag) {
        String data = getTagData(tag);
        if (data == null) return null;

        String[] parts = data.split("_");
        if (parts.length != 4) return null;

        try {
            return BlockFace.valueOf(parts[3]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Check if a pipe tag matches the given location.
     */
    public static boolean matchesLocation(String tag, Location location) {
        Location parsed = parseLocation(tag, location.getWorld());
        if (parsed == null) return false;

        return parsed.getBlockX() == location.getBlockX() &&
                parsed.getBlockY() == location.getBlockY() &&
                parsed.getBlockZ() == location.getBlockZ();
    }

    private static String stripDisplaySuffix(String tag) {
        if (tag == null) return null;
        if (tag.endsWith(DIRECTIONAL_SUFFIX)) {
            return tag.substring(0, tag.length() - DIRECTIONAL_SUFFIX.length());
        }
        if (tag.endsWith(HEAD_DISPLAY_SUFFIX)) {
            return tag.substring(0, tag.length() - HEAD_DISPLAY_SUFFIX.length());
        }
        return tag;
    }
}
