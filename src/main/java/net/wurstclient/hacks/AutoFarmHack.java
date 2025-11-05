/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.hacks.autofarm.AutoFarmPlantTypeManager;
import net.wurstclient.hacks.autofarm.AutoFarmRenderer;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FaceTargetSetting;
import net.wurstclient.settings.FaceTargetSetting.FaceTarget;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.*;
import net.wurstclient.util.BlockBreaker.BlockBreakingParams;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;

@SearchTags({"auto farm", "AutoHarvest", "auto harvest"})
public final class AutoFarmHack extends Hack
	implements UpdateListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting checkLOS =
		new CheckboxSetting("Check line of sight",
			"description.wurst.setting.autofarm.check_line_of_sight", false);
	
	private final FaceTargetSetting faceTarget =
		FaceTargetSetting.withoutPacketSpam(this, FaceTarget.SERVER);
	
	private final SwingHandSetting swingHand =
		new SwingHandSetting(this, SwingHand.SERVER);
	
	private final AutoFarmPlantTypeManager plantTypes =
		new AutoFarmPlantTypeManager();
	
	private final HashMap<BlockPos, AutoFarmPlantType> replantingSpots =
		new HashMap<>();
	private final BlockBreakingCache cache = new BlockBreakingCache();
	private BlockPos currentlyMining;
	
	private final AutoFarmRenderer renderer = new AutoFarmRenderer();
	private final OverlayRenderer overlay = new OverlayRenderer();
	
	private boolean busy;
	
	public AutoFarmHack()
	{
		super("AutoFarm");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(checkLOS);
		addSetting(faceTarget);
		addSetting(swingHand);
		renderer.getSettings().forEach(this::addSetting);
		plantTypes.getSettings().forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().autoMineHack.setEnabled(false);
		replantingSpots.clear();
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		if(currentlyMining != null)
		{
			MC.interactionManager.breakingBlock = true;
			MC.interactionManager.cancelBlockBreaking();
			currentlyMining = null;
		}
		
		cache.reset();
		overlay.resetProgress();
		busy = false;
	}
	
	@Override
	public void onUpdate()
	{
		currentlyMining = null;
		Vec3d eyesVec = RotationUtils.getEyesPos();
		BlockPos eyesBlock = BlockPos.ofFloored(eyesVec);
		double rangeSq = range.getValueSq();
		int blockRange = range.getValueCeil();
		
		List<BlockPos> nonEmptyBlocks =
			BlockUtils.getAllInBoxStream(eyesBlock, blockRange)
				.filter(pos -> pos.getSquaredDistance(eyesVec) <= rangeSq)
				.filter(BlockUtils::canBeClicked).toList();
		
		for(BlockPos pos : nonEmptyBlocks)
		{
			AutoFarmPlantType plantType = plantTypes.getReplantingSpotType(pos);
			if(plantType != null)
				replantingSpots.put(pos, plantType);
		}
		
		List<BlockPos> blocksToMine = List.of();
		List<BlockPos> blocksToInteract = List.of();
		List<BlockPos> blocksToReplant = List.of();
		
		if(!WURST.getHax().freecamHack.isEnabled())
		{
			blocksToMine = nonEmptyBlocks.stream()
				.filter(plantTypes::shouldHarvestByMining)
				.sorted(Comparator
					.comparingDouble(pos -> pos.getSquaredDistance(eyesVec)))
				.toList();
			
			blocksToInteract = nonEmptyBlocks.stream()
				.filter(plantTypes::shouldHarvestByInteracting)
				.sorted(Comparator
					.comparingDouble(pos -> pos.getSquaredDistance(eyesVec)))
				.toList();
			
			blocksToReplant =
				BlockUtils.getAllInBoxStream(eyesBlock, blockRange)
					.filter(pos -> pos.getSquaredDistance(eyesVec) <= rangeSq)
					.filter(pos -> BlockUtils.getState(pos).isReplaceable())
					.filter(pos -> {
						AutoFarmPlantType plantType = replantingSpots.get(pos);
						return plantType != null
							&& plantType.isReplantingEnabled()
							&& plantType.hasPlantingSurface(pos);
					}).sorted(Comparator.comparingDouble(
						pos -> pos.getSquaredDistance(eyesVec)))
					.toList();
		}
		
		boolean replanting = replant(blocksToReplant);
		boolean interacting =
			replanting ? false : harvestByInteracting(blocksToInteract);
		if(!interacting && !replanting)
			harvestByMining(blocksToMine);
		
		busy = replanting || interacting || currentlyMining != null;
		
		List<BlockPos> blocksToHarvest = Stream
			.of(blocksToMine, blocksToInteract).flatMap(List::stream).toList();
		renderer.update(replantingSpots.keySet(), blocksToHarvest,
			blocksToReplant);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		renderer.render(matrixStack);
		
		if(renderer.drawBlocksToHarvest.isChecked())
			overlay.render(matrixStack, partialTicks, currentlyMining);
	}
	
	/**
	 * Returns true if AutoFarm is currently harvesting or replanting something.
	 */
	public boolean isBusy()
	{
		return busy;
	}
	
	private boolean replant(List<BlockPos> blocksToReplant)
	{
		if(MC.itemUseCooldown > 0)
			return false;
		
		if(MC.interactionManager.isBreakingBlock() || MC.player.isRiding())
			return false;
		
		Optional<Item> heldSeed =
			blocksToReplant.stream().map(replantingSpots::get)
				.filter(Objects::nonNull).map(AutoFarmPlantType::getSeedItem)
				.distinct().filter(MC.player::isHolding).findFirst();
		
		if(heldSeed.isPresent())
		{
			Item item = heldSeed.get();
			Hand hand = MC.player.getMainHandStack().isOf(item) ? Hand.MAIN_HAND
				: Hand.OFF_HAND;
			
			for(BlockPos pos : blocksToReplant)
			{
				AutoFarmPlantType plantType = replantingSpots.get(pos);
				if(plantType == null || plantType.getSeedItem() != item)
					continue;
				
				BlockPlacingParams params =
					BlockPlacer.getBlockPlacingParams(pos);
				if(params == null || params.distanceSq() > range.getValueSq())
					continue;
				
				if(checkLOS.isChecked() && !params.lineOfSight())
					continue;
				
				MC.itemUseCooldown = 4;
				faceTarget.face(params.hitVec());
				InteractionSimulator.rightClickBlock(params.toHitResult(), hand,
					swingHand.getSelected());
				return true;
			}
		}
		
		for(BlockPos pos : blocksToReplant)
		{
			BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
			if(params == null || params.distanceSq() > range.getValueSq())
				continue;
			
			AutoFarmPlantType plantType = replantingSpots.get(pos);
			if(plantType == null)
				continue;
			
			if(InventoryUtils.selectItem(plantType.getSeedItem())
				|| InventoryUtils.giveCreativeItem(plantType.getSeedItem()))
				return true;
		}
		
		return false;
	}
	
	private boolean harvestByInteracting(List<BlockPos> blocksToInteract)
	{
		if(MC.itemUseCooldown > 0)
			return false;
		
		if(MC.interactionManager.isBreakingBlock() || MC.player.isRiding())
			return false;
		
		for(BlockPos pos : blocksToInteract)
		{
			BlockBreakingParams params =
				BlockBreaker.getBlockBreakingParams(pos);
			if(params == null || params.distanceSq() > range.getValueSq())
				continue;
			
			if(checkLOS.isChecked() && !params.lineOfSight())
				continue;
			
			if(MC.player.getMainHandStack().isOf(Items.BONE_MEAL))
				return InventoryUtils.selectItem(s -> !s.isOf(Items.BONE_MEAL));
			
			MC.itemUseCooldown = 4;
			faceTarget.face(params.hitVec());
			InteractionSimulator.rightClickBlock(params.toHitResult(),
				swingHand.getSelected());
			return true;
		}
		
		return false;
	}
	
	private void harvestByMining(List<BlockPos> blocksToMine)
	{
		double rangeSq = range.getValueSq();
		Stream<BlockBreakingParams> stream = blocksToMine.stream()
			.map(BlockBreaker::getBlockBreakingParams).filter(Objects::nonNull)
			.filter(params -> params.distanceSq() <= rangeSq);
		
		if(checkLOS.isChecked())
			stream = stream.filter(BlockBreakingParams::lineOfSight);
		
		stream = stream.sorted(BlockBreaker.comparingParams());
		
		// Break all blocks in creative mode
		if(MC.player.getAbilities().creativeMode
			&& faceTarget.getSelected() == FaceTarget.OFF)
		{
			MC.interactionManager.cancelBlockBreaking();
			overlay.resetProgress();
			
			List<BlockPos> blocks = cache
				.filterOutRecentBlocks(stream.map(BlockBreakingParams::pos));
			if(blocks.isEmpty())
				return;
			
			currentlyMining = blocks.get(0);
			BlockBreaker.breakBlocksWithPacketSpam(blocks);
			swingHand.swing(Hand.MAIN_HAND);
			return;
		}
		
		// Break the first valid block in survival mode
		currentlyMining = stream.filter(this::breakOneBlock)
			.map(BlockBreakingParams::pos).findFirst().orElse(null);
		
		if(currentlyMining == null)
		{
			MC.interactionManager.cancelBlockBreaking();
			overlay.resetProgress();
			return;
		}
		
		overlay.updateProgress();
	}
	
	private boolean breakOneBlock(BlockBreakingParams params)
	{
		faceTarget.face(params.hitVec());
		
		if(!MC.interactionManager.updateBlockBreakingProgress(params.pos(),
			params.side()))
			return false;
		
		swingHand.swing(Hand.MAIN_HAND);
		return true;
	}
}
