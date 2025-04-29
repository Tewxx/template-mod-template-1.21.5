package com.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleArrayList implements ClientModInitializer {
    public static final String MOD_ID = "modarraylist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private KeyBinding toggleArrayList;
    private boolean arrayListEnabled = true;

    // Map to store module names and their enabled status
    private final Map<String, Boolean> moduleStates = new HashMap<>();

    // Colors for module display (can be customized)
    private static final int BACKGROUND_COLOR = 0x80000000; // Semi-transparent black
    private static final int TEXT_COLOR = 0xFFFFFFFF; // White
    private static final int ENABLED_COLOR = 0xFF00FF00; // Green

    @Override
    public void onInitializeClient() {
        LOGGER.info("ModArrayList Initialized!");

        // Register the key binding (J key for toggle)
        toggleArrayList = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "ArrayList", // Translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J, // J key
                "Combat" // Category translation key
        ));

        // Register client tick event for key handling
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Register HUD render callback for drawing the array list
        HudRenderCallback.EVENT.register((context, unused) -> {
            onHudRender(context);
        });
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        // Check if the key was pressed to toggle ArrayList
        if (toggleArrayList.wasPressed()) {
            arrayListEnabled = !arrayListEnabled;
            client.player.sendMessage(Text.literal("ArrayList " +
                    (arrayListEnabled ? "§aEnabled" : "§cDisabled")), true);
            LOGGER.info("ArrayList toggled: " + arrayListEnabled);
        }

        // Update module states (add your actual module state checks here)
        updateModuleStates();
    }

    private void updateModuleStates() {
        // This is where you need to check the state of each module you want to display
        // For demonstration, I'll create some example modules

        // Example from your existing mods:
        // Check if TriggerBot is enabled (you would need to access this from the TriggerBot class)
        // moduleStates.put("TriggerBot", TriggerBot.isEnabled());

        // For testing purposes, let's just add some dummy modules
        moduleStates.put("TriggerBot", true);
        moduleStates.put("S-Tap", true);
        moduleStates.put("AutoSprint", false);
        moduleStates.put("KillAura", true);
        moduleStates.put("ESP", true);
        moduleStates.put("NoFall", false);
        moduleStates.put("Speed", true);

        // In a real implementation, you would connect to your actual modules:
        // For example, if TriggerBot has a static method or field to check its state:
        // moduleStates.put("TriggerBot", TriggerBot.isEnabled());
        // moduleStates.put("S-Tap", STap.isEnabled());
        // etc.
    }

    private void onHudRender(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Fixed: Use the debug overlay check instead of debugEnabled property
        if (!arrayListEnabled || client.player == null || client.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();

        // Get enabled modules and sort by name length (longest first)
        List<String> enabledModules = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : moduleStates.entrySet()) {
            if (entry.getValue()) {
                enabledModules.add(entry.getKey());
            }
        }

        // Sort by length - longest module names first
        enabledModules.sort(Comparator.comparingInt(String::length).reversed());

        // Draw module list in top right corner
        int y = 2; // Starting Y position
        for (String moduleName : enabledModules) {
            int moduleWidth = textRenderer.getWidth(moduleName);
            int x = screenWidth - moduleWidth - 4; // Position from right edge

            // Draw background
            context.fill(x - 2, y - 2, screenWidth - 2, y + textRenderer.fontHeight + 2, BACKGROUND_COLOR);

            // Draw module name
            context.drawText(textRenderer, moduleName, x, y, ENABLED_COLOR, true);

            // Move down for next module
            y += textRenderer.fontHeight + 4;
        }
    }

    // Method to allow other modules to register themselves with the ArrayList
    public void registerModule(String moduleName, boolean enabled) {
        moduleStates.put(moduleName, enabled);
    }

    // Static instance for access from other modules (optional)
    // You would need to create this instance in onInitializeClient()
    // private static ModArrayList INSTANCE;
    // public static ModArrayList getInstance() { return INSTANCE; }
}