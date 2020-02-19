/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;

public final class GlideHack extends Hack implements UpdateListener
{
	private final SliderSetting fallSpeed = new SliderSetting("Fall speed",
		0.125, 0.005, 0.25, 0.005, ValueDisplay.DECIMAL);
	
	private final SliderSetting moveSpeed =
		new SliderSetting("Move speed", "Horizontal movement factor.", 1.2, 1,
			5, 0.05, ValueDisplay.PERCENTAGE);
	
	private final SliderSetting minHeight = new SliderSetting("Min height",
		"Won't glide when you are\n" + "too close to the ground.", 0, 0, 2,
		0.01,
		v -> v == 0 ? "disabled" : ValueDisplay.DECIMAL.getValueString(v));
	
	public GlideHack()
	{
		super("Glide", "Makes you glide down slowly when falling.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r You will take fall damage if you don't use NoFall.");
		
		setCategory(Category.MOVEMENT);
		addSetting(fallSpeed);
		addSetting(moveSpeed);
		addSetting(minHeight);
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
		ClientPlayerEntity player = MC.player;
		Vec3d v = player.getVelocity();
		
		if(player.onGround || player.isTouchingWater() || player.isInLava()
			|| player.isClimbing() || v.y >= 0)
			return;
		
		if(minHeight.getValue() > 0)
		{
			Box box = player.getBoundingBox();
			box = box.union(box.offset(0, -minHeight.getValue(), 0));
			if(!MC.world.doesNotCollide(box))
				return;
			
			BlockPos min = new BlockPos(new Vec3d(box.x1, box.y1, box.z1));
			BlockPos max = new BlockPos(new Vec3d(box.x2, box.y2, box.z2));
			Stream<BlockPos> stream = StreamSupport
				.stream(BlockUtils.getAllInBox(min, max).spliterator(), true);
			
			// manual collision check, since liquids don't have bounding boxes
			if(stream.map(BlockUtils::getState).map(BlockState::getMaterial)
				.anyMatch(Material::isLiquid))
				return;
		}
		
		player.setVelocity(v.x, Math.max(v.y, -fallSpeed.getValue()), v.z);
		player.flyingSpeed *= moveSpeed.getValueF();
	}
}
