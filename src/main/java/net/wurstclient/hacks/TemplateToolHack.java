/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
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
		int black = 0x80000000;
		int gray = 0x26404040;
		int green1 = 0x2600FF00;
		int green2 = 0x4D00FF00;
		
		// area
		if(area != null)
		{
			// recently scanned blocks
			if(step == Step.SCAN_AREA && area.getProgress() < 1)
			{
				ArrayList<Box> boxes = new ArrayList<>();
				for(int i = Math.max(0,
					area.getBlocksFound().size()
						- area.getScanSpeed()); i < area.getBlocksFound()
							.size(); i++)
					boxes.add(
						new Box(area.getBlocksFound().get(i)).expand(0.005));
				
				RenderUtils.drawOutlinedBoxes(matrixStack, boxes, black, true);
				RenderUtils.drawSolidBoxes(matrixStack, boxes, green1, true);
			}
			
			// area box
			Box areaBox = area.toBox();
			RenderUtils.drawOutlinedBox(matrixStack, areaBox, black, true);
			
			// area scanner
			if(area.getProgress() < 1)
			{
				double scannerX = MathHelper.lerp(area.getProgress(),
					areaBox.minX, areaBox.maxX);
				Box scanner = areaBox.withMinX(scannerX).withMaxX(scannerX);
				
				RenderUtils.drawOutlinedBox(matrixStack, scanner, black, true);
				RenderUtils.drawSolidBox(matrixStack, scanner, green2, true);
				
				// template scanner
			}else if(template != null && template.getProgress() > 0)
			{
				double scannerX = MathHelper.lerp(template.getProgress(),
					areaBox.minX, areaBox.maxX);
				Box scanner = areaBox.withMinX(scannerX).withMaxX(scannerX);
				
				RenderUtils.drawOutlinedBox(matrixStack, scanner, black, true);
				RenderUtils.drawSolidBox(matrixStack, scanner, green2, true);
			}
		}
		
		// sorted blocks
		if(template != null)
		{
			List<Box> boxes = template.getSortedBlocks().reversed().stream()
				.map(pos -> new Box(pos).contract(1 / 16.0)).limit(1024)
				.toList();
			RenderUtils.drawOutlinedBoxes(matrixStack, boxes, black, false);
		}
		
		// area preview
		if(area == null && step == Step.END_POS && step.getPos() != null)
		{
			Box preview =
				Box.enclosing(Step.START_POS.getPos(), Step.END_POS.getPos())
					.contract(1 / 16.0);
			RenderUtils.drawOutlinedBox(matrixStack, preview, black, true);
		}
		
		// selected positions
		ArrayList<Box> selectedBoxes = new ArrayList<>();
		for(Step step : Step.SELECT_POSITION_STEPS)
			if(step.getPos() != null)
				selectedBoxes.add(new Box(step.getPos()).contract(1 / 16.0));
		RenderUtils.drawOutlinedBoxes(matrixStack, selectedBoxes, black, false);
		RenderUtils.drawSolidBoxes(matrixStack, selectedBoxes, green1, false);
		
		// posLookingAt
		if(posLookingAt != null)
		{
			Box box = new Box(posLookingAt).contract(1 / 16.0);
			RenderUtils.drawOutlinedBox(matrixStack, box, black, false);
			RenderUtils.drawSolidBox(matrixStack, box, gray, false);
		}
	}
	
	@Override
	public void onRenderGUI(DrawContext context, float partialTicks)
	{
		String message;
		if(step.doesSelectPos() && step.getPos() != null)
			message = "Press enter to confirm, or select a different position.";
		else if(step == Step.FILE_NAME && file != null && file.exists())
			message = "WARNING: This file already exists.";
		else
			message = step.getMessage();
		
		TextRenderer tr = MC.textRenderer;
		int msgWidth = tr.getWidth(message);
		
		int msgX1 = context.getScaledWindowWidth() / 2 - msgWidth / 2;
		int msgX2 = msgX1 + msgWidth + 2;
		int msgY1 = context.getScaledWindowHeight() / 2 + 1;
		int msgY2 = msgY1 + 10;
		
		// background
		context.fill(msgX1, msgY1, msgX2, msgY2, 0x80000000);
		
		// text
		context.drawText(tr, message, msgX1 + 2, msgY1 + 1, Colors.WHITE,
			false);
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
			ClickEvent event =
				new ClickEvent.OpenFile(file.getParentFile().getAbsolutePath());
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
