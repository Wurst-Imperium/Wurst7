/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.RenderUtils;

public final class AutoFarmRenderer
{
	private static final Box BLOCK_BOX =
		new Box(BlockPos.ORIGIN).contract(1 / 16.0);
	private static final Box NODE_BOX = new Box(BlockPos.ORIGIN).contract(0.25);
	
	public final CheckboxSetting drawReplantingSpots =
		new CheckboxSetting("Draw replanting spots", true);
	public final CheckboxSetting drawBlocksToHarvest =
		new CheckboxSetting("Draw blocks to harvest", true);
	public final CheckboxSetting drawBlocksToReplant =
		new CheckboxSetting("Draw blocks to replant", true);
	
	private List<Box> replantingSpots = List.of();
	private List<Box> blocksToHarvest = List.of();
	private List<Box> blocksToReplant = List.of();
	
	public void update(Collection<BlockPos> replantingSpots,
		Collection<BlockPos> blocksToHarvest,
		Collection<BlockPos> blocksToReplant)
	{
		this.replantingSpots =
			replantingSpots.stream().map(NODE_BOX::offset).toList();
		this.blocksToHarvest =
			blocksToHarvest.stream().map(BLOCK_BOX::offset).toList();
		this.blocksToReplant =
			blocksToReplant.stream().map(BLOCK_BOX::offset).toList();
	}
	
	public void render(MatrixStack matrixStack)
	{
		if(drawReplantingSpots.isChecked())
			RenderUtils.drawNodes(matrixStack, replantingSpots, 0x8000FFFF,
				false);
		
		if(drawBlocksToHarvest.isChecked())
			RenderUtils.drawOutlinedBoxes(matrixStack, blocksToHarvest,
				0x8000FF00, false);
		
		if(drawBlocksToReplant.isChecked())
			RenderUtils.drawOutlinedBoxes(matrixStack, blocksToReplant,
				0x80FF0000, false);
	}
	
	public Stream<Setting> getSettings()
	{
		return Stream.of(drawReplantingSpots, drawBlocksToHarvest,
			drawBlocksToReplant);
	}
}
