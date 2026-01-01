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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.wurstclient.WurstClient;

@Mixin(KeyBindsList.class)
public abstract class ControlsListWidgetMixin
	extends ContainerObjectSelectionList<KeyBindsList.Entry>
{
	public ControlsListWidgetMixin(WurstClient wurst, Minecraft client,
		int width, int height, int y, int itemHeight)
	{
		super(client, width, height, y, itemHeight);
	}
	
	/**
	 * Prevents Wurst's zoom keybind from being added to the controls list.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList;addEntry(Lnet/minecraft/client/gui/components/AbstractSelectionList$Entry;)I",
		ordinal = 1),
		method = "<init>(Lnet/minecraft/client/gui/screens/options/controls/KeyBindsScreen;Lnet/minecraft/client/Minecraft;)V")
	private int dontAddZoomEntry(KeyBindsList instance,
		AbstractSelectionList.Entry<?> entry, Operation<Integer> original)
	{
		if(!(entry instanceof KeyBindsList.KeyEntry kbEntry))
			return original.call(instance, entry);
		
		Component name = kbEntry.name;
		if(name == null
			|| !(name.getContents() instanceof TranslatableContents trContent))
			return original.call(instance, entry);
		
		if(!"key.wurst.zoom".equals(trContent.getKey()))
			return original.call(instance, entry);
		
		return 0;
	}
}
