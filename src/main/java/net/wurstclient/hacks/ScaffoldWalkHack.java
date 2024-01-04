/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"scaffold walk", "BridgeWalk", "bridge walk", "AutoBridge",
	"auto bridge", "tower"})
public final class ScaffoldWalkHack extends Hack implements UpdateListener
{
	public ScaffoldWalkHack()
	{
		super("ScaffoldWalk");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		BlockPos belowPlayer = BlockPos.ofFloored(MC.player.getPos()).down();
		
		// check if block is already placed
		if(!BlockUtils.getState(belowPlayer).isReplaceable())
			return;
		
		// search blocks in hotbar
		int newSlot = -1;
		for(int i = 0; i < 9; i++)
		{
			// filter out non-block items
			ItemStack stack = MC.player.getInventory().getStack(i);
			if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
				continue;
			
			// filter out non-solid blocks
			Block block = Block.getBlockFromItem(stack.getItem());
			BlockState state = block.getDefaultState();
			if(!state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN))
				continue;
			
			// filter out blocks that would fall
			if(block instanceof FallingBlock && FallingBlock
				.canFallThrough(BlockUtils.getState(belowPlayer.down())))
				continue;
			
			newSlot = i;
			break;
		}
		
		// check if any blocks were found
		if(newSlot == -1)
			return;
		
		// set slot
		int oldSlot = MC.player.getInventory().selectedSlot;
		MC.player.getInventory().selectedSlot = newSlot;
		
		scaffoldTo(belowPlayer);
		
		// reset slot
		MC.player.getInventory().selectedSlot = oldSlot;
	}
	
	private void scaffoldTo(BlockPos belowPlayer)
	{
		// tries to place a block directly under the player
		if(placeBlock(belowPlayer))
			return;
			
		// if that doesn't work, tries to place a block next to the block that's
		// under the player
		Direction[] sides = Direction.values();
		for(Direction side : sides)
		{
			BlockPos neighbor = belowPlayer.offset(side);
			if(placeBlock(neighbor))
				return;
		}
		
		// if that doesn't work, tries to place a block next to a block that's
		// next to the block that's under the player
		for(Direction side : sides)
			for(Direction side2 : Arrays.copyOfRange(sides, side.ordinal(), 6))
			{
				if(side.getOpposite().equals(side2))
					continue;
				
				BlockPos neighbor = belowPlayer.offset(side).offset(side2);
				if(placeBlock(neighbor))
					return;
			}
	}
	
	private boolean placeBlock(BlockPos pos)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.offset(side);
			Direction side2 = side.getOpposite();
			
			// check if side is visible (facing away from player)
			if(eyesPos.squaredDistanceTo(Vec3d.ofCenter(pos)) >= eyesPos
				.squaredDistanceTo(Vec3d.ofCenter(neighbor)))
				continue;
			
			// check if neighbor can be right clicked
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			Vec3d hitVec = Vec3d.ofCenter(neighbor)
				.add(Vec3d.of(side2.getVector()).multiply(0.5));
			
			// check if hitVec is within range (4.25 blocks)
			if(eyesPos.squaredDistanceTo(hitVec) > 18.0625)
				continue;
			
			// place block
			RotationUtils.getNeededRotations(hitVec).sendPlayerLookPacket();
			IMC.getInteractionManager().rightClickBlock(neighbor, side2,
				hitVec);
			MC.player.swingHand(Hand.MAIN_HAND);
			MC.itemUseCooldown = 4;
			
			return true;
		}
		
		return false;
	}
}
