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

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IKeyBinding;

@Mixin(KeyMapping.class)
public abstract class KeyBindingMixin implements IKeyBinding
{
	@Shadow
	private InputConstants.Key key;
	
	@Override
	@Unique
	@Deprecated // use IKeyBinding.resetPressedState() instead
	public void wurst_resetPressedState()
	{
		Window window = WurstClient.MC.getWindow();
		int code = key.getValue();
		
		if(key.getType() == InputConstants.Type.MOUSE)
			setDown(GLFW.glfwGetMouseButton(window.handle(), code) == 1);
		else
			setDown(InputConstants.isKeyDown(window, code));
	}
	
	@Override
	@Unique
	@Deprecated // use IKeyBinding.simulatePress() instead
	public void wurst_simulatePress(boolean pressed)
	{
		Minecraft mc = WurstClient.MC;
		Window window = mc.getWindow();
		int action = pressed ? 1 : 0;
		
		switch(key.getType())
		{
			case KEYSYM:
			mc.keyboardHandler.keyPress(window.handle(), action,
				new KeyEvent(key.getValue(), 0, 0));
			break;
			
			case SCANCODE:
			mc.keyboardHandler.keyPress(window.handle(), action,
				new KeyEvent(GLFW.GLFW_KEY_UNKNOWN, key.getValue(), 0));
			break;
			
			case MOUSE:
			mc.mouseHandler.onButton(window.handle(),
				new MouseButtonInfo(key.getValue(), 0), action);
			break;
			
			default:
			System.out.println("Unknown keybinding type: " + key.getType());
			break;
		}
	}
	
	@Override
	@Shadow
	public abstract void setDown(boolean pressed);
}
