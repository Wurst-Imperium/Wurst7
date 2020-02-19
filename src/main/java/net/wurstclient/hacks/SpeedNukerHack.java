/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.block.Material;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"speed nuker", "FastNuker", "fast nuker"})
public final class SpeedNukerHack extends Hack
	implements LeftClickListener, UpdateListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Mode> mode =
		new EnumSetting<>("Mode", Mode.values(), Mode.NORMAL);
	
	public SpeedNukerHack()
	{
		super("SpeedNuker",
			"Faster version of Nuker that cannot bypass NoCheat+.");
		
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(mode);
	}
	
	@Override
	public String getRenderName()
	{
		return mode.getSelected().renderName.get();
	}
	
	@Override
	public void onEnable()
	{
		// disable other nukers
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().nukerHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
		WURST.getHax().tunnellerHack.setEnabled(false);
		
		// add listeners
		EVENTS.add(LeftClickListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		// remove listeners
		EVENTS.remove(LeftClickListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
		
		// resets
		WURST.getHax().nukerHack.setId(null);
	}
	
	@Override
	public void onUpdate()
	{
		// abort if using IDNuker without an ID being set
		if(mode.getSelected() == Mode.ID
			&& WURST.getHax().nukerHack.getId() == null)
			return;
		
		// get valid blocks
		Iterable<BlockPos> validBlocks =
			getValidBlocks(range.getValue(), mode.getSelected().validator);
		
		Iterator<BlockPos> autoToolIterator = validBlocks.iterator();
		if(autoToolIterator.hasNext())
			WURST.getHax().autoToolHack.equipIfEnabled(autoToolIterator.next());
		
		// break all blocks
		BlockBreaker.breakBlocksWithPacketSpam(validBlocks);
	}
	
	private ArrayList<BlockPos> getValidBlocks(double range,
		Predicate<BlockPos> validator)
	{
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double rangeSq = Math.pow(range + 0.5, 2);
		int rangeI = (int)Math.ceil(range);
		
		BlockPos center = new BlockPos(RotationUtils.getEyesPos());
		BlockPos min = center.add(-rangeI, -rangeI, -rangeI);
		BlockPos max = center.add(rangeI, rangeI, rangeI);
		
		return BlockUtils.getAllInBox(min, max).stream()
			.filter(pos -> eyesVec.squaredDistanceTo(new Vec3d(pos)) <= rangeSq)
			.filter(BlockUtils::canBeClicked).filter(validator)
			.sorted(Comparator.comparingDouble(
				pos -> eyesVec.squaredDistanceTo(new Vec3d(pos))))
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		// check hitResult
		if(MC.crosshairTarget == null
			|| !(MC.crosshairTarget instanceof BlockHitResult))
			return;
		
		// check pos
		BlockPos pos = ((BlockHitResult)MC.crosshairTarget).getBlockPos();
		if(pos == null
			|| BlockUtils.getState(pos).getMaterial() == Material.AIR)
			return;
		
		// check mode
		if(mode.getSelected() != Mode.ID)
			return;
		
		// set id
		WURST.getHax().nukerHack.setId(BlockUtils.getName(pos));
	}
	
	private enum Mode
	{
		NORMAL("Normal", () -> "SpeedNuker", pos -> true),
		
		ID("ID",
			() -> "IDSpeedNuker [" + WURST.getHax().nukerHack.getId() + "]",
			pos -> WURST.getHax().nukerHack.getId()
				.equals(BlockUtils.getName(pos))),
		
		FLAT("Flat", () -> "FlatSpeedNuker",
			pos -> pos.getY() >= MC.player.getY()),
		
		SMASH("Smash", () -> "SmashSpeedNuker",
			pos -> BlockUtils.getHardness(pos) >= 1);
		
		private final String name;
		private final Supplier<String> renderName;
		private final Predicate<BlockPos> validator;
		
		private Mode(String name, Supplier<String> renderName,
			Predicate<BlockPos> validator)
		{
			this.name = name;
			this.renderName = renderName;
			this.validator = validator;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
