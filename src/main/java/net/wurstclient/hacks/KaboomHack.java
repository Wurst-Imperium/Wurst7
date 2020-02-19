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
import java.util.stream.Collectors;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

public final class KaboomHack extends Hack implements UpdateListener
{
	private final SliderSetting power =
		new SliderSetting("Power", 128, 32, 512, 32, ValueDisplay.INTEGER);
	
	public KaboomHack()
	{
		super("Kaboom",
			"Breaks blocks around you like an explosion.\n"
				+ "This can be a lot faster than Nuker if the server doesn't\n"
				+ "have NoCheat+.\n"
				+ "It works best with fast tools and weak blocks.\n"
				+ "Note: This is not an actual explosion.");
		
		setCategory(Category.BLOCKS);
		addSetting(power);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// check fly-kick
		if(!MC.player.abilities.creativeMode && !MC.player.onGround)
			return;
		
		// do explosion particles
		new Explosion(MC.world, MC.player, MC.player.getX(), MC.player.getY(),
			MC.player.getZ(), 6F, false, Explosion.DestructionType.NONE)
				.affectWorld(true);
		
		// get valid blocks
		ArrayList<BlockPos> blocks = getBlocksByDistanceReversed(6);
		
		// break all blocks
		for(int i = 0; i < power.getValueI(); i++)
			BlockBreaker.breakBlocksWithPacketSpam(blocks);
		
		// disable
		setEnabled(false);
	}
	
	private ArrayList<BlockPos> getBlocksByDistanceReversed(double range)
	{
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double rangeSq = Math.pow(range + 0.5, 2);
		int rangeI = (int)Math.ceil(range);
		
		BlockPos center = new BlockPos(RotationUtils.getEyesPos());
		BlockPos min = center.add(-rangeI, -rangeI, -rangeI);
		BlockPos max = center.add(rangeI, rangeI, rangeI);
		
		return BlockUtils.getAllInBox(min, max).stream()
			.filter(pos -> eyesVec.squaredDistanceTo(new Vec3d(pos)) <= rangeSq)
			.sorted(Comparator.comparingDouble(
				pos -> -eyesVec.squaredDistanceTo(new Vec3d(pos))))
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
	}
}
