/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.autofarm.AutoFarmRenderer;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockPlacer;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.OverlayRenderer;
import net.wurstclient.util.RotationUtils;

@SearchTags({"auto farm", "AutoHarvest", "auto harvest"})
public final class AutoFarmHack extends Hack
	implements UpdateListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting replant =
		new CheckboxSetting("Replant", true);
	
	private final HashMap<Block, Item> seeds = new HashMap<>();
	{
		seeds.put(Blocks.WHEAT, Items.WHEAT_SEEDS);
		seeds.put(Blocks.CARROTS, Items.CARROT);
		seeds.put(Blocks.POTATOES, Items.POTATO);
		seeds.put(Blocks.BEETROOTS, Items.BEETROOT_SEEDS);
		seeds.put(Blocks.PUMPKIN_STEM, Items.PUMPKIN_SEEDS);
		seeds.put(Blocks.MELON_STEM, Items.MELON_SEEDS);
		seeds.put(Blocks.NETHER_WART, Items.NETHER_WART);
		seeds.put(Blocks.COCOA, Items.COCOA_BEANS);
	}
	
	private final HashMap<BlockPos, Item> plants = new HashMap<>();
	private final ArrayDeque<Set<BlockPos>> prevBlocks = new ArrayDeque<>();
	private BlockPos currentBlock;
	
	private final AutoFarmRenderer renderer = new AutoFarmRenderer();
	private final OverlayRenderer overlay = new OverlayRenderer();
	
	private boolean busy;
	
	public AutoFarmHack()
	{
		super("AutoFarm");
		
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(replant);
	}
	
	@Override
	public void onEnable()
	{
		plants.clear();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		if(currentBlock != null)
		{
			IMC.getInteractionManager().setBreakingBlock(true);
			MC.interactionManager.cancelBlockBreaking();
			currentBlock = null;
		}
		
		prevBlocks.clear();
		overlay.resetProgress();
		busy = false;
		
		renderer.reset();
	}
	
	@Override
	public void onUpdate()
	{
		currentBlock = null;
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		BlockPos eyesBlock = BlockPos.ofFloored(RotationUtils.getEyesPos());
		double rangeSq = range.getValueSq();
		int blockRange = range.getValueCeil();
		
		List<BlockPos> blocks = BlockUtils
			.getAllInBoxStream(eyesBlock, blockRange)
			.filter(pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(BlockUtils::canBeClicked).collect(Collectors.toList());
		
		updatePlants(blocks);
		
		List<BlockPos> blocksToHarvest = new ArrayList<>();
		List<BlockPos> blocksToReplant = new ArrayList<>();
		
		if(!WURST.getHax().freecamHack.isEnabled())
		{
			blocksToHarvest = getBlocksToHarvest(eyesVec, blocks);
			
			if(replant.isChecked())
				blocksToReplant =
					getBlocksToReplant(eyesVec, eyesBlock, rangeSq, blockRange);
		}
		
		boolean replanting = false;
		while(!blocksToReplant.isEmpty())
		{
			BlockPos pos = blocksToReplant.get(0);
			Item neededItem = plants.get(pos);
			if(tryToReplant(pos, neededItem))
			{
				replanting = true;
				break;
			}
			
			blocksToReplant.removeIf(p -> plants.get(p) == neededItem);
		}
		
		if(!replanting)
			harvest(blocksToHarvest);
		
		busy = !blocksToHarvest.isEmpty() || !blocksToReplant.isEmpty();
		
		renderer.updateVertexBuffers(blocksToHarvest, plants.keySet(),
			blocksToReplant);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		renderer.render(matrixStack);
		overlay.render(matrixStack, partialTicks, currentBlock);
	}
	
	/**
	 * Returns true if AutoFarm is currently harvesting or replanting something.
	 */
	public boolean isBusy()
	{
		return busy;
	}
	
	private void updatePlants(List<BlockPos> blocks)
	{
		for(BlockPos pos : blocks)
		{
			Item seed = seeds.get(BlockUtils.getBlock(pos));
			if(seed == null)
				continue;
			
			plants.put(pos, seed);
		}
	}
	
	private List<BlockPos> getBlocksToHarvest(Vec3d eyesVec,
		List<BlockPos> blocks)
	{
		return blocks.parallelStream().filter(this::shouldBeHarvested)
			.sorted(Comparator.comparingDouble(
				pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos))))
			.collect(Collectors.toList());
	}
	
	private boolean shouldBeHarvested(BlockPos pos)
	{
		Block block = BlockUtils.getBlock(pos);
		BlockState state = BlockUtils.getState(pos);
		
		if(block instanceof CropBlock)
			return ((CropBlock)block).isMature(state);
		
		if(block instanceof NetherWartBlock)
			return state.get(NetherWartBlock.AGE) >= 3;
		
		if(block instanceof CocoaBlock)
			return state.get(CocoaBlock.AGE) >= 2;
		
		if(block instanceof GourdBlock)
			return true;
		
		if(block instanceof SugarCaneBlock)
			return BlockUtils.getBlock(pos.down()) instanceof SugarCaneBlock
				&& !(BlockUtils
					.getBlock(pos.down(2)) instanceof SugarCaneBlock);
		
		if(block instanceof CactusBlock)
			return BlockUtils.getBlock(pos.down()) instanceof CactusBlock
				&& !(BlockUtils.getBlock(pos.down(2)) instanceof CactusBlock);
		
		if(block instanceof KelpPlantBlock)
			return BlockUtils.getBlock(pos.down()) instanceof KelpPlantBlock
				&& !(BlockUtils
					.getBlock(pos.down(2)) instanceof KelpPlantBlock);
		
		if(block instanceof BambooBlock)
			return BlockUtils.getBlock(pos.down()) instanceof BambooBlock
				&& !(BlockUtils.getBlock(pos.down(2)) instanceof BambooBlock);
		
		return false;
	}
	
	private List<BlockPos> getBlocksToReplant(Vec3d eyesVec, BlockPos eyesBlock,
		double rangeSq, int blockRange)
	{
		return BlockUtils.getAllInBoxStream(eyesBlock, blockRange)
			.filter(pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(pos -> BlockUtils.getState(pos).isReplaceable())
			.filter(pos -> plants.containsKey(pos)).filter(this::canBeReplanted)
			.sorted(Comparator.comparingDouble(
				pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos))))
			.collect(Collectors.toList());
	}
	
	private boolean canBeReplanted(BlockPos pos)
	{
		Item item = plants.get(pos);
		
		if(item == Items.WHEAT_SEEDS || item == Items.CARROT
			|| item == Items.POTATO || item == Items.BEETROOT_SEEDS
			|| item == Items.PUMPKIN_SEEDS || item == Items.MELON_SEEDS)
			return BlockUtils.getBlock(pos.down()) instanceof FarmlandBlock;
		
		if(item == Items.NETHER_WART)
			return BlockUtils.getBlock(pos.down()) instanceof SoulSandBlock;
		
		if(item == Items.COCOA_BEANS)
			return BlockUtils.getState(pos.north()).isIn(BlockTags.JUNGLE_LOGS)
				|| BlockUtils.getState(pos.east()).isIn(BlockTags.JUNGLE_LOGS)
				|| BlockUtils.getState(pos.south()).isIn(BlockTags.JUNGLE_LOGS)
				|| BlockUtils.getState(pos.west()).isIn(BlockTags.JUNGLE_LOGS);
		
		return false;
	}
	
	private boolean tryToReplant(BlockPos pos, Item neededItem)
	{
		ClientPlayerEntity player = MC.player;
		ItemStack heldItem = player.getMainHandStack();
		
		if(!heldItem.isEmpty() && heldItem.getItem() == neededItem)
		{
			placeBlockSimple(pos);
			return IMC.getItemUseCooldown() <= 0;
		}
		
		for(int slot = 0; slot < 36; slot++)
		{
			if(slot == player.getInventory().selectedSlot)
				continue;
			
			ItemStack stack = player.getInventory().getStack(slot);
			if(stack.isEmpty() || stack.getItem() != neededItem)
				continue;
			
			if(slot < 9)
				player.getInventory().selectedSlot = slot;
			else if(player.getInventory().getEmptySlot() < 9)
				IMC.getInteractionManager().windowClick_QUICK_MOVE(slot);
			else if(player.getInventory().getEmptySlot() != -1)
			{
				IMC.getInteractionManager().windowClick_QUICK_MOVE(
					player.getInventory().selectedSlot + 36);
				IMC.getInteractionManager().windowClick_QUICK_MOVE(slot);
			}else
			{
				IMC.getInteractionManager().windowClick_PICKUP(
					player.getInventory().selectedSlot + 36);
				IMC.getInteractionManager().windowClick_PICKUP(slot);
				IMC.getInteractionManager().windowClick_PICKUP(
					player.getInventory().selectedSlot + 36);
			}
			
			return true;
		}
		
		return false;
	}
	
	private void placeBlockSimple(BlockPos pos)
	{
		// should never happen, but just in case
		if(!BlockUtils.getState(pos).isReplaceable())
			return;
		
		BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
		if(params == null || params.distanceSq() > range.getValueSq())
			return;
		
		// face block
		WURST.getRotationFaker().faceVectorPacket(params.hitVec());
		if(RotationUtils.getAngleToLastReportedLookVec(params.hitVec()) > 1)
			return;
		
		// check cooldown
		if(IMC.getItemUseCooldown() > 0)
			return;
		
		Hand hand = Hand.MAIN_HAND;
		
		// place block
		ActionResult result = MC.interactionManager.interactBlock(MC.player,
			hand, params.toHitResult());
		
		// swing arm
		if(result.isAccepted() && result.shouldSwingHand())
			MC.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
		
		// reset cooldown
		IMC.setItemUseCooldown(4);
	}
	
	private void harvest(List<BlockPos> blocksToHarvest)
	{
		if(MC.player.getAbilities().creativeMode)
		{
			Stream<BlockPos> stream = blocksToHarvest.parallelStream();
			for(Set<BlockPos> set : prevBlocks)
				stream = stream.filter(pos -> !set.contains(pos));
			List<BlockPos> filteredBlocks = stream.collect(Collectors.toList());
			
			prevBlocks.addLast(new HashSet<>(filteredBlocks));
			while(prevBlocks.size() > 5)
				prevBlocks.removeFirst();
			
			if(!filteredBlocks.isEmpty())
				currentBlock = filteredBlocks.get(0);
			
			MC.interactionManager.cancelBlockBreaking();
			overlay.resetProgress();
			BlockBreaker.breakBlocksWithPacketSpam(filteredBlocks);
			return;
		}
		
		for(BlockPos pos : blocksToHarvest)
			if(BlockBreaker.breakOneBlock(pos))
			{
				currentBlock = pos;
				break;
			}
		
		if(currentBlock == null)
			MC.interactionManager.cancelBlockBreaking();
		
		if(currentBlock != null && BlockUtils.getHardness(currentBlock) < 1)
			overlay.updateProgress();
		else
			overlay.resetProgress();
	}
}
