/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IKeyBinding;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin implements IKeyBinding
{
	@Shadow
	private InputUtil.Key boundKey;
	
	@Override
	@Unique
	@Deprecated // use IKeyBinding.resetPressedState() instead
	public void wurst_resetPressedState()
	{
		long handle = WurstClient.MC.getWindow().getHandle();
		int code = boundKey.getCode();
		
		if(boundKey.getCategory() == InputUtil.Type.MOUSE)
			setPressed(GLFW.glfwGetMouseButton(handle, code) == 1);
		else
			setPressed(InputUtil.isKeyPressed(handle, code));
	}
	
	@Override
	@Unique
	@Deprecated // use IKeyBinding.simulatePress() instead
	public void wurst_simulatePress(boolean pressed)
	{
		MinecraftClient mc = WurstClient.MC;
		long window = mc.getWindow().getHandle();
		int action = pressed ? 1 : 0;
		
		switch(boundKey.getCategory())
		{
			case KEYSYM:
			mc.keyboard.onKey(window, boundKey.getCode(), 0, action, 0);
			break;
			
			case SCANCODE:
			mc.keyboard.onKey(window, GLFW.GLFW_KEY_UNKNOWN, boundKey.getCode(),
				action, 0);
			break;
			
			case MOUSE:
			mc.mouse.onMouseButton(window, boundKey.getCode(), action, 0);
			break;
			
			default:
			System.out
				.println("Unknown keybinding type: " + boundKey.getCategory());
			break;
		}
	}
	
	@Override
	@Shadow
	public abstract void setPressed(boolean pressed);
}
