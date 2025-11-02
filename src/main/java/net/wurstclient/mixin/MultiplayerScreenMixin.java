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

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
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
	
	@Unique
	private ButtonWidget lastServerButton;
	
	@Unique
	private ButtonWidget serverFinderButton;
	
	@Unique
	private ButtonWidget cleanUpButton;
	
	private MultiplayerScreenMixin(WurstClient wurst, Text title)
	{
		super(title);
	}
	
	@Inject(at = @At("TAIL"), method = "refreshWidgetPositions")
	private void onRefreshWidgetPositions(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		if(lastServerButton == null)
		{
			lastServerButton =
				addDrawableChild(
					ButtonWidget
						.builder(Text.literal("Last Server"),
							b -> LastServerRememberer.joinLastServer(
								(MultiplayerScreen)(Object)this))
						.size(100, 20).build());
		}
		lastServerButton.setPosition(width / 2 - 154, 10);
		updateLastServerButton();
		
		if(serverFinderButton == null)
		{
			serverFinderButton = addDrawableChild(ButtonWidget
				.builder(Text.literal("Server Finder"),
					b -> client.setScreen(new ServerFinderScreen(
						(MultiplayerScreen)(Object)this)))
				.size(100, 20).build());
		}
		serverFinderButton.setPosition(width / 2 + 154 + 4, height - 54);
		
		if(cleanUpButton == null)
		{
			cleanUpButton =
				addDrawableChild(
					ButtonWidget
						.builder(Text.literal("Clean Up"),
							b -> client.setScreen(new CleanUpScreen(
								(MultiplayerScreen)(Object)this)))
						.size(100, 20).build());
		}
		cleanUpButton.setPosition(width / 2 + 154 + 4, height - 30);
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
	}
	
	@Override
	public MultiplayerServerListWidget getServerListSelector()
	{
		return serverListWidget;
	}
}
