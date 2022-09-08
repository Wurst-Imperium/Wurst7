/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
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
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public class ChestEspHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	private final ColorSetting basicColor = new ColorSetting("Chest color",
		"Normal chests will be\n" + "highlighted in this color.", Color.GREEN);
	
	private final ColorSetting trapColor = new ColorSetting("Trap color",
		"Trapped chests will be\n" + "highlighted in this color.",
		new Color(0xFF8000));
	
	private final ColorSetting enderColor = new ColorSetting("Ender color",
		"Ender chests will be\n" + "highlighted in this color.", Color.CYAN);
	
	private final ColorSetting shulkerColor = new ColorSetting("Shulker color",
		"Shulker boxes will be\n" + "highlighted in this color.",
		Color.MAGENTA);
	
	private final ColorSetting cartColor = new ColorSetting("Cart color",
		"Minecarts will be\n" + "highlighted in this color.", Color.GREEN);
	
	private final EnumSetting<Style> style =
		new EnumSetting<>("Style", Style.values(), Style.BOXES);
	
	private final ArrayList<Box> basicChests = new ArrayList<>();
	private final ArrayList<Box> trapChests = new ArrayList<>();
	private final ArrayList<Box> enderChests = new ArrayList<>();
	private final ArrayList<Box> shulkerBoxes = new ArrayList<>();
	private final ArrayList<Entity> minecarts = new ArrayList<>();
	
	private int solidBox;
	private int outlinedBox;
	
	public ChestEspHack()
	{
		super("ChestESP", "Highlights nearby chests.");
		
		setCategory(Category.RENDER);
		addSetting(basicColor);
		addSetting(trapColor);
		addSetting(enderColor);
		addSetting(shulkerColor);
		addSetting(cartColor);
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
		
		solidBox = GL11.glGenLists(1);
		GL11.glNewList(solidBox, GL11.GL_COMPILE);
		RenderUtils.drawSolidBox(box);
		GL11.glEndList();
		
		outlinedBox = GL11.glGenLists(1);
		GL11.glNewList(outlinedBox, GL11.GL_COMPILE);
		RenderUtils.drawOutlinedBox(box);
		GL11.glEndList();
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
		GL11.glDeleteLists(solidBox, 1);
		GL11.glDeleteLists(outlinedBox, 1);
	}
	
	@Override
	public void onUpdate()
	{
		basicChests.clear();
		trapChests.clear();
		enderChests.clear();
		shulkerBoxes.clear();
		
		for(BlockEntity blockEntity : MC.world.blockEntities)
			if(blockEntity instanceof TrappedChestBlockEntity)
			{
				Box box = getBoxFromChest((ChestBlockEntity)blockEntity);
				
				if(box != null)
					trapChests.add(box);
				
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
		RenderUtils.applyRegionalRenderOffset();
		
		ArrayList<Box> minecartBoxes = calculateMinecartBoxes(partialTicks);
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		if(style.getSelected().boxes)
		{
			renderBoxes(basicChests, basicColor.getColorF(), regionX, regionZ);
			renderBoxes(trapChests, trapColor.getColorF(), regionX, regionZ);
			renderBoxes(enderChests, enderColor.getColorF(), regionX, regionZ);
			renderBoxes(shulkerBoxes, shulkerColor.getColorF(), regionX,
				regionZ);
			renderBoxes(minecartBoxes, cartColor.getColorF(), regionX, regionZ);
		}
		
		if(style.getSelected().lines)
		{
			Vec3d start = RotationUtils.getClientLookVec()
				.add(RenderUtils.getCameraPos());
			
			GL11.glBegin(GL11.GL_LINES);
			
			float[] basicColorF = basicColor.getColorF();
			GL11.glColor4f(basicColorF[0], basicColorF[1], basicColorF[2],
				0.5F);
			renderLines(start, basicChests, regionX, regionZ);
			
			float[] trapColorF = trapColor.getColorF();
			GL11.glColor4f(trapColorF[0], trapColorF[1], trapColorF[2], 0.5F);
			renderLines(start, trapChests, regionX, regionZ);
			
			float[] enderColorF = enderColor.getColorF();
			GL11.glColor4f(enderColorF[0], enderColorF[1], enderColorF[2],
				0.5F);
			renderLines(start, enderChests, regionX, regionZ);
			
			float[] shulkerColorF = shulkerColor.getColorF();
			GL11.glColor4f(shulkerColorF[0], shulkerColorF[1], shulkerColorF[2],
				0.5F);
			renderLines(start, shulkerBoxes, regionX, regionZ);
			
			float[] cartColorF = cartColor.getColorF();
			GL11.glColor4f(cartColorF[0], cartColorF[1], cartColorF[2], 0.5F);
			renderLines(start, minecartBoxes, regionX, regionZ);
			
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
	
	private void renderBoxes(ArrayList<Box> boxes, float[] colorF, int regionX,
		int regionZ)
	{
		for(Box box : boxes)
		{
			GL11.glPushMatrix();
			
			GL11.glTranslated(box.minX - regionX, box.minY, box.minZ - regionZ);
			
			GL11.glScaled(box.maxX - box.minX, box.maxY - box.minY,
				box.maxZ - box.minZ);
			
			GL11.glColor4f(colorF[0], colorF[1], colorF[2], 0.25F);
			GL11.glCallList(solidBox);
			
			GL11.glColor4f(colorF[0], colorF[1], colorF[2], 0.5F);
			GL11.glCallList(outlinedBox);
			
			GL11.glPopMatrix();
		}
	}
	
	private void renderLines(Vec3d start, ArrayList<Box> boxes, int regionX,
		int regionZ)
	{
		for(Box box : boxes)
		{
			Vec3d end = box.getCenter();
			GL11.glVertex3d(start.x - regionX, start.y, start.z - regionZ);
			GL11.glVertex3d(end.x - regionX, end.y, end.z - regionZ);
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
