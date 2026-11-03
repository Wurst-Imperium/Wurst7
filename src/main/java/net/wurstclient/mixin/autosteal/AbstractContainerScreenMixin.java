/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.autosteal;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoStealHack;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen
{
	@Shadow
	@Final
	protected int imageWidth;
	@Shadow
	protected int leftPos;
	@Shadow
	protected int topPos;
	
	private AbstractContainerScreenMixin(WurstClient wurst, Component title)
	{
		super(title);
	}
	
	@Inject(method = "init()V", at = @At("TAIL"))
	private void onInit(CallbackInfo ci)
	{
		AbstractContainerScreen<?> screen =
			(AbstractContainerScreen<?>)(Object)this;
		int rows;
		if(screen instanceof ContainerScreen chest)
			rows = chest.getMenu().getRowCount();
		else if(screen instanceof ShulkerBoxScreen)
			rows = 3;
		else
			return;
		
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		AutoStealHack autoSteal = WurstClient.INSTANCE.getHax().autoStealHack;
		if(autoSteal.areButtonsVisible())
		{
			addRenderableWidget(Button
				.builder(Component.literal("Steal"),
					b -> autoSteal.steal(screen, rows))
				.bounds(leftPos + imageWidth - 108, topPos + 4, 50, 12)
				.build());
			
			addRenderableWidget(Button
				.builder(Component.literal("Store"),
					b -> autoSteal.store(screen, rows))
				.bounds(leftPos + imageWidth - 56, topPos + 4, 50, 12).build());
		}
		
		if(autoSteal.isEnabled())
			autoSteal.steal(screen, rows);
	}
}
