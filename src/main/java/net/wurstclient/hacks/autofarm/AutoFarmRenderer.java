/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.RenderUtils;

public final class AutoFarmRenderer
{
	private static final AABB BLOCK_BOX =
		new AABB(BlockPos.ZERO).deflate(1 / 16.0);
	private static final AABB NODE_BOX = new AABB(BlockPos.ZERO).deflate(0.25);
	
	public final CheckboxSetting drawReplantingSpots =
		new CheckboxSetting("Draw replanting spots", true);
	public final CheckboxSetting drawBlocksToHarvest =
		new CheckboxSetting("Draw blocks to harvest", true);
	public final CheckboxSetting drawBlocksToReplant =
		new CheckboxSetting("Draw blocks to replant", true);
	
	private List<AABB> replantingSpots = List.of();
	private List<AABB> blocksToHarvest = List.of();
	private List<AABB> blocksToReplant = List.of();
	
	public void update(Collection<BlockPos> replantingSpots,
		Collection<BlockPos> blocksToHarvest,
		Collection<BlockPos> blocksToReplant)
	{
		this.replantingSpots =
			replantingSpots.stream().map(NODE_BOX::move).toList();
		this.blocksToHarvest =
			blocksToHarvest.stream().map(BLOCK_BOX::move).toList();
		this.blocksToReplant =
			blocksToReplant.stream().map(BLOCK_BOX::move).toList();
	}
	
	public void render(PoseStack matrixStack)
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
