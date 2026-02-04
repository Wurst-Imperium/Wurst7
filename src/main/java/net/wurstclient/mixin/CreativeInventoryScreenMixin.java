/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.wurstclient.WurstClient;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin
	extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu>
{
	private CreativeInventoryScreenMixin(WurstClient wurst,
		ItemPickerMenu screenHandler, Inventory inventory, Component title)
	{
		super(screenHandler, inventory, title);
	}
	
	@Inject(at = @At("HEAD"),
		method = "hasPermissions(Lnet/minecraft/world/entity/player/Player;)Z",
		cancellable = true)
	private void onShouldShowOperatorTab(Player player,
		CallbackInfoReturnable<Boolean> cir)
	{
		if(WurstClient.INSTANCE.isEnabled())
			cir.setReturnValue(true);
	}
}
