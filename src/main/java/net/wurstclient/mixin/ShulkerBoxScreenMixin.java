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

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoStealHack;

@Mixin(ShulkerBoxScreen.class)
public abstract class ShulkerBoxScreenMixin
	extends AbstractContainerScreen<ShulkerBoxMenu>
{
	@Unique
	private final AutoStealHack autoSteal =
		WurstClient.INSTANCE.getHax().autoStealHack;
	
	private ShulkerBoxScreenMixin(WurstClient wurst, ShulkerBoxMenu handler,
		Inventory inventory, Component title)
	{
		super(handler, inventory, title);
	}
	
	@Override
	public void init()
	{
		super.init();
		
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		if(autoSteal.areButtonsVisible())
		{
			addRenderableWidget(Button
				.builder(Component.literal("Steal"),
					b -> autoSteal.steal(this, 3))
				.bounds(leftPos + imageWidth - 108, topPos + 4, 50, 12)
				.build());
			
			addRenderableWidget(Button
				.builder(Component.literal("Store"),
					b -> autoSteal.store(this, 3))
				.bounds(leftPos + imageWidth - 56, topPos + 4, 50, 12).build());
		}
		
		if(autoSteal.isEnabled())
			autoSteal.steal(this, 3);
	}
}
