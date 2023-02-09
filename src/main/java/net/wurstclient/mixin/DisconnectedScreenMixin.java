/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
	private Text reason;
	@Shadow
	@Final
	private Screen parent;
	@Shadow
	private int reasonHeight;
	
	private DisconnectedScreenMixin(WurstClient wurst, Text title)
	{
		super(title);
	}
	
	@Inject(at = @At("TAIL"), method = {"init()V"})
	private void onInit(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
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
		int backButtonX = width / 2 - 100;
		int backButtonY =
			Math.min(height / 2 + reasonHeight / 2 + 9, height - 30);
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Reconnect"),
				b -> LastServerRememberer.reconnect(parent))
			.dimensions(backButtonX, backButtonY + 24, 200, 20).build());
		
		autoReconnectButton = addDrawableChild(ButtonWidget
			.builder(Text.literal("AutoReconnect"), b -> pressAutoReconnect())
			.dimensions(backButtonX, backButtonY + 48, 200, 20).build());
		
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
