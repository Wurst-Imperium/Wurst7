/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.Category;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.templatetool.Area;
import net.wurstclient.hacks.templatetool.ChooseNameScreen;
import net.wurstclient.hacks.templatetool.Step;
import net.wurstclient.hacks.templatetool.Template;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.json.JsonUtils;

public final class TemplateToolHack extends Hack
	implements UpdateListener, RenderListener, GUIRenderListener
{
	private Step step;
	private BlockPos posLookingAt;
	private Area area;
	private Template template;
	private File file;
	
	public TemplateToolHack()
	{
		super("TemplateTool");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	public void onEnable()
	{
		// disable conflicting hacks
		WURST.getHax().autoBuildHack.setEnabled(false);
		WURST.getHax().bowAimbotHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		
		step = Step.START_POS;
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(GUIRenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(GUIRenderListener.class, this);
		
		for(Step step : Step.values())
			step.setPos(null);
		posLookingAt = null;
		area = null;
		template = null;
		file = null;
	}
	
	@Override
	public void onUpdate()
	{
		// select position steps
		if(step.doesSelectPos())
		{
			// continue with next step
			if(step.getPos() != null && InputUtil
				.isKeyPressed(MC.getWindow().getHandle(), GLFW.GLFW_KEY_ENTER))
			{
				step = Step.values()[step.ordinal() + 1];
				
				// delete posLookingAt
				if(!step.doesSelectPos())
					posLookingAt = null;
				
				return;
			}
			
			if(MC.crosshairTarget instanceof BlockHitResult bHitResult)
			{
				// set posLookingAt
				posLookingAt = bHitResult.getBlockPos();
				
				// offset if sneaking
				if(MC.options.sneakKey.isPressed())
					posLookingAt = posLookingAt.offset(bHitResult.getSide());
				
			}else
				posLookingAt = null;
			
			// set selected position
			if(posLookingAt != null && MC.options.useKey.isPressed())
				step.setPos(posLookingAt);
			
			// scanning area step
		}else if(step == Step.SCAN_AREA)
		{
			// initialize area
			if(area == null)
			{
				area = new Area(Step.START_POS.getPos(), Step.END_POS.getPos());
				Step.START_POS.setPos(null);
				Step.END_POS.setPos(null);
			}
			
			// scan area
			for(int i = 0; i < area.getScanSpeed()
				&& area.getIterator().hasNext(); i++)
			{
				area.setScannedBlocks(area.getScannedBlocks() + 1);
				BlockPos pos = area.getIterator().next();
				
				if(!BlockUtils.getState(pos).isReplaceable())
					area.getBlocksFound().add(pos);
			}
			
			// update progress
			area.setProgress(
				(float)area.getScannedBlocks() / (float)area.getTotalBlocks());
			
			// continue with next step
			if(!area.getIterator().hasNext())
				step = Step.values()[step.ordinal() + 1];
			
			// creating template step
		}else if(step == Step.CREATE_TEMPLATE)
		{
			// initialize template
			if(template == null)
				template = new Template(Step.FIRST_BLOCK.getPos(),
					area.getBlocksFound().size());
			
			// sort blocks by distance
			if(!area.getBlocksFound().isEmpty())
			{
				// move blocks to TreeSet
				int min = Math.max(0,
					area.getBlocksFound().size() - template.getScanSpeed());
				for(int i = area.getBlocksFound().size() - 1; i >= min; i--)
				{
					BlockPos pos = area.getBlocksFound().get(i);
					template.getRemainingBlocks().add(pos);
					area.getBlocksFound().remove(i);
				}
				
				// update progress
				template.setProgress((float)template.getRemainingBlocks().size()
					/ (float)template.getTotalBlocks());
				
				return;
			}
			
			// add closest block first
			if(template.getSortedBlocks().isEmpty()
				&& !template.getRemainingBlocks().isEmpty())
			{
				BlockPos first = template.getRemainingBlocks().first();
				template.getSortedBlocks().add(first);
				template.getRemainingBlocks().remove(first);
				template.setLastAddedBlock(first);
			}
			
			// add remaining blocks
			for(int i = 0; i < template.getScanSpeed()
				&& !template.getRemainingBlocks().isEmpty(); i++)
			{
				BlockPos current = template.getRemainingBlocks().first();
				double dCurrent = Double.MAX_VALUE;
				
				for(BlockPos pos : template.getRemainingBlocks())
				{
					double dPos =
						template.getLastAddedBlock().getSquaredDistance(pos);
					if(dPos >= dCurrent)
						continue;
					
					for(Direction facing : Direction.values())
					{
						BlockPos next = pos.offset(facing);
						if(!template.getSortedBlocks().contains(next))
							continue;
						
						current = pos;
						dCurrent = dPos;
					}
				}
				
				template.getSortedBlocks().add(current);
				template.getRemainingBlocks().remove(current);
				template.setLastAddedBlock(current);
			}
			
			// update progress
			template.setProgress((float)template.getRemainingBlocks().size()
				/ (float)template.getTotalBlocks());
			
			// continue with next step
			if(template.getSortedBlocks().size() == template.getTotalBlocks())
			{
				step = Step.values()[step.ordinal() + 1];
				MC.setScreen(new ChooseNameScreen());
			}
		}
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// scale and offset
		float scale = 7F / 8F;
		double offset = (1.0 - scale) / 2.0;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		// area
		if(area != null)
		{
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			
			// recently scanned blocks
			if(step == Step.SCAN_AREA && area.getProgress() < 1)
				for(int i = Math.max(0,
					area.getBlocksFound().size()
						- area.getScanSpeed()); i < area.getBlocksFound()
							.size(); i++)
				{
					BlockPos pos = area.getBlocksFound().get(i)
						.subtract(region.toBlockPos());
					
					matrixStack.push();
					matrixStack.translate(pos.getX(), pos.getY(), pos.getZ());
					matrixStack.translate(-0.005, -0.005, -0.005);
					matrixStack.scale(1.01F, 1.01F, 1.01F);
					
					RenderSystem.setShaderColor(0, 1, 0, 0.15F);
					RenderUtils.drawSolidBox(matrixStack);
					
					RenderSystem.setShaderColor(0, 0, 0, 0.5F);
					RenderUtils.drawOutlinedBox(matrixStack);
					
					matrixStack.pop();
				}
			
			matrixStack.push();
			matrixStack.translate(area.getMinX() + offset - region.x(),
				area.getMinY() + offset, area.getMinZ() + offset - region.z());
			matrixStack.scale(area.getSizeX() + scale, area.getSizeY() + scale,
				area.getSizeZ() + scale);
			
			// area scanner
			if(area.getProgress() < 1)
			{
				matrixStack.push();
				matrixStack.translate(area.getProgress(), 0, 0);
				matrixStack.scale(0, 1, 1);
				
				RenderSystem.setShaderColor(0, 1, 0, 0.3F);
				RenderUtils.drawSolidBox(matrixStack);
				
				RenderSystem.setShaderColor(0, 0, 0, 0.5F);
				RenderUtils.drawOutlinedBox(matrixStack);
				
				matrixStack.pop();
				
				// template scanner
			}else if(template != null && template.getProgress() > 0)
			{
				matrixStack.push();
				matrixStack.translate(template.getProgress(), 0, 0);
				matrixStack.scale(0, 1, 1);
				
				RenderSystem.setShaderColor(0, 1, 0, 0.3F);
				RenderUtils.drawSolidBox(matrixStack);
				
				RenderSystem.setShaderColor(0, 0, 0, 0.5F);
				RenderUtils.drawOutlinedBox(matrixStack);
				
				matrixStack.pop();
			}
			
			// area box
			RenderSystem.setShaderColor(0, 0, 0, 0.5F);
			RenderUtils.drawOutlinedBox(matrixStack);
			
			matrixStack.pop();
			
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		}
		
		// sorted blocks
		if(template != null)
			for(BlockPos pos : template.getSortedBlocks())
			{
				matrixStack.push();
				matrixStack.translate(pos.getX() - region.x(), pos.getY(),
					pos.getZ() - region.z());
				matrixStack.translate(offset, offset, offset);
				matrixStack.scale(scale, scale, scale);
				
				RenderSystem.setShaderColor(0F, 0F, 0F, 0.5F);
				RenderUtils.drawOutlinedBox(matrixStack);
				
				matrixStack.pop();
			}
		
		// selected positions
		for(Step step : Step.SELECT_POSITION_STEPS)
		{
			BlockPos pos = step.getPos();
			if(pos == null)
				continue;
			
			matrixStack.push();
			matrixStack.translate(pos.getX() - region.x(), pos.getY(),
				pos.getZ() - region.z());
			matrixStack.translate(offset, offset, offset);
			matrixStack.scale(scale, scale, scale);
			
			RenderSystem.setShaderColor(0, 1, 0, 0.15F);
			RenderUtils.drawSolidBox(matrixStack);
			
			RenderSystem.setShaderColor(0, 0, 0, 0.5F);
			RenderUtils.drawOutlinedBox(matrixStack);
			
			matrixStack.pop();
		}
		
		// posLookingAt
		if(posLookingAt != null)
		{
			matrixStack.push();
			matrixStack.translate(posLookingAt.getX() - region.x(),
				posLookingAt.getY(), posLookingAt.getZ() - region.z());
			matrixStack.translate(offset, offset, offset);
			matrixStack.scale(scale, scale, scale);
			
			RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 0.15F);
			RenderUtils.drawSolidBox(matrixStack);
			
			RenderSystem.setShaderColor(0, 0, 0, 0.5F);
			RenderUtils.drawOutlinedBox(matrixStack);
			
			matrixStack.pop();
		}
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	@Override
	public void onRenderGUI(DrawContext context, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		MatrixStack matrixStack = context.getMatrices();
		matrixStack.push();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		
		String message;
		if(step.doesSelectPos() && step.getPos() != null)
			message = "Press enter to confirm, or select a different position.";
		else if(step == Step.FILE_NAME && file != null && file.exists())
			message = "WARNING: This file already exists.";
		else
			message = step.getMessage();
		
		// translate to center
		Window sr = MC.getWindow();
		TextRenderer tr = MC.textRenderer;
		int msgWidth = tr.getWidth(message);
		matrixStack.translate(sr.getScaledWidth() / 2 - msgWidth / 2,
			sr.getScaledHeight() / 2 + 1, 0);
		
		// background
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		RenderSystem.setShaderColor(0, 0, 0, 0.5F);
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, 0, 0, 0);
		bufferBuilder.vertex(matrix, msgWidth + 2, 0, 0);
		bufferBuilder.vertex(matrix, msgWidth + 2, 10, 0);
		bufferBuilder.vertex(matrix, 0, 10, 0);
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
		
		// text
		RenderSystem.setShaderColor(1, 1, 1, 1);
		context.drawText(tr, message, 2, 1, 0xffffffff, false);
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	public void saveFile()
	{
		step = Step.values()[step.ordinal() + 1];
		JsonObject json = new JsonObject();
		
		// get facings
		Direction front = MC.player.getHorizontalFacing();
		Direction left = front.rotateYCounterclockwise();
		
		// add sorted blocks
		JsonArray jsonBlocks = new JsonArray();
		for(BlockPos pos : template.getSortedBlocks())
		{
			// translate
			pos = pos.subtract(Step.FIRST_BLOCK.getPos());
			
			// rotate
			pos = new BlockPos(0, pos.getY(), 0).offset(front, pos.getZ())
				.offset(left, pos.getX());
			
			// add to json
			jsonBlocks.add(JsonUtils.GSON.toJsonTree(
				new int[]{pos.getX(), pos.getY(), pos.getZ()}, int[].class));
		}
		json.add("blocks", jsonBlocks);
		
		try(PrintWriter save = new PrintWriter(new FileWriter(file)))
		{
			// save file
			save.print(JsonUtils.PRETTY_GSON.toJson(json));
			
			// show success message
			MutableText message = Text.literal("Saved template as ");
			ClickEvent event = new ClickEvent(ClickEvent.Action.OPEN_FILE,
				file.getParentFile().getAbsolutePath());
			MutableText link = Text.literal(file.getName())
				.styled(s -> s.withUnderline(true).withClickEvent(event));
			message.append(link);
			ChatUtils.component(message);
			
		}catch(IOException e)
		{
			e.printStackTrace();
			
			// show error message
			ChatUtils.error("File could not be saved.");
		}
		
		// disable TemplateTool
		setEnabled(false);
	}
	
	public void setFile(File file)
	{
		this.file = file;
	}
}
