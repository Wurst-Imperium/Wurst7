/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.wurstclient.WurstClient;

@Mixin(ControlsListWidget.class)
public abstract class ControlsListWidgetMixin
	extends ElementListWidget<ControlsListWidget.Entry>
{
	public ControlsListWidgetMixin(WurstClient wurst, MinecraftClient client,
		int width, int height, int y, int itemHeight)
	{
		super(client, width, height, y, itemHeight);
	}
	
	/**
	 * Prevents Wurst's zoom keybind from being added to the controls list.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screen/option/ControlsListWidget;addEntry(Lnet/minecraft/client/gui/widget/EntryListWidget$Entry;)I",
		ordinal = 1),
		method = "<init>(Lnet/minecraft/client/gui/screen/option/KeybindsScreen;Lnet/minecraft/client/MinecraftClient;)V")
	private int dontAddZoomEntry(ControlsListWidget instance,
		EntryListWidget.Entry<?> entry, Operation<Integer> original)
	{
		if(!(entry instanceof ControlsListWidget.KeyBindingEntry kbEntry))
			return original.call(instance, entry);
		
		Text name = kbEntry.bindingName;
		if(name == null || !(name
			.getContent() instanceof TranslatableTextContent trContent))
			return original.call(instance, entry);
		
		if(!"key.wurst.zoom".equals(trContent.getKey()))
			return original.call(instance, entry);
		
		return 0;
	}
}
