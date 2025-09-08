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
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
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
		Window window = WurstClient.MC.getWindow();
		int code = boundKey.getCode();
		
		if(boundKey.getCategory() == InputUtil.Type.MOUSE)
			setPressed(GLFW.glfwGetMouseButton(window.getHandle(), code) == 1);
		else
			setPressed(InputUtil.isKeyPressed(window, code));
	}
	
	@Override
	@Unique
	@Deprecated // use IKeyBinding.simulatePress() instead
	public void wurst_simulatePress(boolean pressed)
	{
		MinecraftClient mc = WurstClient.MC;
		Window window = mc.getWindow();
		int action = pressed ? 1 : 0;
		
		switch(boundKey.getCategory())
		{
			case KEYSYM:
			mc.keyboard.onKey(window.getHandle(), action,
				new KeyInput(boundKey.getCode(), 0, 0));
			break;
			
			case SCANCODE:
			mc.keyboard.onKey(window.getHandle(), action,
				new KeyInput(GLFW.GLFW_KEY_UNKNOWN, boundKey.getCode(), 0));
			break;
			
			case MOUSE:
			mc.mouse.onMouseButton(window.getHandle(),
				new MouseInput(boundKey.getCode(), 0), action);
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
