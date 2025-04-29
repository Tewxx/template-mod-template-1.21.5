package com.example.Combat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class STap implements ClientModInitializer {
    public static final String MOD_ID = "stap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private KeyBinding toggleSTap;
    private boolean sTapEnabled = false;
    private int sTapDelay = 0;
    private boolean isInSTapAction = false;
    private boolean waitingForAttack = true;
    private int lastAttackTime = 0;
    private boolean wasAttackPressed = false;

    // Constants
    private static final double RANGE_CHECK = 5.0;
    private static final int TAP_DURATION = 4;
    private static final int S_TAP_COOLDOWN = 10;

    @Override
    public void onInitializeClient() {
        LOGGER.info("S-Tap Mod Initialized!");

        // Register the key binding (O key for toggle)
        toggleSTap = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "S-Tap", // Translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O, // O key
                "Combat" // Category translation key
        ));

        // Register client tick event - make sure this runs AFTER TriggerBot's tick
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        // Check if the key was pressed to toggle S-Tap
        if (toggleSTap.wasPressed()) {
            sTapEnabled = !sTapEnabled;
            client.player.sendMessage(Text.literal("S-Tap " +
                    (sTapEnabled ? "§aEnabled" : "§cDisabled")), true);
            LOGGER.info("S-Tap toggled: " + sTapEnabled);
        }

        // Run S-Tap logic if enabled
        if (sTapEnabled && client.currentScreen == null) {
            processSTap(client);
        } else if (!sTapEnabled && isInSTapAction) {
            // Make sure to release S key if disabled
            client.options.backKey.setPressed(false);
            isInSTapAction = false;
        }
    }

    private void processSTap(MinecraftClient client) {
        if (client.player == null || client.options == null) return;
        if (client.player.hasVehicle()) return;

        // More reliable attack detection using attack button press/release
        boolean isAttackPressed = client.options.attackKey.isPressed();

        // Detect attack by checking press and release pattern
        if (isAttackPressed && !wasAttackPressed) {
            // Attack button was just pressed
            if (isPvPTargetInRange(client)) {
                lastAttackTime = client.player.age;
                waitingForAttack = false;
                sTapDelay = 0; // Reset delay to ensure immediate S press
                LOGGER.info("Attack detected on player - Starting S-tap sequence");

                // Immediately start S-tap after hitting (don't wait for next tick)
                client.options.backKey.setPressed(true);
                client.player.setSprinting(false);
                isInSTapAction = true;
                LOGGER.info("S-Tap activated - pressing S immediately");
            }
        }

        // Update attack button state for next tick
        wasAttackPressed = isAttackPressed;

        // Handle ongoing S-tap sequence
        if (isInSTapAction) {
            sTapDelay++;

            if (sTapDelay >= TAP_DURATION) {
                // Release S key after duration
                client.options.backKey.setPressed(false);
                isInSTapAction = false;
                waitingForAttack = true;
                LOGGER.info("S-Tap released - releasing S after " + sTapDelay + " ticks");
                sTapDelay = 0;
            }
        }
    }

    /**
     * Checks if a player target is in range and being aimed at
     */
    private boolean isPvPTargetInRange(MinecraftClient client) {
        if (client.crosshairTarget != null &&
                client.crosshairTarget.getType() == HitResult.Type.ENTITY) {

            EntityHitResult hitResult = (EntityHitResult) client.crosshairTarget;
            Entity target = hitResult.getEntity();

            // Only activate for player targets
            if (target instanceof PlayerEntity) {
                double distance = client.player.squaredDistanceTo(target);

                if (distance <= RANGE_CHECK * RANGE_CHECK) {
                    return true;
                }
            }
        }
        return false;
    }
}