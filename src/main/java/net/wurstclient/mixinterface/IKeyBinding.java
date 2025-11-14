/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import net.minecraft.client.KeyMapping;

public interface IKeyBinding
{
	/**
	 * Resets the pressed state to whether or not the user is actually pressing
	 * this key on their keyboard.
	 */
	public default void resetPressedState()
	{
		wurst_resetPressedState();
	}
	
	/**
	 * Simulates the user pressing this key on their keyboard or mouse. This is
	 * much more aggressive than using {@link #setDown(boolean)} and should
	 * be used sparingly.
	 */
	public default void simulatePress(boolean pressed)
	{
		wurst_simulatePress(pressed);
	}
	
	public default void setDown(boolean down)
	{
		asVanilla().setDown(down);
	}
	
	public default KeyMapping asVanilla()
	{
		return (KeyMapping)this;
	}
	
	/**
	 * Returns the given KeyBinding object as an IKeyBinding, allowing you to
	 * access the resetPressedState() method.
	 */
	public static IKeyBinding get(KeyMapping kb)
	{
		return (IKeyBinding)kb;
	}
	
	/**
	 * @deprecated Use {@link #resetPressedState()} instead.
	 */
	@Deprecated
	public void wurst_resetPressedState();
	
	/**
	 * @deprecated Use {@link #simulatePress()} instead.
	 */
	@Deprecated
	public void wurst_simulatePress(boolean pressed);
}
