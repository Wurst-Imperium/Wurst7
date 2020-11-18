/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
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
		super("Excavator",
			"Automatically breaks all blocks in the selected area.");
		
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(mode);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName();
		
		if(step == Step.EXCAVATE && area != null)
			name += " "
				+ (int)((float)(area.blocksList.size() - area.remainingBlocks)
					/ (float)area.blocksList.size() * 100)
				+ "%";
		
		return name;
	}
	
	@Override
	public void onEnable()
	{
		// disable conflicting hacks
		// TODO:
		// WURST.getHax().bowAimbotMod.setEnabled(false);
		// WURST.getHax().templateToolMod.setEnabled(false);
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
		WURST.getHax().speedNukerHack.setEnabled(false);
		WURST.getHax().tunnellerHack.setEnabled(false);
		
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
	public void onRender(float partialTicks)
	{
		if(pathFinder != null)
		{
			PathCmd pathCmd = WURST.getCmds().pathCmd;
			pathFinder.renderPath(pathCmd.isDebugMode(), pathCmd.isDepthTest());
		}
		
		// scale and offset
		double scale = 7D / 8D;
		double offset = (1D - scale) / 2D;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		// area
		if(area != null)
		{
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			
			// recently scanned blocks
			if(step == Step.SCAN_AREA && area.progress < 1)
				for(int i = Math.max(0, area.blocksList.size()
					- area.scanSpeed); i < area.blocksList.size(); i++)
				{
					BlockPos pos = area.blocksList.get(i);
					
					GL11.glPushMatrix();
					GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
					GL11.glTranslated(-0.005, -0.005, -0.005);
					GL11.glScaled(1.01, 1.01, 1.01);
					
					GL11.glColor4f(0F, 1F, 0F, 0.15F);
					RenderUtils.drawSolidBox();
					
					GL11.glColor4f(0F, 0F, 0F, 0.5F);
					RenderUtils.drawOutlinedBox();
					
					GL11.glPopMatrix();
				}
			
			GL11.glPushMatrix();
			GL11.glTranslated(area.minX + offset, area.minY + offset,
				area.minZ + offset);
			GL11.glScaled(area.sizeX + scale, area.sizeY + scale,
				area.sizeZ + scale);
			
			// area scanner
			if(area.progress < 1)
			{
				GL11.glPushMatrix();
				GL11.glTranslated(0, 0, area.progress);
				GL11.glScaled(1, 1, 0);
				
				GL11.glColor4f(0F, 1F, 0F, 0.3F);
				RenderUtils.drawSolidBox();
				
				GL11.glColor4f(0F, 0F, 0F, 0.5F);
				RenderUtils.drawOutlinedBox();
				
				GL11.glPopMatrix();
			}
			
			// area box
			GL11.glColor4f(0F, 0F, 0F, 0.5F);
			RenderUtils.drawOutlinedBox();
			
			GL11.glPopMatrix();
			
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		}
		
		// selected positions
		for(Step step : Step.SELECT_POSITION_STEPS)
		{
			BlockPos pos = step.pos;
			if(pos == null)
				continue;
			
			GL11.glPushMatrix();
			GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
			GL11.glTranslated(offset, offset, offset);
			GL11.glScaled(scale, scale, scale);
			
			GL11.glColor4f(0F, 1F, 0F, 0.15F);
			RenderUtils.drawSolidBox();
			
			GL11.glColor4f(0F, 0F, 0F, 0.5F);
			RenderUtils.drawOutlinedBox();
			
			GL11.glPopMatrix();
		}
		
		// area preview
		if(area == null && step == Step.END_POS && step.pos != null)
		{
			Area preview = new Area(Step.START_POS.pos, Step.END_POS.pos);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			
			// area box
			GL11.glPushMatrix();
			GL11.glTranslated(preview.minX + offset, preview.minY + offset,
				preview.minZ + offset);
			GL11.glScaled(preview.sizeX + scale, preview.sizeY + scale,
				preview.sizeZ + scale);
			GL11.glColor4f(0F, 0F, 0F, 0.5F);
			RenderUtils.drawOutlinedBox();
			GL11.glPopMatrix();
			
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		}
		
		// posLookingAt
		if(posLookingAt != null)
		{
			GL11.glPushMatrix();
			GL11.glTranslated(posLookingAt.getX(), posLookingAt.getY(),
				posLookingAt.getZ());
			GL11.glTranslated(offset, offset, offset);
			GL11.glScaled(scale, scale, scale);
			
			GL11.glColor4f(0.25F, 0.25F, 0.25F, 0.15F);
			RenderUtils.drawSolidBox();
			
			GL11.glColor4f(0F, 0F, 0F, 0.5F);
			RenderUtils.drawOutlinedBox();
			
			GL11.glPopMatrix();
		}
		
		// currentBlock
		if(currentBlock != null)
		{
			// set position
			GL11.glTranslated(currentBlock.getX(), currentBlock.getY(),
				currentBlock.getZ());
			
			// get progress
			float progress;
			if(BlockUtils.getHardness(currentBlock) < 1)
				progress =
					IMC.getInteractionManager().getCurrentBreakingProgress();
			else
				progress = 1;
			
			// set size
			if(progress < 1)
			{
				GL11.glTranslated(0.5, 0.5, 0.5);
				GL11.glScaled(progress, progress, progress);
				GL11.glTranslated(-0.5, -0.5, -0.5);
			}
			
			// get color
			float red = progress * 2F;
			float green = 2 - red;
			
			// draw box
			GL11.glColor4f(red, green, 0, 0.25F);
			RenderUtils.drawSolidBox();
			GL11.glColor4f(red, green, 0, 0.5F);
			RenderUtils.drawOutlinedBox();
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	@Override
	public void onRenderGUI(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glPushMatrix();
		
		String message;
		if(step.selectPos && step.pos != null)
			message = "Press enter to confirm, or select a different position.";
		else
			message = step.message;
		
		TextRenderer tr = MC.textRenderer;
		
		// translate to center
		Window sr = MC.getWindow();
		int msgWidth = tr.getWidth(message);
		GL11.glTranslated(sr.getScaledWidth() / 2 - msgWidth / 2,
			sr.getScaledHeight() / 2 + 1, 0);
		
		// background
		GL11.glColor4f(0, 0, 0, 0.5F);
		GL11.glBegin(GL11.GL_QUADS);
		{
			GL11.glVertex2d(0, 0);
			GL11.glVertex2d(msgWidth + 2, 0);
			GL11.glVertex2d(msgWidth + 2, 10);
			GL11.glVertex2d(0, 10);
		}
		GL11.glEnd();
		
		// text
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		tr.draw(matrixStack, message, 2, 1, 0xffffffff);
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
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
		
		if(MC.crosshairTarget != null
			&& MC.crosshairTarget instanceof BlockHitResult)
		{
			// set posLookingAt
			posLookingAt = ((BlockHitResult)MC.crosshairTarget).getBlockPos();
			
			// offset if sneaking
			if(MC.options.keySneak.isPressed())
				posLookingAt = posLookingAt
					.offset(((BlockHitResult)MC.crosshairTarget).getSide());
			
		}else
			posLookingAt = null;
		
		// set selected position
		if(posLookingAt != null && MC.options.keyUse.isPressed())
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
		boolean legit = mode.getSelected() == Mode.LEGIT;
		currentBlock = null;
		
		// get valid blocks
		Iterable<BlockPos> validBlocks = getValidBlocks(range.getValue(),
			pos -> area.blocksSet.contains(pos));
		
		// nuke all
		if(MC.player.abilities.creativeMode && !legit)
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
			ArrayList<BlockPos> blocks = new ArrayList<>();
			for(BlockPos pos : validBlocks)
				blocks.add(pos);
			blocks.sort(Comparator.comparingInt((BlockPos pos) -> -pos.getY()));
			
			// find closest valid block
			for(BlockPos pos : blocks)
			{
				boolean successful;
				
				// break block
				successful = BlockBreaker.breakOneBlock(pos);
				
				// set currentBlock if successful
				if(successful)
				{
					currentBlock = pos;
					break;
				}
			}
			
			// reset if no block was found
			if(currentBlock == null)
				MC.interactionManager.cancelBlockBreaking();
		}
		
		// get remaining blocks
		Predicate<BlockPos> pClickable = pos -> BlockUtils.canBeClicked(pos);
		area.remainingBlocks =
			(int)area.blocksList.parallelStream().filter(pClickable).count();
		
		if(area.remainingBlocks == 0)
		{
			setEnabled(false);
			return;
		}
		
		if(pathFinder == null)
		{
			Comparator<BlockPos> cDistance = Comparator.comparingDouble(
				pos -> MC.player.squaredDistanceTo(Vec3d.ofCenter(pos)));
			Comparator<BlockPos> cAltitude =
				Comparator.comparingInt(pos -> -pos.getY());
			BlockPos closestBlock =
				area.blocksList.parallelStream().filter(pClickable)
					.min(cAltitude.thenComparing(cDistance)).get();
			
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
	
	private ArrayList<BlockPos> getValidBlocks(double range,
		Predicate<BlockPos> validator)
	{
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double rangeSq = Math.pow(range + 0.5, 2);
		int rangeI = (int)Math.ceil(range);
		
		BlockPos center = new BlockPos(RotationUtils.getEyesPos());
		BlockPos min = center.add(-rangeI, -rangeI, -rangeI);
		BlockPos max = center.add(rangeI, rangeI, rangeI);
		
		return BlockUtils.getAllInBox(min, max).stream()
			.filter(pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(BlockUtils::canBeClicked).filter(validator)
			.sorted(Comparator.comparingDouble(
				pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos))))
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
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
