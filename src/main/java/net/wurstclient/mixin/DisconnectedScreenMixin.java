/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoReconnectHack;
import net.wurstclient.util.LastServerRememberer;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends Screen
{
	private int autoReconnectTimer;
	
	private ButtonWidget autoReconnectButton;
	
	@Shadow
	@Final
	private Screen parent;
	
	@Shadow
	private int reasonHeight;
	
	private DisconnectedScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}
	
	@Inject(at = {@At("TAIL")}, method = {"init()V"})
	private void onInit(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		int backButtonX = width / 2 - 100;
		int backButtonY =
			Math.min(height / 2 + reasonHeight / 2 + 9, height - 30);
		
		addButton(new ButtonWidget(backButtonX, backButtonY + 24, 200, 20,
			new LiteralText("Reconnect"),
			b -> LastServerRememberer.reconnect(parent)));
		
		autoReconnectButton =
			addButton(new ButtonWidget(backButtonX, backButtonY + 48, 200, 20,
				new LiteralText("AutoReconnect"), b -> pressAutoReconnect()));
		
		if(WurstClient.INSTANCE.getHax().autoReconnectHack.isEnabled())
			autoReconnectTimer = 100;
	}
	
	private void pressAutoReconnect()
	{
		AutoReconnectHack autoReconnect =
			WurstClient.INSTANCE.getHax().autoReconnectHack;
		
		autoReconnect.setEnabled(!autoReconnect.isEnabled());
		
		if(autoReconnect.isEnabled())
			autoReconnectTimer = 100;
	}
	
	@Override
	public void tick()
	{
		AutoReconnectHack autoReconnect =
			WurstClient.INSTANCE.getHax().autoReconnectHack;
		
		if(!autoReconnect.isEnabled())
		{
			autoReconnectButton.setMessage(new LiteralText("AutoReconnect"));
			return;
		}
		
		autoReconnectButton.setMessage(new LiteralText("AutoReconnect ("
			+ (int)Math.ceil(autoReconnectTimer / 20.0) + ")"));
		
		if(autoReconnectTimer > 0)
		{
			autoReconnectTimer--;
			return;
		}
		
		LastServerRememberer.reconnect(parent);
	}
}
