/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.Objects;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.wurstclient.mixinterface.ISimpleOption;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> implements ISimpleOption<T>
{
	@Shadow
	T value;
	
	@Shadow
	@Final
	private Consumer<T> changeCallback;
	
	@Override
	public void forceSetValue(T newValue)
	{
		if(!MinecraftClient.getInstance().isRunning())
		{
			value = newValue;
			return;
		}
		
		if(!Objects.equals(value, newValue))
		{
			value = newValue;
			changeCallback.accept(value);
		}
	}
}
