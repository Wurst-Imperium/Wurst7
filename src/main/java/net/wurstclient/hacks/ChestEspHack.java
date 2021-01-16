/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public class ChestEspHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	private final EnumSetting<Style> style =
		new EnumSetting<>("Style", Style.values(), Style.BOXES);
	
	private final ArrayList<Box> basicChests = new ArrayList<>();
	private final ArrayList<Box> trappedChests = new ArrayList<>();
	private final ArrayList<Box> enderChests = new ArrayList<>();
	private final ArrayList<Box> shulkerBoxes = new ArrayList<>();
	private final ArrayList<Entity> minecarts = new ArrayList<>();
	
	private int greenBox;
	private int orangeBox;
	private int cyanBox;
	private int purpleBox;
	private int normalChests;
	
	public ChestEspHack()
	{
		super("ChestESP",
			"Highlights nearby chests.\n"
				+ "\u00a7agreen\u00a7r - normal chests & barrels\n"
				+ "\u00a76orange\u00a7r - trapped chests\n"
				+ "\u00a7bcyan\u00a7r - ender chests\n"
				+ "\u00a7dpurple\u00a7r - shulker boxes");
		
		setCategory(Category.RENDER);
		addSetting(style);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		setupDisplayLists();
	}
	
	private void setupDisplayLists()
	{
		Box box = new Box(BlockPos.ORIGIN);
		
		greenBox = GL11.glGenLists(1);
		GL11.glNewList(greenBox, GL11.GL_COMPILE);
		GL11.glColor4f(0, 1, 0, 0.25F);
		RenderUtils.drawSolidBox(box);
		GL11.glColor4f(0, 1, 0, 0.5F);
		RenderUtils.drawOutlinedBox(box);
		GL11.glEndList();
		
		orangeBox = GL11.glGenLists(1);
		GL11.glNewList(orangeBox, GL11.GL_COMPILE);
		GL11.glColor4f(1, 0.5F, 0, 0.25F);
		RenderUtils.drawSolidBox(box);
		GL11.glColor4f(1, 0.5F, 0, 0.5F);
		RenderUtils.drawOutlinedBox(box);
		GL11.glEndList();
		
		cyanBox = GL11.glGenLists(1);
		GL11.glNewList(cyanBox, GL11.GL_COMPILE);
		GL11.glColor4f(0, 1, 1, 0.25F);
		RenderUtils.drawSolidBox(box);
		GL11.glColor4f(0, 1, 1, 0.5F);
		RenderUtils.drawOutlinedBox(box);
		GL11.glEndList();
		
		purpleBox = GL11.glGenLists(1);
		GL11.glNewList(purpleBox, GL11.GL_COMPILE);
		GL11.glColor4f(1, 0, 1, 0.25F);
		RenderUtils.drawSolidBox(box);
		GL11.glColor4f(1, 0, 1, 0.5F);
		RenderUtils.drawOutlinedBox(box);
		GL11.glEndList();
		
		normalChests = GL11.glGenLists(1);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		deleteDisplayLists();
	}
	
	private void deleteDisplayLists()
	{
		GL11.glDeleteLists(greenBox, 1);
		GL11.glDeleteLists(orangeBox, 1);
		GL11.glDeleteLists(cyanBox, 1);
		GL11.glDeleteLists(purpleBox, 1);
		GL11.glDeleteLists(normalChests, 1);
	}
	
	@Override
	public void onUpdate()
	{
		basicChests.clear();
		trappedChests.clear();
		enderChests.clear();
		shulkerBoxes.clear();
		
		for(BlockEntity blockEntity : MC.world.blockEntities)
			if(blockEntity instanceof TrappedChestBlockEntity)
			{
				Box box = getBoxFromChest((ChestBlockEntity)blockEntity);
				
				if(box != null)
					trappedChests.add(box);
				
			}else if(blockEntity instanceof ChestBlockEntity)
			{
				Box box = getBoxFromChest((ChestBlockEntity)blockEntity);
				
				if(box != null)
					basicChests.add(box);
				
			}else if(blockEntity instanceof EnderChestBlockEntity)
			{
				BlockPos pos = blockEntity.getPos();
				if(!BlockUtils.canBeClicked(pos))
					continue;
				
				Box bb = BlockUtils.getBoundingBox(pos);
				enderChests.add(bb);
				
			}else if(blockEntity instanceof ShulkerBoxBlockEntity)
			{
				BlockPos pos = blockEntity.getPos();
				if(!BlockUtils.canBeClicked(pos))
					continue;
				
				Box bb = BlockUtils.getBoundingBox(pos);
				shulkerBoxes.add(bb);
				
			}else if(blockEntity instanceof BarrelBlockEntity)
			{
				BlockPos pos = blockEntity.getPos();
				if(!BlockUtils.canBeClicked(pos))
					continue;
				
				Box bb = BlockUtils.getBoundingBox(pos);
				basicChests.add(bb);
			}
		
		GL11.glNewList(normalChests, GL11.GL_COMPILE);
		renderBoxes(basicChests, greenBox);
		renderBoxes(trappedChests, orangeBox);
		renderBoxes(enderChests, cyanBox);
		renderBoxes(shulkerBoxes, purpleBox);
		GL11.glEndList();
		
		minecarts.clear();
		for(Entity entity : MC.world.getEntities())
			if(entity instanceof ChestMinecartEntity)
				minecarts.add(entity);
	}
	
	private Box getBoxFromChest(ChestBlockEntity chestBE)
	{
		BlockState state = chestBE.getCachedState();
		if(!state.contains(ChestBlock.CHEST_TYPE))
			return null;
		
		ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
		
		// ignore other block in double chest
		if(chestType == ChestType.LEFT)
			return null;
		
		BlockPos pos = chestBE.getPos();
		if(!BlockUtils.canBeClicked(pos))
			return null;
		
		Box box = BlockUtils.getBoundingBox(pos);
		
		// larger box for double chest
		if(chestType != ChestType.SINGLE)
		{
			BlockPos pos2 = pos.offset(ChestBlock.getFacing(state));
			
			if(BlockUtils.canBeClicked(pos2))
			{
				Box box2 = BlockUtils.getBoundingBox(pos2);
				box = box.union(box2);
			}
		}
		
		return box;
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.getSelected().lines)
			event.cancel();
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		ArrayList<Box> minecartBoxes = calculateMinecartBoxes(partialTicks);
		
		if(style.getSelected().boxes)
		{
			GL11.glCallList(normalChests);
			renderBoxes(minecartBoxes, greenBox);
		}
		
		if(style.getSelected().lines)
		{
			Vec3d start = RotationUtils.getClientLookVec()
				.add(RenderUtils.getCameraPos());
			
			GL11.glBegin(GL11.GL_LINES);
			
			GL11.glColor4f(0, 1, 0, 0.5F);
			renderLines(start, basicChests);
			renderLines(start, minecartBoxes);
			
			GL11.glColor4f(1, 0.5F, 0, 0.5F);
			renderLines(start, trappedChests);
			
			GL11.glColor4f(0, 1, 1, 0.5F);
			renderLines(start, enderChests);
			
			GL11.glColor4f(1, 0, 1, 0.5F);
			renderLines(start, shulkerBoxes);
			
			GL11.glEnd();
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private ArrayList<Box> calculateMinecartBoxes(float partialTicks)
	{
		ArrayList<Box> minecartBoxes = new ArrayList<>(minecarts.size());
		
		minecarts.forEach(e -> {
			double offsetX = -(e.getX() - e.lastRenderX)
				+ (e.getX() - e.lastRenderX) * partialTicks;
			double offsetY = -(e.getY() - e.lastRenderY)
				+ (e.getY() - e.lastRenderY) * partialTicks;
			double offsetZ = -(e.getZ() - e.lastRenderZ)
				+ (e.getZ() - e.lastRenderZ) * partialTicks;
			minecartBoxes
				.add(e.getBoundingBox().offset(offsetX, offsetY, offsetZ));
		});
		
		return minecartBoxes;
	}
	
	private void renderBoxes(ArrayList<Box> boxes, int displayList)
	{
		for(Box box : boxes)
		{
			GL11.glPushMatrix();
			GL11.glTranslated(box.minX, box.minY, box.minZ);
			GL11.glScaled(box.maxX - box.minX, box.maxY - box.minY,
				box.maxZ - box.minZ);
			GL11.glCallList(displayList);
			GL11.glPopMatrix();
		}
	}
	
	private void renderLines(Vec3d start, ArrayList<Box> boxes)
	{
		for(Box box : boxes)
		{
			Vec3d end = box.getCenter();
			GL11.glVertex3d(start.x, start.y, start.z);
			GL11.glVertex3d(end.x, end.y, end.z);
		}
	}
	
	private enum Style
	{
		BOXES("Boxes only", true, false),
		LINES("Lines only", false, true),
		LINES_AND_BOXES("Lines and boxes", true, true);
		
		private final String name;
		private final boolean boxes;
		private final boolean lines;
		
		private Style(String name, boolean boxes, boolean lines)
		{
			this.name = name;
			this.boxes = boxes;
			this.lines = lines;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
