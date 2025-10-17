/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"instant bunker"})
public final class InstantBunkerHack extends Hack implements UpdateListener
{
	private final int[][] template = {{2, 0, 2}, {-2, 0, 2}, {2, 0, -2},
		{-2, 0, -2}, {2, 1, 2}, {-2, 1, 2}, {2, 1, -2}, {-2, 1, -2}, {2, 2, 2},
		{-2, 2, 2}, {2, 2, -2}, {-2, 2, -2}, {1, 2, 2}, {0, 2, 2}, {-1, 2, 2},
		{2, 2, 1}, {2, 2, 0}, {2, 2, -1}, {-2, 2, 1}, {-2, 2, 0}, {-2, 2, -1},
		{1, 2, -2}, {0, 2, -2}, {-1, 2, -2}, {1, 0, 2}, {0, 0, 2}, {-1, 0, 2},
		{2, 0, 1}, {2, 0, 0}, {2, 0, -1}, {-2, 0, 1}, {-2, 0, 0}, {-2, 0, -1},
		{1, 0, -2}, {0, 0, -2}, {-1, 0, -2}, {1, 1, 2}, {0, 1, 2}, {-1, 1, 2},
		{2, 1, 1}, {2, 1, 0}, {2, 1, -1}, {-2, 1, 1}, {-2, 1, 0}, {-2, 1, -1},
		{1, 1, -2}, {0, 1, -2}, {-1, 1, -2}, {1, 2, 1}, {-1, 2, 1}, {1, 2, -1},
		{-1, 2, -1}, {0, 2, 1}, {1, 2, 0}, {-1, 2, 0}, {0, 2, -1}, {0, 2, 0}};
	private final ArrayList<BlockPos> positions = new ArrayList<>();
	
	private int startTimer;
	
	public InstantBunkerHack()
	{
		super("InstantBunker");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().tunnellerHack.setEnabled(false);
		
		if(!MC.player.isOnGround())
		{
			ChatUtils.error("Can't build this in mid-air.");
			setEnabled(false);
			return;
		}
		
		ItemStack stack = MC.player.getInventory().getSelectedStack();
		
		if(!(stack.getItem() instanceof BlockItem))
		{
			ChatUtils.error("You must have blocks in the main hand.");
			setEnabled(false);
			return;
		}
		
		if(stack.getCount() < 57 && !MC.player.getAbilities().creativeMode)
			ChatUtils.warning("Not enough blocks. Bunker may be incomplete.");
		
		// get start pos and facings
		BlockPos startPos = BlockPos.ofFloored(MC.player.getEntityPos());
		Direction facing = MC.player.getHorizontalFacing();
		Direction facing2 = facing.rotateYCounterclockwise();
		
		// set positions
		positions.clear();
		for(int[] pos : template)
			positions.add(startPos.up(pos[1]).offset(facing, pos[2])
				.offset(facing2, pos[0]));
		
		startTimer = 2;
		MC.player.jump();
		
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
		if(startTimer > 0)
		{
			startTimer--;
			return;
		}
		
		// build instantly
		if(startTimer <= 0)
		{
			for(BlockPos pos : positions)
				if(BlockUtils.getState(pos).isReplaceable()
					&& !MC.player.getBoundingBox().intersects(new Box(pos)))
					placeBlockSimple(pos);
			MC.player.swingHand(Hand.MAIN_HAND);
			
			if(MC.player.isOnGround())
				setEnabled(false);
		}
	}
	
	private void placeBlockSimple(BlockPos pos)
	{
		Direction side = null;
		Direction[] sides = Direction.values();
		
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		Vec3d[] hitVecs = new Vec3d[sides.length];
		for(int i = 0; i < sides.length; i++)
			hitVecs[i] =
				posVec.add(Vec3d.of(sides[i].getVector()).multiply(0.5));
		
		for(int i = 0; i < sides.length; i++)
		{
			// check if neighbor can be right clicked
			BlockPos neighbor = pos.offset(sides[i]);
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			// check line of sight
			BlockState neighborState = BlockUtils.getState(neighbor);
			VoxelShape neighborShape =
				neighborState.getOutlineShape(MC.world, neighbor);
			if(MC.world.raycastBlock(eyesPos, hitVecs[i], neighbor,
				neighborShape, neighborState) != null)
				continue;
			
			side = sides[i];
			break;
		}
		
		if(side == null)
			for(int i = 0; i < sides.length; i++)
			{
				// check if neighbor can be right clicked
				if(!BlockUtils.canBeClicked(pos.offset(sides[i])))
					continue;
				
				// check if side is facing away from player
				if(distanceSqPosVec > eyesPos.squaredDistanceTo(hitVecs[i]))
					continue;
				
				side = sides[i];
				break;
			}
		
		if(side == null)
			return;
		
		Vec3d hitVec = hitVecs[side.ordinal()];
		
		// face block
		// WURST.getRotationFaker().faceVectorPacket(hitVec);
		// if(RotationUtils.getAngleToLastReportedLookVec(hitVec) > 1)
		// return;
		
		// check timer
		// if(IMC.getItemUseCooldown() > 0)
		// return;
		
		// place block
		IMC.getInteractionManager().rightClickBlock(pos.offset(side),
			side.getOpposite(), hitVec);
		
		// swing arm
		SwingHand.SERVER.swing(Hand.MAIN_HAND);
		
		// reset timer
		MC.itemUseCooldown = 4;
	}
}
