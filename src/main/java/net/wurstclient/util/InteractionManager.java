/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;

public final class InteractionManager
{
	private static final MinecraftClient MC = WurstClient.MC;
	
	public static ItemStack windowClick_PICKUP(int slot)
	{
		return MC.interactionManager.method_2906(0, slot, 0,
			SlotActionType.PICKUP, MC.player);
	}
	
	public static ItemStack windowClick_QUICK_MOVE(int slot)
	{
		return MC.interactionManager.method_2906(0, slot, 0,
			SlotActionType.QUICK_MOVE, MC.player);
	}
	
	public static ItemStack windowClick_THROW(int slot)
	{
		return MC.interactionManager.method_2906(0, slot, 1,
			SlotActionType.THROW, MC.player);
	}
	
	public static void rightClickItem()
	{
		MC.interactionManager.interactItem(MC.player, MC.world, Hand.MAIN_HAND);
	}
	
	public static void rightClickBlock(BlockPos pos, Direction side,
		Vec3d hitVec)
	{
		MC.interactionManager.interactBlock(MC.player, MC.world, Hand.MAIN_HAND,
			new BlockHitResult(hitVec, side, pos, false));
	}
}
