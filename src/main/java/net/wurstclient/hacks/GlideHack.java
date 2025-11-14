/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.events.AirStrafingSpeedListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;

public final class GlideHack extends Hack
	implements UpdateListener, AirStrafingSpeedListener
{
	private final SliderSetting fallSpeed = new SliderSetting("Fall speed",
		0.125, 0.005, 0.25, 0.005, ValueDisplay.DECIMAL);
	
	private final SliderSetting moveSpeed =
		new SliderSetting("Move speed", "Horizontal movement factor.", 1.2, 1,
			5, 0.05, ValueDisplay.PERCENTAGE);
	
	private final SliderSetting minHeight = new SliderSetting("Min height",
		"Won't glide when you are too close to the ground.", 0, 0, 2, 0.01,
		ValueDisplay.DECIMAL.withLabel(0, "disabled"));
	
	private final CheckboxSetting pauseOnSneak =
		new CheckboxSetting("Pause when sneaking", true);
	
	public GlideHack()
	{
		super("Glide");
		setCategory(Category.MOVEMENT);
		addSetting(fallSpeed);
		addSetting(moveSpeed);
		addSetting(minHeight);
		addSetting(pauseOnSneak);
	}
	
	@Override
	public String getRenderName()
	{
		LocalPlayer player = MC.player;
		if(player == null)
			return getName();
		
		if(pauseOnSneak.isChecked() && player.isShiftKeyDown())
			return getName() + " (paused)";
		
		return getName();
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(AirStrafingSpeedListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(AirStrafingSpeedListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		LocalPlayer player = MC.player;
		
		if(pauseOnSneak.isChecked() && player.isShiftKeyDown())
			return;
		
		Vec3 v = player.getDeltaMovement();
		
		if(player.onGround() || player.isInWater() || player.isInLava()
			|| player.onClimbable() || v.y >= 0)
			return;
		
		if(minHeight.getValue() > 0)
		{
			AABB box = player.getBoundingBox();
			box = box.minmax(box.move(0, -minHeight.getValue(), 0));
			if(!MC.level.noCollision(box))
				return;
			
			BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
			BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
			Stream<BlockPos> stream = StreamSupport
				.stream(BlockUtils.getAllInBox(min, max).spliterator(), true);
			
			// manual collision check, since liquids don't have bounding boxes
			if(stream.map(BlockUtils::getBlock)
				.anyMatch(LiquidBlock.class::isInstance))
				return;
		}
		
		player.setDeltaMovement(v.x, Math.max(v.y, -fallSpeed.getValue()), v.z);
	}
	
	@Override
	public void onGetAirStrafingSpeed(AirStrafingSpeedEvent event)
	{
		event.setSpeed(event.getDefaultSpeed() * moveSpeed.getValueF());
	}
}
