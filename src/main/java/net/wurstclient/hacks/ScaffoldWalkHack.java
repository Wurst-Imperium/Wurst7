/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		BlockPos belowPlayer =
			BlockPos.containing(MC.player.position()).below();
		
		// check if block is already placed
		if(!BlockUtils.getState(belowPlayer).canBeReplaced())
			return;
		
		// search blocks in hotbar
		int newSlot = -1;
		for(int i = 0; i < 9; i++)
		{
			// filter out non-block items
			ItemStack stack = MC.player.getInventory().getItem(i);
			if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
				continue;
			
			// filter out non-solid blocks
			Block block = Block.byItem(stack.getItem());
			BlockState state = block.defaultBlockState();
			if(!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE,
				BlockPos.ZERO))
				continue;
			
			// filter out blocks that would fall
			if(block instanceof FallingBlock && FallingBlock
				.isFree(BlockUtils.getState(belowPlayer.below())))
				continue;
			
			newSlot = i;
			break;
		}
		
		// check if any blocks were found
		if(newSlot == -1)
			return;
		
		// set slot
		int oldSlot = MC.player.getInventory().getSelectedSlot();
		MC.player.getInventory().setSelectedSlot(newSlot);
		
		scaffoldTo(belowPlayer);
		
		// reset slot
		MC.player.getInventory().setSelectedSlot(oldSlot);
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
			BlockPos neighbor = belowPlayer.relative(side);
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
				
				BlockPos neighbor = belowPlayer.relative(side).relative(side2);
				if(placeBlock(neighbor))
					return;
			}
	}
	
	private boolean placeBlock(BlockPos pos)
	{
		Vec3 eyesPos = RotationUtils.getEyesPos();
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.relative(side);
			Direction side2 = side.getOpposite();
			
			// check if side is visible (facing away from player)
			if(eyesPos.distanceToSqr(Vec3.atCenterOf(pos)) >= eyesPos
				.distanceToSqr(Vec3.atCenterOf(neighbor)))
				continue;
			
			// check if neighbor can be right clicked
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			Vec3 hitVec = Vec3.atCenterOf(neighbor)
				.add(Vec3.atLowerCornerOf(side2.getUnitVec3i()).scale(0.5));
			
			// check if hitVec is within range (4.25 blocks)
			if(eyesPos.distanceToSqr(hitVec) > 18.0625)
				continue;
			
			// place block
			RotationUtils.getNeededRotations(hitVec).sendPlayerLookPacket();
			IMC.getInteractionManager().rightClickBlock(neighbor, side2,
				hitVec);
			MC.player.swing(InteractionHand.MAIN_HAND);
			MC.rightClickDelay = 4;
			
			return true;
		}
		
		return false;
	}
}
