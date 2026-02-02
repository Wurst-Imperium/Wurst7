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

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
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
	
	@Inject(at = @At("HEAD"), method = "init()V")
	private void beforeVanillaButtons(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		JoinMultiplayerScreen mpScreen = (JoinMultiplayerScreen)(Object)this;
		
		// Add Last Server button early for better tab navigation
		lastServerButton = Button
			.builder(Component.nullToEmpty("Last Server"),
				b -> LastServerRememberer.joinLastServer(mpScreen))
			.width(100).build();
		addRenderableWidget(lastServerButton);
	}
	
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/multiplayer/JoinMultiplayerScreen;repositionElements()V",
		ordinal = 0), method = "init()V")
	private void afterVanillaButtons(CallbackInfo ci,
		@Local(ordinal = 1) LinearLayout footerTopRow,
		@Local(ordinal = 2) LinearLayout footerBottomRow)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		JoinMultiplayerScreen mpScreen = (JoinMultiplayerScreen)(Object)this;
		
		Button serverFinderButton = Button
			.builder(Component.nullToEmpty("Server Finder"),
				b -> minecraft.setScreen(new ServerFinderScreen(mpScreen)))
			.width(100).build();
		addRenderableWidget(serverFinderButton);
		footerTopRow.addChild(serverFinderButton);
		
		Button cleanUpButton = Button
			.builder(Component.nullToEmpty("Clean Up"),
				b -> minecraft.setScreen(new CleanUpScreen(mpScreen)))
			.width(100).build();
		addRenderableWidget(cleanUpButton);
		footerBottomRow.addChild(cleanUpButton);
	}
	
	@Inject(at = @At("TAIL"), method = "repositionElements()V")
	private void onRefreshWidgetPositions(CallbackInfo ci)
	{
		updateLastServerButton();
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
		lastServerButton.setX(width / 2 - 154);
		lastServerButton.setY(6);
	}
}
