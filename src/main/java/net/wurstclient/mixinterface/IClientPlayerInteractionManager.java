/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.packet.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public interface IClientPlayerInteractionManager
{
	public float getCurrentBreakingProgress();
	
	public void setBreakingBlock(boolean breakingBlock);
	
	public ItemStack windowClick_PICKUP(int slot);
	
	public ItemStack windowClick_QUICK_MOVE(int slot);
	
	public ItemStack windowClick_THROW(int slot);
	
	public void rightClickItem();
	
	public void rightClickBlock(BlockPos pos, Direction side, Vec3d hitVec);
	
	public void sendPlayerActionC2SPacket(PlayerActionC2SPacket.Action action,
		BlockPos blockPos, Direction direction);
	
	public void setBlockHitDelay(int delay);

	public void setReachDistance(float dist);

	public void setHasExtendedReach(boolean value);
}
