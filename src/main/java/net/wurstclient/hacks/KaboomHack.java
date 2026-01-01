/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

public final class KaboomHack extends Hack implements UpdateListener
{
	private final SliderSetting power =
		new SliderSetting("Power", "description.wurst.setting.kaboom.power",
			128, 32, 512, 32, ValueDisplay.INTEGER);
	
	private final CheckboxSetting sound = new CheckboxSetting("Sound",
		"description.wurst.setting.kaboom.sound", true);
	
	private final CheckboxSetting particles = new CheckboxSetting("Particles",
		"description.wurst.setting.kaboom.particles", true);
	
	private final RandomSource random = RandomSource.create();
	
	public KaboomHack()
	{
		super("Kaboom");
		setCategory(Category.BLOCKS);
		addSetting(power);
		addSetting(sound);
		addSetting(particles);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// Abort if flying to prevent getting kicked
		if(!MC.player.getAbilities().instabuild && !MC.player.onGround())
			return;
		
		double x = MC.player.getX();
		double y = MC.player.getY();
		double z = MC.player.getZ();
		
		// Do explosion effect
		if(sound.isChecked())
		{
			float soundPitch =
				(1F + (random.nextFloat() - random.nextFloat()) * 0.2F) * 0.7F;
			MC.level.playLocalSound(x, y, z,
				SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4,
				soundPitch, false);
		}
		if(particles.isChecked())
			MC.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0,
				0);
		
		// Break all blocks
		ArrayList<BlockPos> blocks = getBlocksByDistanceReversed();
		for(int i = 0; i < power.getValueI(); i++)
			BlockBreaker.breakBlocksWithPacketSpam(blocks);
		
		setEnabled(false);
	}
	
	private ArrayList<BlockPos> getBlocksByDistanceReversed()
	{
		Vec3 eyesVec = RotationUtils.getEyesPos();
		BlockPos eyesBlock = BlockPos.containing(eyesVec);
		double rangeSq = 36;
		int blockRange = 6;
		
		// farthest blocks first
		return BlockUtils.getAllInBoxStream(eyesBlock, blockRange)
			.filter(pos -> pos.distToCenterSqr(eyesVec) <= rangeSq)
			.sorted(Comparator
				.comparingDouble(pos -> -pos.distToCenterSqr(eyesVec)))
			.collect(Collectors.toCollection(ArrayList::new));
	}
}
