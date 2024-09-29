/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathProcessor;
import net.wurstclient.commands.PathCmd;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class ExcavatorHack extends Hack
	implements UpdateListener, RenderListener, GUIRenderListener
{
	private Step step;
	private BlockPos posLookingAt;
	private Area area;
	private BlockPos currentBlock;
	private ExcavatorPathFinder pathFinder;
	private PathProcessor processor;
	
	private final SliderSetting range =
		new SliderSetting("Range", 5, 2, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Mode> mode =
		new EnumSetting<>("Mode", Mode.values(), Mode.FAST);
	
	public ExcavatorHack()
	{
		super("Excavator");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(mode);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName();
		
		if(step == Step.EXCAVATE && area != null)
		{
			int totalBlocks = area.blocksList.size();
			double brokenBlocks = totalBlocks - area.remainingBlocks;
			double progress = brokenBlocks / totalBlocks;
			int percentage = (int)(progress * 100);
			name += " " + percentage + "%";
		}
		
		return name;
	}
	
	@Override
	protected void onEnable()
	{
		// disable conflicting hacks
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().bowAimbotHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
		WURST.getHax().speedNukerHack.setEnabled(false);
		WURST.getHax().templateToolHack.setEnabled(false);
		WURST.getHax().tunnellerHack.setEnabled(false);
		WURST.getHax().veinMinerHack.setEnabled(false);
		
		step = Step.START_POS;
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(GUIRenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(GUIRenderListener.class, this);
		
		for(Step step : Step.values())
			step.pos = null;
		posLookingAt = null;
		area = null;
		
		MC.interactionManager.cancelBlockBreaking();
		currentBlock = null;
		
		pathFinder = null;
		processor = null;
		PathProcessor.releaseControls();
	}
	
	@Override
	public void onUpdate()
	{
		if(step.selectPos)
			handlePositionSelection();
		else if(step == Step.SCAN_AREA)
			scanArea();
		else if(step == Step.EXCAVATE)
			excavate();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(pathFinder != null)
		{
			PathCmd pathCmd = WURST.getCmds().pathCmd;
			pathFinder.renderPath(matrixStack, pathCmd.isDebugMode(),
				pathCmd.isDepthTest());
		}
		
		// scale and offset
		float scale = 7F / 8F;
		double offset = (1D - scale) / 2D;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		// area
		if(area != null)
		{
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			
			// recently scanned blocks
			if(step == Step.SCAN_AREA && area.progress < 1)
				for(int i = Math.max(0, area.blocksList.size()
					- area.scanSpeed); i < area.blocksList.size(); i++)
				{
					BlockPos pos =
						area.blocksList.get(i).subtract(region.toBlockPos());
					
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
			matrixStack.translate(area.minX + offset - region.x(),
				area.minY + offset, area.minZ + offset - region.z());
			matrixStack.scale(area.sizeX + scale, area.sizeY + scale,
				area.sizeZ + scale);
			
			// area scanner
			if(area.progress < 1)
			{
				matrixStack.push();
				matrixStack.translate(0, 0, area.progress);
				matrixStack.scale(1, 1, 0);
				
				RenderSystem.setShaderColor(0F, 1F, 0F, 0.3F);
				RenderUtils.drawSolidBox(matrixStack);
				
				RenderSystem.setShaderColor(0F, 0F, 0F, 0.5F);
				RenderUtils.drawOutlinedBox(matrixStack);
				
				matrixStack.pop();
			}
			
			// area box
			RenderSystem.setShaderColor(0F, 0F, 0F, 0.5F);
			RenderUtils.drawOutlinedBox(matrixStack);
			
			matrixStack.pop();
			
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		}
		
		// selected positions
		for(Step step : Step.SELECT_POSITION_STEPS)
		{
			BlockPos pos = step.pos;
			if(pos == null)
				continue;
			
			matrixStack.push();
			matrixStack.translate(pos.getX() - region.x(), pos.getY(),
				pos.getZ() - region.z());
			matrixStack.translate(offset, offset, offset);
			matrixStack.scale(scale, scale, scale);
			
			RenderSystem.setShaderColor(0F, 1F, 0F, 0.15F);
			RenderUtils.drawSolidBox(matrixStack);
			
			RenderSystem.setShaderColor(0F, 0F, 0F, 0.5F);
			RenderUtils.drawOutlinedBox(matrixStack);
			
			matrixStack.pop();
		}
		
		// area preview
		if(area == null && step == Step.END_POS && step.pos != null)
		{
			Area preview = new Area(Step.START_POS.pos, Step.END_POS.pos);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			
			// area box
			matrixStack.push();
			matrixStack.translate(preview.minX + offset - region.x(),
				preview.minY + offset, preview.minZ + offset - region.z());
			matrixStack.scale(preview.sizeX + scale, preview.sizeY + scale,
				preview.sizeZ + scale);
			RenderSystem.setShaderColor(0F, 0F, 0F, 0.5F);
			RenderUtils.drawOutlinedBox(matrixStack);
			matrixStack.pop();
			
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		}
		
		// posLookingAt
		if(posLookingAt != null)
		{
			matrixStack.push();
			matrixStack.translate(posLookingAt.getX() - region.x(),
				posLookingAt.getY(), posLookingAt.getZ() - region.z());
			matrixStack.translate(offset, offset, offset);
			matrixStack.scale(scale, scale, scale);
			
			RenderSystem.setShader(GameRenderer::getPositionProgram);
			RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 0.15F);
			RenderUtils.drawSolidBox(matrixStack);
			
			RenderSystem.setShaderColor(0F, 0F, 0F, 0.5F);
			RenderUtils.drawOutlinedBox(matrixStack);
			
			matrixStack.pop();
		}
		
		// currentBlock
		if(currentBlock != null)
		{
			// set position
			matrixStack.translate(currentBlock.getX() - region.x(),
				currentBlock.getY(), currentBlock.getZ() - region.z());
			
			// get progress
			float progress;
			if(BlockUtils.getHardness(currentBlock) < 1)
				progress = MC.interactionManager.currentBreakingProgress;
			else
				progress = 1;
			
			// set size
			if(progress < 1)
			{
				matrixStack.translate(0.5, 0.5, 0.5);
				matrixStack.scale(progress, progress, progress);
				matrixStack.translate(-0.5, -0.5, -0.5);
			}
			
			// get color
			float red = progress * 2F;
			float green = 2 - red;
			
			// draw box
			RenderSystem.setShaderColor(red, green, 0, 0.25F);
			RenderUtils.drawSolidBox(matrixStack);
			RenderSystem.setShaderColor(red, green, 0, 0.5F);
			RenderUtils.drawOutlinedBox(matrixStack);
		}
		
		matrixStack.pop();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		RenderSystem.setShaderColor(1, 1, 1, 1);
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
		if(step.selectPos && step.pos != null)
			message = "Press enter to confirm, or select a different position.";
		else
			message = step.message;
		
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
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
	
	public void enableWithArea(BlockPos pos1, BlockPos pos2)
	{
		setEnabled(true);
		Step.START_POS.pos = pos1;
		Step.END_POS.pos = pos2;
		step = Step.SCAN_AREA;
	}
	
	private void handlePositionSelection()
	{
		// continue with next step
		if(step.pos != null && InputUtil
			.isKeyPressed(MC.getWindow().getHandle(), GLFW.GLFW_KEY_ENTER))
		{
			step = Step.values()[step.ordinal() + 1];
			
			// delete posLookingAt
			if(!step.selectPos)
				posLookingAt = null;
			
			return;
		}
		
		if(MC.crosshairTarget instanceof BlockHitResult)
		{
			// set posLookingAt
			posLookingAt = ((BlockHitResult)MC.crosshairTarget).getBlockPos();
			
			// offset if sneaking
			if(MC.options.sneakKey.isPressed())
				posLookingAt = posLookingAt
					.offset(((BlockHitResult)MC.crosshairTarget).getSide());
			
		}else
			posLookingAt = null;
		
		// set selected position
		if(posLookingAt != null && MC.options.useKey.isPressed())
			step.pos = posLookingAt;
	}
	
	private void scanArea()
	{
		// initialize area
		if(area == null)
		{
			area = new Area(Step.START_POS.pos, Step.END_POS.pos);
			Step.START_POS.pos = null;
			Step.END_POS.pos = null;
		}
		
		// scan area
		for(int i = 0; i < area.scanSpeed && area.iterator.hasNext(); i++)
		{
			area.scannedBlocks++;
			BlockPos pos = area.iterator.next();
			
			if(BlockUtils.canBeClicked(pos))
			{
				area.blocksList.add(pos);
				area.blocksSet.add(pos);
			}
		}
		
		// update progress
		area.progress = (float)area.scannedBlocks / (float)area.totalBlocks;
		
		// continue with next step
		if(!area.iterator.hasNext())
		{
			area.remainingBlocks = area.blocksList.size();
			step = Step.values()[step.ordinal() + 1];
		}
	}
	
	private void excavate()
	{
		// wait for AutoEat to finish eating
		if(WURST.getHax().autoEatHack.isEating())
			return;
		
		// prioritize the closest block from the top layer
		Vec3d eyesVec = RotationUtils.getEyesPos();
		Comparator<BlockPos> cNextTargetBlock =
			Comparator.comparingInt(BlockPos::getY).reversed()
				.thenComparingDouble(pos -> pos.getSquaredDistance(eyesVec));
		
		// get valid blocks
		ArrayList<BlockPos> validBlocks = getValidBlocks();
		validBlocks.sort(cNextTargetBlock);
		currentBlock = null;
		
		// nuke all
		boolean legit = mode.getSelected() == Mode.LEGIT;
		if(MC.player.getAbilities().creativeMode && !legit)
		{
			MC.interactionManager.cancelBlockBreaking();
			
			// set closest block as current
			for(BlockPos pos : validBlocks)
			{
				currentBlock = pos;
				break;
			}
			
			// break all blocks
			BlockBreaker.breakBlocksWithPacketSpam(validBlocks);
			
		}else
		{
			// break next block
			for(BlockPos pos : validBlocks)
			{
				WURST.getHax().autoToolHack.equipIfEnabled(pos);
				if(!BlockBreaker.breakOneBlock(pos))
					continue;
				
				currentBlock = pos;
				break;
			}
			
			// reset if no block was found
			if(currentBlock == null)
				MC.interactionManager.cancelBlockBreaking();
		}
		
		// get remaining blocks
		Predicate<BlockPos> pBreakable = MC.player.isCreative()
			? BlockUtils::canBeClicked : pos -> BlockUtils.canBeClicked(pos)
				&& !BlockUtils.isUnbreakable(pos);
		area.remainingBlocks =
			(int)area.blocksList.parallelStream().filter(pBreakable).count();
		
		if(area.remainingBlocks == 0)
		{
			setEnabled(false);
			return;
		}
		
		if(pathFinder == null)
		{
			BlockPos closestBlock = area.blocksList.parallelStream()
				.filter(pBreakable).min(cNextTargetBlock).get();
			
			pathFinder = new ExcavatorPathFinder(closestBlock);
		}
		
		// find path
		if(!pathFinder.isDone() && !pathFinder.isFailed())
		{
			PathProcessor.lockControls();
			
			pathFinder.think();
			
			if(!pathFinder.isDone() && !pathFinder.isFailed())
				return;
			
			pathFinder.formatPath();
			
			// set processor
			processor = pathFinder.getProcessor();
		}
		
		// check path
		if(processor != null
			&& !pathFinder.isPathStillValid(processor.getIndex()))
		{
			pathFinder = new ExcavatorPathFinder(pathFinder);
			return;
		}
		
		// process path
		processor.process();
		
		if(processor.isDone())
		{
			pathFinder = null;
			processor = null;
			PathProcessor.releaseControls();
		}
	}
	
	private ArrayList<BlockPos> getValidBlocks()
	{
		Vec3d eyesVec = RotationUtils.getEyesPos();
		BlockPos eyesBlock = BlockPos.ofFloored(eyesVec);
		double rangeSq = Math.pow(range.getValue() + 0.5, 2);
		int blockRange = range.getValueCeil();
		
		return BlockUtils.getAllInBoxStream(eyesBlock, blockRange)
			.filter(pos -> pos.getSquaredDistance(eyesVec) <= rangeSq)
			.filter(area.blocksSet::contains).filter(BlockUtils::canBeClicked)
			.filter(pos -> !BlockUtils.isUnbreakable(pos))
			.sorted(Comparator
				.comparingDouble(pos -> pos.getSquaredDistance(eyesVec)))
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private static enum Mode
	{
		FAST("Fast"),
		
		LEGIT("Legit");
		
		private final String name;
		
		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	private static enum Step
	{
		START_POS("Select start position.", true),
		
		END_POS("Select end position.", true),
		
		SCAN_AREA("Scanning area...", false),
		
		EXCAVATE("Excavating...", false);
		
		private static final Step[] SELECT_POSITION_STEPS =
			{START_POS, END_POS};
		
		private final String message;
		private boolean selectPos;
		
		private BlockPos pos;
		
		private Step(String message, boolean selectPos)
		{
			this.message = message;
			this.selectPos = selectPos;
		}
	}
	
	private static class Area
	{
		private final int minX, minY, minZ;
		private final int sizeX, sizeY, sizeZ;
		
		private final int totalBlocks, scanSpeed;
		private final Iterator<BlockPos> iterator;
		
		private int scannedBlocks, remainingBlocks;
		private float progress;
		
		private final ArrayList<BlockPos> blocksList = new ArrayList<>();
		private final HashSet<BlockPos> blocksSet = new HashSet<>();
		
		private Area(BlockPos start, BlockPos end)
		{
			int startX = start.getX();
			int startY = start.getY();
			int startZ = start.getZ();
			
			int endX = end.getX();
			int endY = end.getY();
			int endZ = end.getZ();
			
			minX = Math.min(startX, endX);
			minY = Math.min(startY, endY);
			minZ = Math.min(startZ, endZ);
			
			sizeX = Math.abs(startX - endX);
			sizeY = Math.abs(startY - endY);
			sizeZ = Math.abs(startZ - endZ);
			
			totalBlocks = (sizeX + 1) * (sizeY + 1) * (sizeZ + 1);
			scanSpeed = MathHelper.clamp(totalBlocks / 30, 1, 16384);
			iterator = BlockUtils.getAllInBox(start, end).iterator();
		}
	}
	
	private static class ExcavatorPathFinder extends PathFinder
	{
		public ExcavatorPathFinder(BlockPos goal)
		{
			super(goal);
			setThinkTime(10);
		}
		
		public ExcavatorPathFinder(ExcavatorPathFinder pathFinder)
		{
			super(pathFinder);
		}
		
		@Override
		protected boolean checkDone()
		{
			BlockPos goal = getGoal();
			
			return done = goal.down(2).equals(current)
				|| goal.up().equals(current) || goal.north().equals(current)
				|| goal.south().equals(current) || goal.west().equals(current)
				|| goal.east().equals(current)
				|| goal.down().north().equals(current)
				|| goal.down().south().equals(current)
				|| goal.down().west().equals(current)
				|| goal.down().east().equals(current);
		}
	}
}
