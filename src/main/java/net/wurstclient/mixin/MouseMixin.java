/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.Mouse;
import net.wurstclient.mixinterface.IMouse;

@Mixin(Mouse.class)
public class MouseMixin implements IMouse
{
	@Shadow
	private double eventDeltaWheel;
	
	@Override
	public double getWheelDelta()
	{
		return eventDeltaWheel;
	}
}
