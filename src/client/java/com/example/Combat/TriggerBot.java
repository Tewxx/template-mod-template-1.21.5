package com.example.Combat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerBot implements ClientModInitializer {
    public static final String MOD_ID = "crittriggerbot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding keyBinding;
    private static KeyBinding toggleCritOnlyMode;
    private static boolean triggerBotEnabled = false;
    private static boolean critOnlyMode = false; // Default to attack always mode
    private static int attackTimer = 0; // Timer for ground attacks
    private static final float ATTACK_RANGE = 4.5F; // Maximum reach distance

    @Override
    public void onInitializeClient() {
        LOGGER.info("Critical TriggerBot Mod Initialized!");

        // Register the key binding (T key for toggle)
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "TriggerBot", // Translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_T, // T key
                "Combat" // Category translation key
        ));

        // Register another key binding (Y key for crit-only mode toggle)
        toggleCritOnlyMode = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "TriggerBot - Crit Only Mode", // Translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y, // Y key
                "Combat" // Category translation key
        ));

        // Register client tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            // Check if the key was pressed to toggle TriggerBot
            if (keyBinding.wasPressed()) {
                triggerBotEnabled = !triggerBotEnabled;
                client.player.sendMessage(Text.literal("CritTriggerBot " + (triggerBotEnabled ? "§aEnabled" : "§cDisabled")), true);
                LOGGER.info("CritTriggerBot toggled: " + triggerBotEnabled);
            }

            // Check if the key was pressed to toggle crit-only mode
            if (toggleCritOnlyMode.wasPressed()) {
                critOnlyMode = !critOnlyMode;
                client.player.sendMessage(Text.literal("Crit-Only Mode " + (critOnlyMode ? "§aEnabled" : "§cDisabled")), true);
                LOGGER.info("Crit-Only Mode toggled: " + critOnlyMode);
            }

            // Run TriggerBot logic if enabled and no GUI (screen) is open
            if (triggerBotEnabled && client.currentScreen == null) {
                // Check if attack cooldown is ready
                if (client.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
                    // Look for target entity in crosshair
                    if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                        EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
                        Entity target = entityHit.getEntity();

                        // Only attack living entities (monsters, animals, players)
                        if (target instanceof LivingEntity && target.isAlive()) {
                            // Check if target is in range
                            double distance = client.player.getPos().distanceTo(target.getPos());

                            if (distance <= ATTACK_RANGE) {
                                // Check for critical hit conditions
                                boolean canCrit = canPerformCriticalHit(client);
                                boolean playerIsBeingAttacked = client.player.hurtTime > 0;

                                // If we can crit OR player is being attacked, attack immediately
                                if (canCrit || playerIsBeingAttacked) {
                                    client.interactionManager.attackEntity(client.player, target);
                                    client.player.swingHand(Hand.MAIN_HAND);
                                    if (canCrit) {
                                        LOGGER.info("Performed CRITICAL hit on " + target.getName().getString());
                                    } else if (playerIsBeingAttacked) {
                                        LOGGER.info("Counter-attacking while being hit!");
                                    }
                                }
                                // If on ground and not being attacked, slight delay to prioritize crits
                                else if (client.player.isOnGround()) {
                                    // Slight delay before attacking when on ground
                                    if (attackTimer >= 2) {
                                        client.interactionManager.attackEntity(client.player, target);
                                        client.player.swingHand(Hand.MAIN_HAND);
                                        LOGGER.info("Performed normal hit on " + target.getName().getString());
                                        attackTimer = 0;
                                    } else {
                                        attackTimer++;
                                    }
                                }
                                // If in air but not in crit conditions, attack anyway after brief wait
                                else {
                                    // Short wait for possible crit, but don't wait too long
                                    if (attackTimer >= 3) {
                                        client.interactionManager.attackEntity(client.player, target);
                                        client.player.swingHand(Hand.MAIN_HAND);
                                        LOGGER.info("Attacking from air (non-crit)");
                                        attackTimer = 0;
                                    } else {
                                        attackTimer++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Checks if the player can perform a critical hit.
     * In Minecraft, critical hits occur when:
     * 1. The player is falling
     * 2. The player is not on the ground
     * 3. The player is not sprinting
     * 4. The player is not affected by blindness
     * 5. The player is not in water
     *
     * @param client The Minecraft client instance
     * @return true if a critical hit is possible, false otherwise
     */
    private boolean canPerformCriticalHit(MinecraftClient client) {
        return client.player.fallDistance > 0.0F &&
                !client.player.isOnGround() &&
                !client.player.isSprinting() &&
                !client.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS) &&
                !client.player.isSubmergedInWater() &&
                !client.player.hasVehicle();
    }
}