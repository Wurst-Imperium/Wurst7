/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.OreBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RotationUtils;

// TODO: Implement a customized excavator hack
// TODO: Add GUI with number of mined ores and the targeted ore
@DontSaveState
public final class OreMinerHack extends Hack
	implements UpdateListener
{
	
	private int mineDir = 0;

	private final SliderSetting range =
		new SliderSetting("Search Range", 20, 5, 200, 1, ValueDisplay.INTEGER);

	private final CheckboxSetting yRange = new CheckboxSetting("(Experimental) Also mine at Y +/- 1", false);

	private final CheckboxSetting messages = new CheckboxSetting("'Ore found' messages", false);

	private final SliderSetting coalWeight =
		new SliderSetting("Coal Weight (0 = Don't Search)", 1, 0, 200, 0.05, ValueDisplay.DECIMAL);

	private final SliderSetting ironWeight =
		new SliderSetting("Iron Weight (0 = Don't Search)", 8, 0, 200, 0.05, ValueDisplay.DECIMAL);

	private final SliderSetting goldWeight =
		new SliderSetting("Gold Weight (0 = Don't Search)", 3, 0, 200, 0.05, ValueDisplay.DECIMAL);

	private final SliderSetting diamondWeight =
		new SliderSetting("Diamond Weight (0 = Don't Search)", 60, 0, 200, 0.05, ValueDisplay.DECIMAL);

	private final SliderSetting emeraldWeight =
		new SliderSetting("Emerald Weight (0 = Don't Search)", 45, 0, 200, 0.05, ValueDisplay.DECIMAL);

	private final SliderSetting redstoneWeight =
		new SliderSetting("Redstone Weight (0 = Don't Search)", 12, 0, 200, 0.05, ValueDisplay.DECIMAL);

	private final SliderSetting lapisWeight =
		new SliderSetting("Lapis Weight (0 = Don't Search)", 12, 0, 200, 0.05, ValueDisplay.DECIMAL);

	public OreMinerHack()
	{
		super("OreMiner",
			"Searches near ores and mines them.\n\n"
				+ "\u00a76\u00a7lWARNING:\u00a7r Uses Excavator Hack.\n"
				+ "Some settings can only be changed there.\n"
				+ "Range 2 is recommended to get all ores.\n"
				+ "Disable Excavator if it is stuck.\n");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(yRange);
		addSetting(messages);

		addSetting(coalWeight);
		addSetting(ironWeight);
		addSetting(goldWeight);
		addSetting(diamondWeight);
		addSetting(emeraldWeight);
		addSetting(redstoneWeight);
		addSetting(lapisWeight);
    }
    
    @Override
    public void onEnable()
    {
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
		WURST.getHax().speedNukerHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
    }

    @Override
    public void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);
		WURST.getHax().excavatorHack.setEnabled(false);
    }

    @Override
    public void onUpdate()
    {
		boolean isExcavating = WURST.getHax().excavatorHack.isEnabled();
		if(!isExcavating) {
			BlockPos eyesBlock = new BlockPos(RotationUtils.getEyesPos());
			int blockRange = (int)Math.ceil(range.getValue());
			
			List<BlockPos> blocks = getBlockStream(eyesBlock, blockRange)
				.filter(this::isOre)
				.sorted(Comparator.comparingDouble(this::oreWeight))
				.collect(Collectors.toList());
			
			if(blocks.isEmpty()) {
				ChatUtils.message("Can't find more ores in range!");
				setEnabled(false);
				return;
			}
			
			BlockPos target = blocks.get(0);

			// mineDir 0: mine in x direction with start at player pos
			// mineDir 1: mine in z direction with end at block pos
			// mineDir 2: mine in y direction
			if(mineDir == 0) {
				if(messages.isChecked()) sendOreFoundMessage(target);
				BlockPos end = new BlockPos(target.getX(), eyesBlock.getY()-1, eyesBlock.getZ());
				WURST.getHax().excavatorHack.enableWithArea(eyesBlock, end);
			} else if(mineDir == 1) {
				BlockPos start = new BlockPos(target.getX(), eyesBlock.getY(), eyesBlock.getZ());
				BlockPos end = new BlockPos(target.getX(), eyesBlock.getY()-1, target.getZ());
				WURST.getHax().excavatorHack.enableWithArea(start, end);
			} else if(yRange.isChecked() && mineDir == 2) {
				BlockPos start = new BlockPos(target.getX(), eyesBlock.getY()-(eyesBlock.getY()>target.getY()?0:1), target.getZ());
				WURST.getHax().excavatorHack.enableWithArea(start, target);
			}

			mineDir = (++mineDir)%(yRange.isChecked()?3:2);
		}
	}

	private boolean isOre(BlockPos pos)
	{
		Block block = BlockUtils.getBlock(pos);
		
		if(!(block instanceof OreBlock)) return false;
		if(block == Blocks.COAL_ORE && coalWeight.getValue() != 0.0) return true;
		if(block == Blocks.IRON_ORE && ironWeight.getValue() != 0.0) return true;
		if(block == Blocks.GOLD_ORE && goldWeight.getValue() != 0.0) return true;
		if(block == Blocks.DIAMOND_ORE && diamondWeight.getValue() != 0.0) return true;
		if(block == Blocks.EMERALD_ORE && emeraldWeight.getValue() != 0.0) return true;
		if(block == Blocks.REDSTONE_ORE && redstoneWeight.getValue() != 0.0) return true;
		if(block == Blocks.LAPIS_ORE && lapisWeight.getValue() != 0.0) return true;
		
		return false;
	}

	private double oreWeight(BlockPos pos)
	{
		Block block = BlockUtils.getBlock(pos);
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double dist = eyesVec.squaredDistanceTo(new Vec3d(pos));
		double weight = 1.0;
		
		if(block instanceof OreBlock) {
			if(block == Blocks.COAL_ORE) weight = coalWeight.getValue();
			if(block == Blocks.IRON_ORE) weight = ironWeight.getValue();
			if(block == Blocks.GOLD_ORE) weight = goldWeight.getValue();
			if(block == Blocks.DIAMOND_ORE) weight = diamondWeight.getValue();
			if(block == Blocks.EMERALD_ORE) weight = emeraldWeight.getValue();
			if(block == Blocks.REDSTONE_ORE) weight = redstoneWeight.getValue();
			if(block == Blocks.LAPIS_ORE) weight = lapisWeight.getValue();
		}
		if(weight <= 0.0) weight = 1.0;
		return dist/weight;
	}

	private Stream<BlockPos> getBlockStream(BlockPos center, int range)
	{
		BlockPos min = center.add(-range, yRange.isChecked()?-2:-1, -range);
		BlockPos max = center.add(range, yRange.isChecked()?1:0, range);
		
		return BlockUtils.getAllInBox(min, max).stream();
	}

	private void sendOreFoundMessage(BlockPos pos) {
		Block block = BlockUtils.getBlock(pos);
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double dist = (int)Math.ceil(eyesVec.distanceTo(new Vec3d(pos)));
		String oreName = "";
		
		if(block == Blocks.COAL_ORE) oreName = "Coal";
		if(block == Blocks.IRON_ORE) oreName = "Iron";
		if(block == Blocks.GOLD_ORE) oreName = "Gold";
		if(block == Blocks.DIAMOND_ORE) oreName = "Diamond";
		if(block == Blocks.EMERALD_ORE) oreName = "Emerald";
		if(block == Blocks.REDSTONE_ORE) oreName = "Redstone";
		if(block == Blocks.LAPIS_ORE) oreName = "Lapis";

		ChatUtils.message("Found " + oreName + " Ore " + dist + " blocks away");
	}

}
