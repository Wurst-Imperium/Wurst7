/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.server.network.packet.PlayerActionC2SPacket;
import net.minecraft.server.network.packet.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.BlockBreakingProgressListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;

@SearchTags({"FastMine", "SpeedMine", "SpeedyGonzales", "fast break",
	"fast mine", "speed mine", "speedy gonzales"})
public final class FastBreakHack extends Hack
	implements UpdateListener, BlockBreakingProgressListener
{
	public FastBreakHack()
	{
		super("FastBreak", "Allows you to break blocks faster.\n"
			+ "Tip: This works with Nuker.");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getEventManager().add(UpdateListener.class, this);
		WURST.getEventManager().add(BlockBreakingProgressListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		WURST.getEventManager().remove(UpdateListener.class, this);
		WURST.getEventManager().remove(BlockBreakingProgressListener.class,
			this);
	}
	
	@Override
	public void onUpdate()
	{
		IMC.getInteractionManager().setBlockHitDelay(0);
	}
	
	@Override
	public void onBlockBreakingProgress(BlockBreakingProgressEvent event)
	{
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		
		if(im.getCurrentBreakingProgress() >= 1)
			return;
		
		Action action = PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK;
		BlockPos blockPos = event.getBlockPos();
		Direction direction = event.getDirection();
		im.sendPlayerActionC2SPacket(action, blockPos, direction);
	}
}
