/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Colors;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
import net.wurstclient.util.OverlayRenderer;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class ExcavatorHack extends Hack
	implements UpdateListener, RenderListener, GUIRenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 2, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Mode> mode =
		new EnumSetting<>("Mode", Mode.values(), Mode.FAST);
	
	private final OverlayRenderer overlay = new OverlayRenderer();
	
	private Step step;
	private BlockPos posLookingAt;
	private Area area;
	private BlockPos currentBlock;
	private ExcavatorPathFinder pathFinder;
	private PathProcessor processor;
	
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
		overlay.resetProgress();
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
		
		int black = 0x80000000;
		int gray = 0x26404040;
		int green1 = 0x2600FF00;
		int green2 = 0x4D00FF00;
		
		// area
		if(area != null)
		{
			// recently scanned blocks
			if(step == Step.SCAN_AREA && area.progress < 1)
			{
				ArrayList<Box> boxes = new ArrayList<>();
				for(int i = Math.max(0, area.blocksList.size()
					- area.scanSpeed); i < area.blocksList.size(); i++)
					boxes.add(new Box(area.blocksList.get(i)).expand(0.005));
				
				RenderUtils.drawOutlinedBoxes(matrixStack, boxes, black, true);
				RenderUtils.drawSolidBoxes(matrixStack, boxes, green1, true);
			}
			
			// area box
			Box areaBox =
				new Box(area.minX, area.minY, area.minZ, area.minX + area.sizeX,
					area.minY + area.sizeY, area.minZ + area.sizeZ)
						.contract(1 / 16.0);
			RenderUtils.drawOutlinedBox(matrixStack, areaBox, black, true);
			
			// area scanner
			if(area.progress < 1)
			{
				double scannerX =
					MathHelper.lerp(area.progress, areaBox.minX, areaBox.maxX);
				Box scanner = areaBox.withMinX(scannerX).withMaxX(scannerX);
				
				RenderUtils.drawOutlinedBox(matrixStack, scanner, black, true);
				RenderUtils.drawSolidBox(matrixStack, scanner, green2, true);
			}
		}
		
		// area preview
		if(area == null && step == Step.END_POS && step.pos != null)
		{
			Box preview = Box.enclosing(Step.START_POS.pos, Step.END_POS.pos)
				.contract(1 / 16.0);
			RenderUtils.drawOutlinedBox(matrixStack, preview, black, true);
		}
		
		// selected positions
		ArrayList<Box> selectedBoxes = new ArrayList<>();
		for(Step step : Step.SELECT_POSITION_STEPS)
			if(step.pos != null)
				selectedBoxes.add(new Box(step.pos).contract(1 / 16.0));
		RenderUtils.drawOutlinedBoxes(matrixStack, selectedBoxes, black, false);
		RenderUtils.drawSolidBoxes(matrixStack, selectedBoxes, green1, false);
		
		// posLookingAt
		if(posLookingAt != null)
		{
			Box box = new Box(posLookingAt).contract(1 / 16.0);
			RenderUtils.drawOutlinedBox(matrixStack, box, black, false);
			RenderUtils.drawSolidBox(matrixStack, box, gray, false);
		}
		
		overlay.render(matrixStack, partialTicks, currentBlock);
	}
	
	@Override
	public void onRenderGUI(DrawContext context, float partialTicks)
	{
		String message;
		if(step.selectPos && step.pos != null)
			message = "Press enter to confirm, or select a different position.";
		else
			message = step.message;
		
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
			overlay.resetProgress();
			
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
			{
				MC.interactionManager.cancelBlockBreaking();
				overlay.resetProgress();
			}
		}
		
		overlay.updateProgress();
		
		// get remaining blocks
		Predicate<BlockPos> pBreakable = MC.player.getAbilities().creativeMode
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
