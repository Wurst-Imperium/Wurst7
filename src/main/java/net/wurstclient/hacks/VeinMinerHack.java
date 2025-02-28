/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.nukers.NukerMultiIdListSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockBreaker.BlockBreakingParams;
import net.wurstclient.util.BlockBreakingCache;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.OverlayRenderer;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class VeinMinerHack extends Hack
	implements UpdateListener, LeftClickListener, RenderListener
{
	private static final Box BLOCK_BOX =
		new Box(1 / 16.0, 1 / 16.0, 1 / 16.0, 15 / 16.0, 15 / 16.0, 15 / 16.0);
	
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting flat = new CheckboxSetting("Flat mode",
		"Won't break any blocks below your feet.", false);
	
	private final NukerMultiIdListSetting multiIdList =
		new NukerMultiIdListSetting("The types of blocks to mine as veins.");
	
	private final SwingHandSetting swingHand = new SwingHandSetting(
		SwingHandSetting.genericMiningDescription(this), SwingHand.SERVER);
	
	private final BlockBreakingCache cache = new BlockBreakingCache();
	private final OverlayRenderer overlay = new OverlayRenderer();
	private final HashSet<BlockPos> currentVein = new HashSet<>();
	private BlockPos currentBlock;
	
	private final SliderSetting maxVeinSize = new SliderSetting("Max vein size",
		"Maximum number of blocks to mine in a single vein.", 64, 1, 1000, 1,
		ValueDisplay.INTEGER);
	
	private final CheckboxSetting checkLOS = new CheckboxSetting(
		"Check line of sight",
		"Makes sure that you don't reach through walls when breaking blocks.",
		false);
	
	public VeinMinerHack()
	{
		super("VeinMiner");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(flat);
		addSetting(multiIdList);
		addSetting(swingHand);
		addSetting(maxVeinSize);
		addSetting(checkLOS);
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
		WURST.getHax().speedNukerHack.setEnabled(false);
		WURST.getHax().tunnellerHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(LeftClickListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(LeftClickListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		currentVein.clear();
		if(currentBlock != null)
		{
			MC.interactionManager.breakingBlock = true;
			MC.interactionManager.cancelBlockBreaking();
			currentBlock = null;
		}
		
		cache.reset();
		overlay.resetProgress();
	}
	
	@Override
	public void onUpdate()
	{
		currentBlock = null;
		currentVein.removeIf(pos -> BlockUtils.getState(pos).isReplaceable());
		
		if(MC.options.attackKey.isPressed())
			return;
		
		Vec3d eyesVec = RotationUtils.getEyesPos();
		BlockPos eyesBlock = BlockPos.ofFloored(eyesVec);
		double rangeSq = range.getValueSq();
		int blockRange = range.getValueCeil();
		
		Stream<BlockBreakingParams> stream = BlockUtils
			.getAllInBoxStream(eyesBlock, blockRange)
			.filter(this::shouldBreakBlock)
			.map(BlockBreaker::getBlockBreakingParams).filter(Objects::nonNull)
			.filter(params -> params.distanceSq() <= rangeSq);
		
		if(checkLOS.isChecked())
			stream = stream.filter(BlockBreakingParams::lineOfSight);
		
		stream = stream.sorted(BlockBreaker.comparingParams());
		
		// Break all blocks in creative mode
		if(MC.player.getAbilities().creativeMode)
		{
			MC.interactionManager.cancelBlockBreaking();
			overlay.resetProgress();
			
			ArrayList<BlockPos> blocks = cache
				.filterOutRecentBlocks(stream.map(BlockBreakingParams::pos));
			if(blocks.isEmpty())
				return;
			
			currentBlock = blocks.get(0);
			BlockBreaker.breakBlocksWithPacketSpam(blocks);
			swingHand.swing(Hand.MAIN_HAND);
			return;
		}
		
		// Break the first valid block in survival mode
		currentBlock = stream.filter(this::breakOneBlock)
			.map(BlockBreakingParams::pos).findFirst().orElse(null);
		
		if(currentBlock == null)
		{
			MC.interactionManager.cancelBlockBreaking();
			overlay.resetProgress();
			return;
		}
		
		overlay.updateProgress();
	}
	
	private boolean shouldBreakBlock(BlockPos pos)
	{
		if(flat.isChecked() && pos.getY() < MC.player.getY())
			return false;
		
		return currentVein.contains(pos);
	}
	
	private boolean breakOneBlock(BlockBreakingParams params)
	{
		WURST.getRotationFaker().faceVectorPacket(params.hitVec());
		
		if(!MC.interactionManager.updateBlockBreakingProgress(params.pos(),
			params.side()))
			return false;
		
		swingHand.swing(Hand.MAIN_HAND);
		return true;
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(!currentVein.isEmpty())
			return;
		
		if(!(MC.crosshairTarget instanceof BlockHitResult bHitResult)
			|| bHitResult.getType() != HitResult.Type.BLOCK)
			return;
		
		if(!multiIdList.contains(BlockUtils.getBlock(bHitResult.getBlockPos())))
			return;
		
		buildVein(bHitResult.getBlockPos());
	}
	
	private void buildVein(BlockPos pos)
	{
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Block targetBlock = BlockUtils.getBlock(pos);
		int maxSize = maxVeinSize.getValueI();
		
		queue.offer(pos);
		currentVein.add(pos);
		
		while(!queue.isEmpty() && currentVein.size() < maxSize)
		{
			BlockPos current = queue.poll();
			
			for(Direction direction : Direction.values())
			{
				BlockPos neighbor = current.offset(direction);
				if(!currentVein.contains(neighbor)
					&& BlockUtils.getBlock(neighbor) == targetBlock)
				{
					queue.offer(neighbor);
					currentVein.add(neighbor);
				}
			}
		}
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		overlay.render(matrixStack, partialTicks, currentBlock);
		if(currentVein.isEmpty())
			return;
		
		List<Box> boxes =
			currentVein.stream().map(pos -> BLOCK_BOX.offset(pos)).toList();
		RenderUtils.drawOutlinedBoxes(matrixStack, boxes, 0x80000000, false);
	}
}
