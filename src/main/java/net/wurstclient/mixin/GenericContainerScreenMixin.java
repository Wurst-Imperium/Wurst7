/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoStealHack;

@Mixin(ContainerScreen.class)
public abstract class GenericContainerScreenMixin
	extends AbstractContainerScreen<ChestMenu>
{
	@Shadow
	@Final
	private int containerRows;
	
	@Unique
	private final AutoStealHack autoSteal =
		WurstClient.INSTANCE.getHax().autoStealHack;
	
	public GenericContainerScreenMixin(WurstClient wurst, ChestMenu container,
		Inventory playerInventory, Component name)
	{
		super(container, playerInventory, name);
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
					b -> autoSteal.steal(this, containerRows))
				.bounds(leftPos + imageWidth - 108, topPos + 4, 50, 12)
				.build());
			
			addRenderableWidget(Button
				.builder(Component.literal("Store"),
					b -> autoSteal.store(this, containerRows))
				.bounds(leftPos + imageWidth - 56, topPos + 4, 50, 12).build());
		}
		
		if(autoSteal.isEnabled())
			autoSteal.steal(this, containerRows);
	}
}
