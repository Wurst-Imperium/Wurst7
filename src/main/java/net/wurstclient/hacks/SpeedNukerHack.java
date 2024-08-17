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
import java.util.stream.Collectors;

import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"speed nuker", "FastNuker", "fast nuker"})
@DontSaveState
public final class SpeedNukerHack extends Hack
	implements LeftClickListener, UpdateListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"\u00a7lNormal\u00a7r mode simply breaks everything around you.\n"
			+ "\u00a7lID\u00a7r mode only breaks the selected block type. Left-click on a block to select it.\n"
			+ "\u00a7lMultiID\u00a7r mode only breaks the block types in your MultiID List.\n"
			+ "\u00a7lFlat\u00a7r mode flattens the area around you, but won't dig down.\n"
			+ "\u00a7lSmash\u00a7r mode only breaks blocks that can be destroyed instantly (e.g. tall grass).",
		Mode.values(), Mode.NORMAL);
	
	private final BlockSetting id =
		new BlockSetting("ID", "The type of block to break in ID mode.\n"
			+ "air = won't break anything", "minecraft:air", true);
	
	private final CheckboxSetting lockId = new CheckboxSetting("Lock ID",
		"Prevents changing the ID by clicking on blocks or restarting SpeedNuker.",
		false);
	
	private final BlockListSetting multiIdList = new BlockListSetting(
		"MultiID List", "The types of blocks to break in MultiID mode.",
		"minecraft:ancient_debris", "minecraft:bone_block",
		"minecraft:coal_ore", "minecraft:copper_ore",
		"minecraft:deepslate_coal_ore", "minecraft:deepslate_copper_ore",
		"minecraft:deepslate_diamond_ore", "minecraft:deepslate_emerald_ore",
		"minecraft:deepslate_gold_ore", "minecraft:deepslate_iron_ore",
		"minecraft:deepslate_lapis_ore", "minecraft:deepslate_redstone_ore",
		"minecraft:diamond_ore", "minecraft:emerald_ore", "minecraft:glowstone",
		"minecraft:gold_ore", "minecraft:iron_ore", "minecraft:lapis_ore",
		"minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
		"minecraft:raw_copper_block", "minecraft:raw_gold_block",
		"minecraft:raw_iron_block", "minecraft:redstone_ore");
	
	public SpeedNukerHack()
	{
		super("SpeedNuker");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(mode);
		addSetting(id);
		addSetting(lockId);
		addSetting(multiIdList);
	}
	
	@Override
	public String getRenderName()
	{
		switch(mode.getSelected())
		{
			default:
			case NORMAL:
			return getName();
			
			case ID:
			return "IDSpeedNuker [" + id.getShortBlockName() + "]";
			
			case MULTI_ID:
			int ids = multiIdList.getBlockNames().size();
			return "MultiIDSpeedNuker [" + ids + (ids == 1 ? " ID]" : " IDs]");
			
			case FLAT:
			return "FlatSpeedNuker";
			
			case SMASH:
			return "SmashSpeedNuker";
		}
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
		WURST.getHax().tunnellerHack.setEnabled(false);
		
		EVENTS.add(LeftClickListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(LeftClickListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
		
		// resets
		if(!lockId.isChecked())
			id.setBlock(Blocks.AIR);
	}
	
	@Override
	public void onUpdate()
	{
		// abort if using ID mode without an ID being set
		if(mode.getSelected() == Mode.ID && id.getBlock() == Blocks.AIR)
			return;
		
		Vec3d eyesVec = RotationUtils.getEyesPos();
		BlockPos eyesBlock = BlockPos.ofFloored(eyesVec);
		double rangeSq = range.getValueSq();
		int blockRange = range.getValueCeil();
		
		ArrayList<BlockPos> blocks =
			BlockUtils.getAllInBoxStream(eyesBlock, blockRange)
				.filter(pos -> pos.getSquaredDistance(eyesVec) <= rangeSq)
				.filter(BlockUtils::canBeClicked).filter(this::shouldBreakBlock)
				.sorted(Comparator
					.comparingDouble(pos -> pos.getSquaredDistance(eyesVec)))
				.collect(Collectors.toCollection(ArrayList::new));
		
		if(!blocks.isEmpty())
			WURST.getHax().autoToolHack.equipIfEnabled(blocks.get(0));
		
		BlockBreaker.breakBlocksWithPacketSpam(blocks);
	}
	
	private boolean shouldBreakBlock(BlockPos pos)
	{
		switch(mode.getSelected())
		{
			default:
			case NORMAL:
			return true;
			
			case ID:
			return BlockUtils.getName(pos).equals(id.getBlockName());
			
			case MULTI_ID:
			return multiIdList.contains(BlockUtils.getBlock(pos));
			
			case FLAT:
			return pos.getY() >= MC.player.getY();
			
			case SMASH:
			return BlockUtils.getHardness(pos) >= 1;
		}
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(lockId.isChecked() || mode.getSelected() != Mode.ID)
			return;
		
		if(!(MC.crosshairTarget instanceof BlockHitResult bHitResult)
			|| bHitResult.getType() != HitResult.Type.BLOCK)
			return;
		
		id.setBlockName(BlockUtils.getName(bHitResult.getBlockPos()));
	}
	
	private enum Mode
	{
		NORMAL("Normal"),
		ID("ID"),
		MULTI_ID("MultiID"),
		FLAT("Flat"),
		SMASH("Smash");
		
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
}
