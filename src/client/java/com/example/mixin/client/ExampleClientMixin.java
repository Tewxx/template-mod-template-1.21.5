package com.example.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ExampleClientMixin {
	@Inject(at = @At("HEAD"), method = "run")
	private void init(CallbackInfo info) {
		// You can initialize things here if needed
	}

	@ModifyExpressionValue(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/client/option/GameOptions"))
	private GameOptions enableProgrammerArt(GameOptions options) {
		options.resourcePacks.add("programmer_art");
		return options;
	}
}