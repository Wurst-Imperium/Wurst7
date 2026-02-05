/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.FreecamHack;
import net.wurstclient.mixinterface.IKeyBinding;

@Mixin(KeyMapping.class)
public abstract class KeyBindingMixin implements IKeyBinding
{
	@Shadow
	private InputConstants.Key key;
	
	@Unique
	private static final ThreadLocal<Boolean> WURST_SIMULATING_INPUT =
		new ThreadLocal<>();
	
	@Inject(method = "set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0)
	private static void onSetKey(InputConstants.Key key, boolean pressed,
		CallbackInfo ci)
	{
		if(Boolean.TRUE.equals(WURST_SIMULATING_INPUT.get()))
			return;
		
		Minecraft mc = WurstClient.MC;
		if(mc == null || mc.options == null)
			return;
		
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam == null || !freecam.isAiCompatibilityMode())
			return;
		
		if(shouldBlockKeyInAiCompatibilityFreecam(key))
			ci.cancel();
	}
	
	@Inject(method = "click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0)
	private static void onClickKey(InputConstants.Key key, CallbackInfo ci)
	{
		if(Boolean.TRUE.equals(WURST_SIMULATING_INPUT.get()))
			return;
		
		Minecraft mc = WurstClient.MC;
		if(mc == null || mc.options == null)
			return;
		
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam == null || !freecam.isAiCompatibilityMode())
			return;
		
		if(shouldBlockKeyInAiCompatibilityFreecam(key))
			ci.cancel();
	}
	
	@Unique
	private static boolean shouldBlockKeyInAiCompatibilityFreecam(
		InputConstants.Key key)
	{
		Minecraft mc = WurstClient.MC;
		KeyMapping[] keys = {mc.options.keyUp, mc.options.keyDown,
			mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump,
			mc.options.keyShift, mc.options.keyAttack, mc.options.keyUse,
			mc.options.keyPickItem};
		
		for(KeyMapping km : keys)
			if(getKey(km).equals(key))
				return true;
		for(KeyMapping km : mc.options.keyHotbarSlots)
			if(getKey(km).equals(key))
				return true;
		return false;
	}
	
	@Unique
	private static InputConstants.Key getKey(KeyMapping keyMapping)
	{
		return ((KeyBindingMixin)(Object)keyMapping).key;
	}
	
	@Override
	@Unique
	@Deprecated // use IKeyBinding.isActuallyDown() instead
	public boolean wurst_isActuallyDown()
	{
		Window window = WurstClient.MC.getWindow();
		int code = key.getValue();
		
		if(key.getType() == InputConstants.Type.MOUSE)
			return GLFW.glfwGetMouseButton(window.handle(), code) == 1;
		
		return InputConstants.isKeyDown(window, code);
	}
	
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
		
		WURST_SIMULATING_INPUT.set(true);
		try
		{
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
		}finally
		{
			WURST_SIMULATING_INPUT.set(false);
		}
	}
	
	@Override
	@Shadow
	public abstract void setDown(boolean pressed);
}
