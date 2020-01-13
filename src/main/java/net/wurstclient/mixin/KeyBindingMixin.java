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

import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IKeyBinding;

@Mixin(KeyBinding.class)
public class KeyBindingMixin implements IKeyBinding
{
	@Shadow
	private boolean pressed;
	@Shadow
	private InputUtil.KeyCode keyCode;
	
	@Override
	public void setPressed(boolean pressed)
	{
		this.pressed = pressed;
	}
	
	@Override
	public boolean isActallyPressed()
	{
		long handle = WurstClient.MC.getWindow().getHandle();
		int code = keyCode.getKeyCode();
		return InputUtil.isKeyPressed(handle, code);
	}
}
