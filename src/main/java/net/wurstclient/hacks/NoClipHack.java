/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyMapping;

@SearchTags({"no clip"})
public final class NoClipHack extends Hack implements UpdateListener
{
	private Vec3 direction = Vec3.ZERO;
	private int preJumpTicks;
	private int postJumpTicks;
	private boolean needsButtonRelease;
	
	public NoClipHack()
	{
		super("NoClip");
		setCategory(Category.MOVEMENT);
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
		direction = Vec3.ZERO;
		preJumpTicks = 0;
		postJumpTicks = 0;
		needsButtonRelease = false;
	}
	
	@Override
	public void onUpdate()
	{
		double forward = (MC.options.keyUp.isDown() ? 1 : 0)
			- (MC.options.keyDown.isDown() ? 1 : 0);
		double left = (MC.options.keyLeft.isDown() ? 1 : 0)
			- (MC.options.keyRight.isDown() ? 1 : 0);
		double up = (MC.options.keyJump.isDown() ? 1 : 0)
			- (IKeyMapping.get(MC.options.keyShift).isActuallyDown() ? 1 : 0);
		
		direction = new Vec3(left, up, forward).normalize()
			.yRot(-MC.player.getYRot() * Mth.DEG_TO_RAD);
		if(direction.lengthSqr() == 0)
			needsButtonRelease = false;
	}
	
	public boolean shouldSendPosition()
	{
		if(!isEnabled())
			return true;
		
		if(postJumpTicks > 0)
		{
			postJumpTicks--;
			return false;
		}
		
		LocalPlayer player = MC.player;
		if(!player.isAlive() || player.isPassenger() || player.isSpectator()
			|| MC.gui.screen() != null
			|| WURST.getHax().freecamHack.isMovingCamera()
			|| MC.level.noBlockCollision(player,
				player.getBoundingBox().deflate(1e-5))
			|| needsButtonRelease || direction.lengthSqr() == 0)
		{
			preJumpTicks = 0;
			return true;
		}
		
		if(preJumpTicks++ <= 2)
			return false;
		preJumpTicks = 0;
		
		needsButtonRelease = true;
		boolean canUseElytra =
			!player.isFallFlying() && !player.isInWater()
				&& !player.getAbilities().flying
				&& !player.hasEffect(MobEffects.LEVITATION)
				&& LivingEntity.canGlideUsing(
					player.getItemBySlot(EquipmentSlot.CHEST),
					EquipmentSlot.CHEST);
		double maxDistance = canUseElytra ? 38 : 22;
		
		Vec3 destination = findDestination(direction, maxDistance);
		if(destination == null)
			return true;
		
		Connection connection = player.connection.getConnection();
		for(int i = 0; i < 4; i++)
			connection.send(
				new ServerboundMovePlayerPacket.StatusOnly(false, false), null,
				false);
		
		if(canUseElytra)
			connection.send(
				new ServerboundPlayerCommandPacket(player,
					ServerboundPlayerCommandPacket.Action.START_FALL_FLYING),
				null, false);
			
		// Vanilla sends the final packet, flushes the batch and
		// remembers the position
		player.setPos(destination);
		postJumpTicks = 2;
		return true;
	}
	
	private Vec3 findDestination(Vec3 direction, double maxDistance)
	{
		LocalPlayer player = MC.player;
		for(double distance = 0.25; distance <= maxDistance; distance += 0.25)
		{
			Vec3 offset = direction.scale(distance);
			AABB box = player.getBoundingBox().move(offset);
			
			if(!MC.level.getWorldBorder().isWithinBounds(box)
				|| !BlockPos.betweenClosedStream(box.deflate(1e-7))
					.allMatch(MC.level::isLoaded))
				return null;
			
			if(MC.level.noCollision(player, box))
				return player.position().add(offset);
		}
		
		return null;
	}
}
