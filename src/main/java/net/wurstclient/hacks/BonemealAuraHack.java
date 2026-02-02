/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FaceTargetSetting;
import net.wurstclient.settings.FaceTargetSetting.FaceTarget;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.settings.TakeItemsFromSetting;
import net.wurstclient.settings.TakeItemsFromSetting.TakeItemsFrom;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockBreaker.BlockBreakingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.InteractionSimulator;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"bonemeal aura", "bone meal aura", "AutoBonemeal", "auto bonemeal",
	"auto bone meal", "fertilizer", "bmaura"})
public final class BonemealAuraHack extends Hack implements HandleInputListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting multiMeal = new CheckboxSetting("MultiMeal",
		"description.wurst.setting.bonemealaura.multimeal", false);
	
	private final CheckboxSetting checkLOS =
		new CheckboxSetting("Check line of sight",
			"description.wurst.setting.bonemealaura.check_los", true);
	
	private final FaceTargetSetting faceTarget =
		FaceTargetSetting.withPacketSpam(this, FaceTarget.SERVER);
	
	private final SwingHandSetting swingHand =
		new SwingHandSetting(this, SwingHand.CLIENT);
	
	private final CheckboxSetting fastPlace =
		new CheckboxSetting("Always FastPlace",
			"description.wurst.setting.bonemealaura.always_fastplace", true);
	
	private final CheckboxSetting useWhileBreaking =
		new CheckboxSetting("Use while breaking",
			"description.wurst.setting.bonemealaura.use_while_breaking", false);
	
	private final CheckboxSetting useWhileRiding =
		new CheckboxSetting("Use while riding",
			"description.wurst.setting.bonemealaura.use_while_riding", false);
	
	private final TakeItemsFromSetting takeItemsFrom =
		TakeItemsFromSetting.withHands(this, TakeItemsFrom.HANDS);
	
	private final CheckboxSetting saplings =
		new CheckboxSetting("Saplings", true);
	
	private final CheckboxSetting crops = new CheckboxSetting("Crops",
		"Wheat, carrots, potatoes and beetroots.", true);
	
	private final CheckboxSetting stems =
		new CheckboxSetting("Stems", "Pumpkins and melons.", true);
	
	private final CheckboxSetting cocoa = new CheckboxSetting("Cocoa", true);
	
	private final CheckboxSetting seaPickles =
		new CheckboxSetting("Sea pickles", true);
	
	private final CheckboxSetting other = new CheckboxSetting("Other", false);
	
	public BonemealAuraHack()
	{
		super("BonemealAura");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(multiMeal);
		addSetting(checkLOS);
		addSetting(faceTarget);
		addSetting(swingHand);
		addSetting(fastPlace);
		addSetting(useWhileBreaking);
		addSetting(useWhileRiding);
		addSetting(takeItemsFrom);
		addSetting(saplings);
		addSetting(crops);
		addSetting(stems);
		addSetting(cocoa);
		addSetting(seaPickles);
		addSetting(other);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(HandleInputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(HandleInputListener.class, this);
	}
	
	@Override
	public void onHandleInput()
	{
		if(!fastPlace.isChecked() && MC.rightClickDelay > 0)
			return;
		
		if(!useWhileBreaking.isChecked() && MC.gameMode.isDestroying())
			return;
		
		if(!useWhileRiding.isChecked() && MC.player.isHandsBusy())
			return;
		
		if(WURST.getHax().autoFarmHack.isBusy())
			return;
		
		boolean holdingBoneMeal = MC.player.isHolding(Items.BONE_MEAL);
		int boneMealSlot = InventoryUtils.indexOf(Items.BONE_MEAL,
			takeItemsFrom.getMaxInvSlot());
		if(!holdingBoneMeal && boneMealSlot < 0)
			return;
		
		List<BlockBreakingParams> validBlocks = getValidBlocks();
		if(validBlocks.isEmpty())
			return;
		
		if(!holdingBoneMeal)
		{
			InventoryUtils.selectItem(boneMealSlot);
			return;
		}
		
		if(multiMeal.isChecked())
		{
			boolean shouldSwing = false;
			
			for(BlockBreakingParams params : validBlocks)
			{
				faceTarget.face(params.hitVec());
				InteractionSimulator.rightClickBlock(params.toHitResult(),
					SwingHand.OFF);
				shouldSwing = true;
			}
			
			if(shouldSwing)
				swingHand.swing(InteractionHand.MAIN_HAND);
			
		}else
		{
			BlockBreakingParams params = validBlocks.getFirst();
			MC.rightClickDelay = 4;
			faceTarget.face(params.hitVec());
			InteractionSimulator.rightClickBlock(params.toHitResult(),
				swingHand.getSelected());
		}
	}
	
	private List<BlockBreakingParams> getValidBlocks()
	{
		BlockPos eyesBlock = BlockPos.containing(RotationUtils.getEyesPos());
		double rangeSq = range.getValueSq();
		
		Stream<BlockBreakingParams> stream = BlockUtils
			.getAllInBoxStream(eyesBlock, range.getValueCeil())
			.filter(this::isCorrectBlock)
			.map(BlockBreaker::getBlockBreakingParams).filter(Objects::nonNull)
			.filter(params -> params.distanceSq() <= rangeSq);
		
		if(checkLOS.isChecked())
			stream = stream.filter(BlockBreakingParams::lineOfSight);
			
		// As plants are bone-mealed, they will grow larger and prevent line of
		// sight to other plants behind them. That's why we need to bone-meal
		// the farthest plants first.
		Comparator<BlockBreakingParams> farthestFirst = Comparator
			.comparingDouble(BlockBreakingParams::distanceSq).reversed();
		stream = stream.sorted(farthestFirst);
		
		return stream.toList();
	}
	
	private boolean isCorrectBlock(BlockPos pos)
	{
		Block block = BlockUtils.getBlock(pos);
		BlockState state = BlockUtils.getState(pos);
		
		if(!(block instanceof BonemealableBlock bmBlock)
			|| !bmBlock.isValidBonemealTarget(MC.level, pos, state))
			return false;
		
		if(block instanceof GrassBlock)
			return false;
		
		if(block instanceof SaplingBlock)
			return saplings.isChecked();
		
		if(block instanceof CropBlock)
			return crops.isChecked();
		
		if(block instanceof StemBlock)
			return stems.isChecked();
		
		if(block instanceof CocoaBlock)
			return cocoa.isChecked();
		
		if(block instanceof SeaPickleBlock)
			return seaPickles.isChecked();
		
		return other.isChecked();
	}
}
