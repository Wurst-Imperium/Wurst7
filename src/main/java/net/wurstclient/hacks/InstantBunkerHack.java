/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.packet.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"instant bunker"})
public final class InstantBunkerHack extends Hack
	implements UpdateListener, RenderListener
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
	
	private int blockIndex;
	private boolean building;
	
	public InstantBunkerHack()
	{
		super("InstantBunker",
			"Builds a small bunker around you. Needs 57 blocks.");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	public void onEnable()
	{
		// get start pos and facings
		BlockPos startPos = new BlockPos(MC.player);
		Direction facing = MC.player.getHorizontalFacing();
		Direction facing2 = facing.rotateYCounterclockwise();
		
		// set positions
		positions.clear();
		for(int[] pos : template)
			positions.add(startPos.up(pos[1]).offset(facing, pos[2])
				.offset(facing2, pos[0]));
		
		if(!"".isEmpty())// mode.getSelected() == 1)
		{
			// initialize building process
			blockIndex = 0;
			building = true;
			IMC.setItemUseCooldown(4);
		}
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		building = false;
	}
	
	@Override
	public void onUpdate()
	{
		// build instantly
		if(!building)
		{
			for(BlockPos pos : positions)
				if(BlockUtils.getState(pos).getMaterial().isReplaceable())
					placeBlockSimple(pos);
			MC.player.swingHand(Hand.MAIN_HAND);
			setEnabled(false);
			return;
		}
		
		// place next block
		// if(blockIndex < positions.size() && (IMC.getItemUseCooldown() == 0
		// || WURST.getHax().fastPlaceHack.isEnabled()))
		// {
		// BlockPos pos = positions.get(blockIndex);
		//
		// if(BlockUtils.getState(pos).getMaterial().isReplaceable())
		// {
		// if(!BlockUtils.placeBlockLegit(pos))
		// {
		// BlockPos playerPos = new BlockPos(MC.player);
		// if(MC.player.onGround
		// && Math.abs(pos.getX() - playerPos.getX()) == 2
		// && pos.getY() - playerPos.getY() == 2
		// && Math.abs(pos.getZ() - playerPos.getZ()) == 2)
		// MC.player.jump();
		// }
		// }else
		// {
		// blockIndex++;
		// if(blockIndex == positions.size())
		// {
		// building = false;
		// setEnabled(false);
		// }
		// }
		// }
	}
	
	private void placeBlockSimple(BlockPos pos)
	{
		Direction side = null;
		Direction[] sides = Direction.values();
		
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = new Vec3d(pos).add(0.5, 0.5, 0.5);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		Vec3d[] hitVecs = new Vec3d[sides.length];
		for(int i = 0; i < sides.length; i++)
			hitVecs[i] =
				posVec.add(new Vec3d(sides[i].getVector()).multiply(0.5));
		
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
			if(MC.world.rayTraceBlock(eyesPos, hitVecs[i], neighbor,
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
		MC.player.networkHandler
			.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
		
		// reset timer
		IMC.setItemUseCooldown(4);
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		if(!building || blockIndex >= positions.size())
			return;
		
		// scale and offset
		double scale = 1.0 * 7.0 / 8.0;
		double offset = (1.0 - scale) / 2.0;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		// green box
		{
			GL11.glDepthMask(false);
			GL11.glColor4f(0, 1, 0, 0.15F);
			BlockPos pos = positions.get(blockIndex);
			
			GL11.glPushMatrix();
			GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
			GL11.glTranslated(offset, offset, offset);
			GL11.glScaled(scale, scale, scale);
			
			RenderUtils.drawSolidBox();
			
			GL11.glPopMatrix();
			GL11.glDepthMask(true);
		}
		
		// black outlines
		GL11.glColor4f(0, 0, 0, 0.5F);
		for(int i = blockIndex; i < positions.size(); i++)
		{
			BlockPos pos = positions.get(i);
			
			GL11.glPushMatrix();
			GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
			GL11.glTranslated(offset, offset, offset);
			GL11.glScaled(scale, scale, scale);
			
			RenderUtils.drawOutlinedBox();
			
			GL11.glPopMatrix();
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
}
