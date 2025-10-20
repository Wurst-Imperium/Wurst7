/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

public abstract class AutoFarmPlantType
{
	/**
	 * Returns <code>true</code> if the given block contains a plant of this
	 * type and would be the right spot to replant that plant if it wasn't
	 * already there.
	 */
	public abstract boolean isReplantingSpot(BlockPos pos, BlockState state);
	
	/**
	 * Returns <code>true</code> if the required planting surface (farmland,
	 * logs, etc.) for a plant of this type to be planted at <code>pos</code> is
	 * present.
	 */
	public abstract boolean hasPlantingSurface(BlockPos pos);
	
	/**
	 * Returns the item type that is used to plant this plant type.
	 */
	public abstract Item getSeedItem();
	
	/**
	 * Returns <code>true</code> if the given block contains a plant of this
	 * type that is ready to be harvested by mining it.
	 */
	public abstract boolean shouldHarvestByMining(BlockPos pos,
		BlockState state);
}
