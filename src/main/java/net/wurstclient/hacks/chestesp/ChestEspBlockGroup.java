/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chestesp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.wurstclient.util.BlockUtils;

public abstract class ChestEspBlockGroup extends ChestEspGroup
{
	protected abstract boolean matches(BlockEntity be);
	
	public final void addIfMatches(BlockEntity be)
	{
		if(!matches(be))
			return;
		
		AABB box = getBox(be);
		if(box == null)
			return;
		
		boxes.add(box);
	}
	
	private AABB getBox(BlockEntity be)
	{
		BlockPos pos = be.getBlockPos();
		
		if(!BlockUtils.canBeClicked(pos))
			return null;
		
		if(be instanceof ChestBlockEntity)
			return getChestBox((ChestBlockEntity)be);
		
		return BlockUtils.getBoundingBox(pos);
	}
	
	private AABB getChestBox(ChestBlockEntity chestBE)
	{
		BlockState state = chestBE.getBlockState();
		if(!state.hasProperty(ChestBlock.TYPE))
			return null;
		
		ChestType chestType = state.getValue(ChestBlock.TYPE);
		
		// ignore other block in double chest
		if(chestType == ChestType.LEFT)
			return null;
		
		BlockPos pos = chestBE.getBlockPos();
		AABB box = BlockUtils.getBoundingBox(pos);
		
		// larger box for double chest
		if(chestType != ChestType.SINGLE)
		{
			BlockPos pos2 =
				pos.relative(ChestBlock.getConnectedDirection(state));
			
			if(BlockUtils.canBeClicked(pos2))
			{
				AABB box2 = BlockUtils.getBoundingBox(pos2);
				box = box.minmax(box2);
			}
		}
		
		return box;
	}
	
}
