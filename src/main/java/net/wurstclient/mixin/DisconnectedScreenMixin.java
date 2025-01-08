/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoReconnectHack;
import net.wurstclient.nochatreports.ForcedChatReportsScreen;
import net.wurstclient.nochatreports.NcrModRequiredScreen;
import net.wurstclient.util.LastServerRememberer;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends Screen
{
	private int autoReconnectTimer;
	private ButtonWidget autoReconnectButton;
	
	@Shadow
	@Final
	private DisconnectionInfo info;
	@Shadow
	@Final
	private Screen parent;
	@Shadow
	@Final
	private DirectionalLayoutWidget grid;
	
	private DisconnectedScreenMixin(WurstClient wurst, Text title)
	{
		super(title);
	}
	
	@Inject(at = @At("TAIL"), method = "init()V")
	private void onInit(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		Text reason = info.reason();
		System.out.println("Disconnected: " + reason);
		
		if(ForcedChatReportsScreen.isCausedByNoChatReports(reason))
		{
			client.setScreen(new ForcedChatReportsScreen(parent));
			return;
		}
		
		if(NcrModRequiredScreen.isCausedByLackOfNCR(reason))
		{
			client.setScreen(new NcrModRequiredScreen(parent));
			return;
		}
		
		addReconnectButtons();
	}
	
	private void addReconnectButtons()
	{
		ButtonWidget reconnectButton = grid.add(ButtonWidget
			.builder(Text.literal("Reconnect"),
				b -> LastServerRememberer.reconnect(parent))
			.width(200).build());
		
		autoReconnectButton = grid.add(ButtonWidget
			.builder(Text.literal("AutoReconnect"), b -> pressAutoReconnect())
			.width(200).build());
		
		grid.refreshPositions();
		Stream.of(reconnectButton, autoReconnectButton)
			.forEach(this::addDrawableChild);
		
		AutoReconnectHack autoReconnect =
			WurstClient.INSTANCE.getHax().autoReconnectHack;
		
		if(autoReconnect.isEnabled())
			autoReconnectTimer = autoReconnect.getWaitTicks();
	}
	
	private void pressAutoReconnect()
	{
		AutoReconnectHack autoReconnect =
			WurstClient.INSTANCE.getHax().autoReconnectHack;
		
		autoReconnect.setEnabled(!autoReconnect.isEnabled());
		
		if(autoReconnect.isEnabled())
			autoReconnectTimer = autoReconnect.getWaitTicks();
	}
	
	@Override
	public void tick()
	{
		if(!WurstClient.INSTANCE.isEnabled() || autoReconnectButton == null)
			return;
		
		AutoReconnectHack autoReconnect =
			WurstClient.INSTANCE.getHax().autoReconnectHack;
		
		if(!autoReconnect.isEnabled())
		{
			autoReconnectButton.setMessage(Text.literal("AutoReconnect"));
			return;
		}
		
		autoReconnectButton.setMessage(Text.literal("AutoReconnect ("
			+ (int)Math.ceil(autoReconnectTimer / 20.0) + ")"));
		
		if(autoReconnectTimer > 0)
		{
			autoReconnectTimer--;
			return;
		}
		
		LastServerRememberer.reconnect(parent);
	}
}
