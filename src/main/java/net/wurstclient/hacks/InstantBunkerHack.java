/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
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
		
		if(!MC.player.onGround())
		{
			ChatUtils.error("Can't build this in mid-air.");
			setEnabled(false);
			return;
		}
		
		ItemStack stack = MC.player.getInventory().getSelectedItem();
		
		if(!(stack.getItem() instanceof BlockItem))
		{
			ChatUtils.error("You must have blocks in the main hand.");
			setEnabled(false);
			return;
		}
		
		if(stack.getCount() < 57 && !MC.player.getAbilities().instabuild)
			ChatUtils.warning("Not enough blocks. Bunker may be incomplete.");
		
		// get start pos and facings
		BlockPos startPos = BlockPos.containing(MC.player.position());
		Direction facing = MC.player.getDirection();
		Direction facing2 = facing.getCounterClockWise();
		
		// set positions
		positions.clear();
		for(int[] pos : template)
			positions.add(startPos.above(pos[1]).relative(facing, pos[2])
				.relative(facing2, pos[0]));
		
		startTimer = 2;
		MC.player.jumpFromGround();
		
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
				if(BlockUtils.getState(pos).canBeReplaced()
					&& !MC.player.getBoundingBox().intersects(new AABB(pos)))
					placeBlockSimple(pos);
			MC.player.swing(InteractionHand.MAIN_HAND);
			
			if(MC.player.onGround())
				setEnabled(false);
		}
	}
	
	private void placeBlockSimple(BlockPos pos)
	{
		Direction side = null;
		Direction[] sides = Direction.values();
		
		Vec3 eyesPos = RotationUtils.getEyesPos();
		Vec3 posVec = Vec3.atCenterOf(pos);
		double distanceSqPosVec = eyesPos.distanceToSqr(posVec);
		
		Vec3[] hitVecs = new Vec3[sides.length];
		for(int i = 0; i < sides.length; i++)
			hitVecs[i] = posVec
				.add(Vec3.atLowerCornerOf(sides[i].getUnitVec3i()).scale(0.5));
		
		for(int i = 0; i < sides.length; i++)
		{
			// check if neighbor can be right clicked
			BlockPos neighbor = pos.relative(sides[i]);
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			// check line of sight
			BlockState neighborState = BlockUtils.getState(neighbor);
			VoxelShape neighborShape =
				neighborState.getShape(MC.level, neighbor);
			if(MC.level.clipWithInteractionOverride(eyesPos, hitVecs[i],
				neighbor, neighborShape, neighborState) != null)
				continue;
			
			side = sides[i];
			break;
		}
		
		if(side == null)
			for(int i = 0; i < sides.length; i++)
			{
				// check if neighbor can be right clicked
				if(!BlockUtils.canBeClicked(pos.relative(sides[i])))
					continue;
				
				// check if side is facing away from player
				if(distanceSqPosVec > eyesPos.distanceToSqr(hitVecs[i]))
					continue;
				
				side = sides[i];
				break;
			}
		
		if(side == null)
			return;
		
		Vec3 hitVec = hitVecs[side.ordinal()];
		
		// face block
		// WURST.getRotationFaker().faceVectorPacket(hitVec);
		// if(RotationUtils.getAngleToLastReportedLookVec(hitVec) > 1)
		// return;
		
		// check timer
		// if(IMC.getItemUseCooldown() > 0)
		// return;
		
		// place block
		IMC.getInteractionManager().rightClickBlock(pos.relative(side),
			side.getOpposite(), hitVec);
		
		// swing arm
		SwingHand.SERVER.swing(InteractionHand.MAIN_HAND);
		
		// reset timer
		MC.rightClickDelay = 4;
	}
}
