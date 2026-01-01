/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import java.util.ArrayList;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IKeyBinding;

public abstract class PathProcessor
{
	protected static final WurstClient WURST = WurstClient.INSTANCE;
	protected static final Minecraft MC = WurstClient.MC;
	
	private static final KeyMapping[] CONTROLS =
		{MC.options.keyUp, MC.options.keyDown, MC.options.keyRight,
			MC.options.keyLeft, MC.options.keyJump, MC.options.keyShift};
	
	protected final ArrayList<PathPos> path;
	protected int index;
	protected boolean done;
	protected int ticksOffPath;
	
	public PathProcessor(ArrayList<PathPos> path)
	{
		if(path.isEmpty())
			throw new IllegalStateException("There is no path!");
		
		this.path = path;
	}
	
	public abstract void process();
	
	public abstract boolean canBreakBlocks();
	
	public final int getIndex()
	{
		return index;
	}
	
	public final boolean isDone()
	{
		return done;
	}
	
	public final int getTicksOffPath()
	{
		return ticksOffPath;
	}
	
	protected final void facePosition(BlockPos pos)
	{
		WURST.getRotationFaker()
			.faceVectorClientIgnorePitch(Vec3.atCenterOf(pos));
	}
	
	public static final void lockControls()
	{
		// disable keys
		for(KeyMapping key : CONTROLS)
			key.setDown(false);
		
		// disable sprinting
		MC.player.setSprinting(false);
	}
	
	public static final void releaseControls()
	{
		// reset keys
		for(KeyMapping key : CONTROLS)
			IKeyBinding.get(key).resetPressedState();
	}
}
