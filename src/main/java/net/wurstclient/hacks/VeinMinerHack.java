/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
	private static final AABB BLOCK_BOX =
		new AABB(1 / 16.0, 1 / 16.0, 1 / 16.0, 15 / 16.0, 15 / 16.0, 15 / 16.0);
	
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
			MC.gameMode.isDestroying = true;
			MC.gameMode.stopDestroyBlock();
			currentBlock = null;
		}
		
		cache.reset();
		overlay.resetProgress();
	}
	
	@Override
	public void onUpdate()
	{
		currentBlock = null;
		currentVein.removeIf(pos -> BlockUtils.getState(pos).canBeReplaced());
		
		if(MC.options.keyAttack.isDown())
			return;
		
		Vec3 eyesVec = RotationUtils.getEyesPos();
		BlockPos eyesBlock = BlockPos.containing(eyesVec);
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
		if(MC.player.getAbilities().instabuild)
		{
			MC.gameMode.stopDestroyBlock();
			overlay.resetProgress();
			
			ArrayList<BlockPos> blocks = cache
				.filterOutRecentBlocks(stream.map(BlockBreakingParams::pos));
			if(blocks.isEmpty())
				return;
			
			currentBlock = blocks.get(0);
			BlockBreaker.breakBlocksWithPacketSpam(blocks);
			swingHand.swing(InteractionHand.MAIN_HAND);
			return;
		}
		
		// Break the first valid block in survival mode
		currentBlock = stream.filter(this::breakOneBlock)
			.map(BlockBreakingParams::pos).findFirst().orElse(null);
		
		if(currentBlock == null)
		{
			MC.gameMode.stopDestroyBlock();
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
		
		if(!MC.gameMode.continueDestroyBlock(params.pos(), params.side()))
			return false;
		
		swingHand.swing(InteractionHand.MAIN_HAND);
		return true;
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(!currentVein.isEmpty())
			return;
		
		if(!(MC.hitResult instanceof BlockHitResult bHitResult)
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
				BlockPos neighbor = current.relative(direction);
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
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		overlay.render(matrixStack, partialTicks, currentBlock);
		if(currentVein.isEmpty())
			return;
		
		List<AABB> boxes =
			currentVein.stream().map(pos -> BLOCK_BOX.move(pos)).toList();
		RenderUtils.drawOutlinedBoxes(matrixStack, boxes, 0x80000000, false);
	}
}
