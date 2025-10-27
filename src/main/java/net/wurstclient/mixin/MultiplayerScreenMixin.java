/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMultiplayerScreen;
import net.wurstclient.serverfinder.CleanUpScreen;
import net.wurstclient.serverfinder.ServerFinderScreen;
import net.wurstclient.util.LastServerRememberer;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen implements IMultiplayerScreen
{
	@Shadow
	protected MultiplayerServerListWidget serverListWidget;
	
	private ButtonWidget lastServerButton;
	
	private MultiplayerScreenMixin(WurstClient wurst, Text title)
	{
		super(title);
	}
	
	@Inject(at = @At("HEAD"), method = "init()V")
	private void beforeVanillaButtons(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		MultiplayerScreen mpScreen = (MultiplayerScreen)(Object)this;
		
		// Add Last Server button early for better tab navigation
		lastServerButton = ButtonWidget
			.builder(Text.of("Last Server"),
				b -> LastServerRememberer.joinLastServer(mpScreen))
			.width(100).build();
		addDrawableChild(lastServerButton);
	}
	
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;refreshWidgetPositions()V",
		ordinal = 0), method = "init()V")
	private void afterVanillaButtons(CallbackInfo ci,
		@Local(ordinal = 1) DirectionalLayoutWidget footerTopRow,
		@Local(ordinal = 2) DirectionalLayoutWidget footerBottomRow)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		MultiplayerScreen mpScreen = (MultiplayerScreen)(Object)this;
		
		ButtonWidget serverFinderButton = ButtonWidget
			.builder(Text.of("Server Finder"),
				b -> client.setScreen(new ServerFinderScreen(mpScreen)))
			.width(100).build();
		addDrawableChild(serverFinderButton);
		footerTopRow.add(serverFinderButton);
		
		ButtonWidget cleanUpButton = ButtonWidget
			.builder(Text.of("Clean Up"),
				b -> client.setScreen(new CleanUpScreen(mpScreen)))
			.width(100).build();
		addDrawableChild(cleanUpButton);
		footerBottomRow.add(cleanUpButton);
	}
	
	@Inject(at = @At("TAIL"), method = "refreshWidgetPositions()V")
	private void onRefreshWidgetPositions(CallbackInfo ci)
	{
		updateLastServerButton();
	}
	
	@Inject(at = @At("HEAD"),
		method = "connect(Lnet/minecraft/client/network/ServerInfo;)V")
	private void onConnect(ServerInfo entry, CallbackInfo ci)
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
	
	@Override
	public MultiplayerServerListWidget getServerListSelector()
	{
		return serverListWidget;
	}
}
