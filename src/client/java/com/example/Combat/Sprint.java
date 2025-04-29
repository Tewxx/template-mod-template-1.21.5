package com.example.Combat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sprint implements ClientModInitializer {
    public static final String MOD_ID = "sprint";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private KeyBinding toggleSprint;
    private boolean sprintEnabled = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Sprint Mod Initialized!");

        // Register the key binding (R key for toggle)
        toggleSprint = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Auto Sprint", // Translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N, // R key
                "Combat" // Category translation key
        ));

        // Register client tick event
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        // Check if the key was pressed to toggle Sprint
        if (toggleSprint.wasPressed()) {
            sprintEnabled = !sprintEnabled;
            client.player.sendMessage(Text.literal("Auto Sprint " +
                    (sprintEnabled ? "§aEnabled" : "§cDisabled")), true);
            LOGGER.info("Auto Sprint toggled: " + sprintEnabled);
        }

        // Apply sprint if enabled
        if (sprintEnabled && client.currentScreen == null) {
            // Check conditions where sprint should work
            if (client.player.isOnGround() &&
                    client.player.forwardSpeed > 0 &&
                    !client.player.isSneaking() &&
                    !client.player.isUsingItem() &&
                    client.player.getHungerManager().getFoodLevel() > 6) {

                client.player.setSprinting(true);
            }
        }
    }
}