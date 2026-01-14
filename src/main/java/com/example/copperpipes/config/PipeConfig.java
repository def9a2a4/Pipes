package com.example.copperpipes.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Global configuration from config.yml.
 * Note: Per-variant transfer settings are now in the variants section
 * and are managed by PipeVariant and VariantRegistry.
 */
public class PipeConfig {
    private final boolean debugParticles;
    private final int particleInterval;

    // Recipe unlock settings
    private final String unlockAdvancement;
    private final boolean showUnlockMessage;
    private final String unlockMessage;

    public PipeConfig(FileConfiguration config) {
        this.debugParticles = config.getBoolean("global.debug.particles", false);
        this.particleInterval = config.getInt("global.debug.particle-interval", 10);

        // Recipe unlock settings
        this.unlockAdvancement = config.getString("recipes.unlock-advancement", "minecraft:story/smelt_iron");
        this.showUnlockMessage = config.getBoolean("recipes.show-unlock-message", true);
        this.unlockMessage = config.getString("recipes.unlock-message", "<gold>You've unlocked pipe crafting recipes!");
    }

    public boolean isDebugParticles() {
        return debugParticles;
    }

    public int getParticleInterval() {
        return particleInterval;
    }

    public String getUnlockAdvancement() {
        return unlockAdvancement;
    }

    public boolean isShowUnlockMessage() {
        return showUnlockMessage;
    }

    public String getUnlockMessage() {
        return unlockMessage;
    }

    public boolean isUnlockEnabled() {
        return unlockAdvancement != null && !unlockAdvancement.isEmpty()
               && !unlockAdvancement.equalsIgnoreCase("none");
    }
}
