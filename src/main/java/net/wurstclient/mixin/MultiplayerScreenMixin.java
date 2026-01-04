/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.wurstclient.WurstClient;
import net.wurstclient.serverfinder.CleanUpScreen;
import net.wurstclient.serverfinder.ServerFinderScreen;
import net.wurstclient.util.LastServerRememberer;

@Mixin(JoinMultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen
{
	private Button lastServerButton;
	
	private MultiplayerScreenMixin(WurstClient wurst, Component title)
	{
		super(title);
	}
	
	@Inject(at = @At("TAIL"), method = "init()V")
	private void onInit(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		lastServerButton =
			addRenderableWidget(
				Button
					.builder(Component.literal("Last Server"),
						b -> LastServerRememberer.joinLastServer(
							(JoinMultiplayerScreen)(Object)this))
					.bounds(width / 2 - 154, 10, 100, 20).build());
		updateLastServerButton();
		
		addRenderableWidget(Button
			.builder(Component.literal("Server Finder"),
				b -> minecraft.setScreen(new ServerFinderScreen(
					(JoinMultiplayerScreen)(Object)this)))
			.bounds(width / 2 + 154 + 4, height - 54, 100, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Clean Up"),
				b -> minecraft.setScreen(
					new CleanUpScreen((JoinMultiplayerScreen)(Object)this)))
			.bounds(width / 2 + 154 + 4, height - 30, 100, 20).build());
	}
	
	@Inject(at = @At("HEAD"),
		method = "join(Lnet/minecraft/client/multiplayer/ServerData;)V")
	private void onConnect(ServerData entry, CallbackInfo ci)
	{
		LastServerRememberer.setLastServer(entry);
		updateLastServerButton();
	}
	
	@Unique
	private void updateLastServerButton()
	{
		if(lastServerButton == null)
			return;
		
		lastServerButton.active = LastServerRememberer.getLastServer() != null;
	}
}
