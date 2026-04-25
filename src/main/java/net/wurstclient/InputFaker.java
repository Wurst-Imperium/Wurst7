/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.events.UpdateListener;

/**
 * Temporarily replaces the player's input with an empty {@link ClientInput} to
 * prevent the player from controlling their character or vehicle. Currently
 * only used by Freecam in Camera mode. If any other hacks need this
 * functionality in the future, this file should be updated accordingly.
 *
 * <p>
 * Use {@link TempRealInput} (try-with-resources) to temporarily restore the
 * real input when needed. {@link UpdateListener#onUpdate()} events already have
 * it restored automatically.
 */
public enum InputFaker
{
	;
	
	private static final Minecraft MC = WurstClient.MC;
	
	private static ClientInput realInput;
	private static int swapDepth;
	
	public static void swapIfNeeded()
	{
		if(!WurstClient.INSTANCE.getHax().freecamHack.isMovingCamera())
			return;
		
		swapDepth++;
		if(swapDepth > 1)
			return;
		
		LocalPlayer player = MC.player;
		realInput = player.input;
		player.input.tick();
		player.input = new ClientInput();
	}
	
	public static void restoreIfNeeded()
	{
		if(swapDepth > 0)
			swapDepth--;
		
		if(swapDepth > 0 || realInput == null)
			return;
		
		MC.player.input = realInput;
		realInput = null;
	}
	
	/**
	 * An {@link AutoCloseable} scope that temporarily restores the real
	 * input when needed. {@link UpdateListener#onUpdate()} events already have
	 * it restored automatically.
	 *
	 * <p>
	 * Usage: try(TempRealInput ignore = new TempRealInput()) { ... }
	 */
	public static class TempRealInput implements AutoCloseable
	{
		public TempRealInput()
		{
			if(realInput != null)
				MC.player.input = realInput;
		}
		
		@Override
		public void close()
		{
			if(realInput != null)
				MC.player.input = new ClientInput();
		}
	}
}
