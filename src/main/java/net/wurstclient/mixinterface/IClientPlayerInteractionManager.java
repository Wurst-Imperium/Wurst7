/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
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
	
	public void windowClick_SWAP(int from, int to);
	
	public void rightClickItem();
	
	public void rightClickBlock(BlockPos pos, Direction side, Vec3d hitVec,
		Hand hand);
	
	public void rightClickBlock(BlockPos pos, Direction side, Vec3d hitVec);
	
	public void sendPlayerActionC2SPacket(PlayerActionC2SPacket.Action action,
		BlockPos blockPos, Direction direction);
	
	public void setBlockHitDelay(int delay);
}
