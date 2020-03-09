/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
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
import net.wurstclient.util.RotationUtils.Rotation;

@SearchTags({"scaffold walk", "BridgeWalk", "bridge walk", "AutoBridge",
	"auto bridge", "tower"})
public final class ScaffoldWalkHack extends Hack implements UpdateListener
{
	public ScaffoldWalkHack()
	{
		super("ScaffoldWalk", "Automatically places blocks below your feet.");
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
		BlockPos belowPlayer = new BlockPos(MC.player).down();
		
		// check if block is already placed
		if(!BlockUtils.getState(belowPlayer).getMaterial().isReplaceable())
			return;
		
		// search blocks in hotbar
		int newSlot = -1;
		for(int i = 0; i < 9; i++)
		{
			// filter out non-block items
			ItemStack stack = MC.player.inventory.getInvStack(i);
			if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
				continue;
			
			// filter out non-solid blocks
			Block block = Block.getBlockFromItem(stack.getItem());
			BlockState state = block.getDefaultState();
			if(!Block.isShapeFullCube(state
				.getCullingShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)))
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
		int oldSlot = MC.player.inventory.selectedSlot;
		MC.player.inventory.selectedSlot = newSlot;
		
		placeBlock(belowPlayer);
		
		// reset slot
		MC.player.inventory.selectedSlot = oldSlot;
	}
	
	private boolean placeBlock(BlockPos pos)
	{
		Vec3d eyesPos = new Vec3d(MC.player.getX(),
			MC.player.getY() + MC.player.getEyeHeight(MC.player.getPose()),
			MC.player.getZ());
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.offset(side);
			Direction side2 = side.getOpposite();
			
			// check if side is visible (facing away from player)
			if(eyesPos
				.squaredDistanceTo(new Vec3d(pos).add(0.5, 0.5, 0.5)) >= eyesPos
					.squaredDistanceTo(new Vec3d(neighbor).add(0.5, 0.5, 0.5)))
				continue;
			
			// check if neighbor can be right clicked
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			Vec3d hitVec = new Vec3d(neighbor).add(0.5, 0.5, 0.5)
				.add(new Vec3d(side2.getVector()).multiply(0.5));
			
			// check if hitVec is within range (4.25 blocks)
			if(eyesPos.squaredDistanceTo(hitVec) > 18.0625)
				continue;
			
			// place block
			Rotation rotation = RotationUtils.getNeededRotations(hitVec);
			PlayerMoveC2SPacket.LookOnly packet =
				new PlayerMoveC2SPacket.LookOnly(rotation.getYaw(),
					rotation.getPitch(), MC.player.onGround);
			MC.player.networkHandler.sendPacket(packet);
			IMC.getInteractionManager().rightClickBlock(neighbor, side2,
				hitVec);
			MC.player.swingHand(Hand.MAIN_HAND);
			IMC.setItemUseCooldown(4);
			
			return true;
		}
		
		return false;
	}
}
