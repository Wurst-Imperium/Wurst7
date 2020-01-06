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
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMultiplayerScreen;
import net.wurstclient.serverfinder.CleanUpScreen;
import net.wurstclient.serverfinder.ServerFinderScreen;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen implements IMultiplayerScreen
{
	@Shadow
	protected MultiplayerServerListWidget serverListWidget;
	
	private MultiplayerScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}
	
	@Override
	public MultiplayerServerListWidget getServerListSelector()
	{
		return serverListWidget;
	}
	
	@Inject(at = {@At("TAIL")}, method = {"init()V"})
	private void onInit(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		addButton(new ButtonWidget(width / 2 + 154 + 4, height - 52, 100, 20,
			"Server Finder", b -> minecraft.openScreen(
				new ServerFinderScreen((MultiplayerScreen)(Object)this))));
		
		addButton(new ButtonWidget(width / 2 + 154 + 4, height - 28, 100, 20,
			"Clean Up", b -> minecraft.openScreen(
				new CleanUpScreen((MultiplayerScreen)(Object)this))));
	}
}
