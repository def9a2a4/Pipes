package anon.def9a2a4.pipes.adapter;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Abstraction for interacting with container blocks.
 * Each implementation handles slot routing and extraction rules
 * for a specific container type (furnace, brewing stand, etc.).
 */
public interface ContainerAdapter {

    /**
     * Check if this adapter handles the given block.
     */
    boolean canReceive(Block block);

    /**
     * Insert items into the container. Returns leftover items that didn't fit, or null if fully inserted.
     */
    ItemStack insert(Block block, ItemStack item);

    /**
     * Preview extraction without modifying the inventory. Returns a clone of what would be extracted.
     */
    ItemStack peekExtract(Block block, int maxAmount);

    /**
     * Actually remove items from the inventory after a confirmed delivery.
     * The extracted ItemStack should match what peekExtract returned (same type and amount).
     */
    void commitExtract(Block block, ItemStack extracted);

    /**
     * Check if the container has any extractable items.
     */
    boolean hasItems(Block block);
}
