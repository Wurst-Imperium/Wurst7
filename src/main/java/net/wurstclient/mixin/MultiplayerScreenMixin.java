/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.LiteralText;
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
	
	private MultiplayerScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}
	
	@Override
	public MultiplayerServerListWidget getServerListSelector()
	{
		return serverListWidget;
	}
	
	@Override
	public void connectToServer(ServerInfo server)
	{
		connect(server);
	}
	
	@Inject(at = {@At("TAIL")}, method = {"init()V"})
	private void onInit(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		lastServerButton = addButton(new ButtonWidget(width / 2 - 154, 10, 100,
			20, new LiteralText("Last Server"), b -> LastServerRememberer
				.joinLastServer((MultiplayerScreen)(Object)this)));
		
		addButton(new ButtonWidget(width / 2 + 154 + 4, height - 52, 100, 20,
			new LiteralText("Server Finder"), b -> client.openScreen(
				new ServerFinderScreen((MultiplayerScreen)(Object)this))));
		
		addButton(new ButtonWidget(width / 2 + 154 + 4, height - 28, 100, 20,
			new LiteralText("Clean Up"), b -> client.openScreen(
				new CleanUpScreen((MultiplayerScreen)(Object)this))));
	}
	
	@Inject(at = {@At("TAIL")}, method = {"tick()V"})
	public void onTick(CallbackInfo ci)
	{
		lastServerButton.active = LastServerRememberer.getLastServer() != null;
	}
	
	@Inject(at = {@At("HEAD")},
		method = {"connect(Lnet/minecraft/client/network/ServerInfo;)V"})
	private void onConnect(ServerInfo entry, CallbackInfo ci)
	{
		LastServerRememberer.setLastServer(entry);
	}
	
	@Shadow
	private void connect(ServerInfo entry)
	{
		
	}
}
