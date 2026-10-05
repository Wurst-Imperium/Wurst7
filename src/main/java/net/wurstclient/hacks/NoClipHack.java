/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.LinkedHashSet;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.noclip.NoClipRenderer;
import net.wurstclient.hacks.noclip.NoClipRenderer.JumpToRender;
import net.wurstclient.mixinterface.IKeyMapping;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"no clip"})
public final class NoClipHack extends Hack
	implements UpdateListener, RenderListener
{
	// Limited by ServerGamePacketListenerImpl.handleMovePlayer()
	public static final double MAX_DISTANCE_WITHOUT_ELYTRA = Math.sqrt(500);
	public static final double MAX_DISTANCE = Math.sqrt(1500);
	
	private final CheckboxSetting showPassableBlocks =
		new CheckboxSetting("Show passable blocks",
			"description.wurst.setting.noclip.show_passable_blocks", true);
	
	private final Set<JumpToRender> jumpsToRender = new LinkedHashSet<>();
	
	private Vec3 direction = Vec3.ZERO;
	private int preJumpTicks;
	private int postJumpTicks;
	private boolean needsButtonRelease;
	
	public NoClipHack()
	{
		super("NoClip");
		setCategory(Category.MOVEMENT);
		addSetting(showPassableBlocks);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		jumpsToRender.clear();
		direction = Vec3.ZERO;
		preJumpTicks = 0;
		postJumpTicks = 0;
		needsButtonRelease = false;
	}
	
	@Override
	public void onUpdate()
	{
		jumpsToRender.clear();
		if(showPassableBlocks.isChecked()
			&& MC.player.pick(64, 0, false) instanceof BlockHitResult hit)
			jumpsToRender
				.addAll(NoClipRenderer.findJumpsNear(hit.getBlockPos()));
		
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
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(showPassableBlocks.isChecked())
			NoClipRenderer.renderJumps(matrixStack, jumpsToRender);
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
		if(!player.isAlive() || player.isSpectator() || MC.screen != null
			|| WURST.getHax().freecamHack.isMovingCamera()
			|| MC.level.noBlockCollision(player,
				player.getBoundingBox().deflate(1e-5))
			|| needsButtonRelease || direction.lengthSqr() == 0)
		{
			preJumpTicks = 0;
			return true;
		}
		
		preJumpTicks++;
		if(preJumpTicks <= 2)
			return false;
		preJumpTicks = 0;
		
		needsButtonRelease = true;
		boolean canUseElytra = !player.isFallFlying() && !player.isInWater()
			&& !player.getAbilities().flying
			&& !player.hasEffect(MobEffects.LEVITATION)
			&& EquipmentSlot.VALUES.stream().anyMatch(slot -> LivingEntity
				.canGlideUsing(player.getItemBySlot(slot), slot));
		
		Vec3 destination = findDestination(direction, canUseElytra);
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
	
	private Vec3 findDestination(Vec3 direction, boolean canUseElytra)
	{
		double distance = 0;
		double maxDistance =
			canUseElytra ? MAX_DISTANCE : MAX_DISTANCE_WITHOUT_ELYTRA;
		
		while(distance < maxDistance)
		{
			distance = Math.min(distance + 0.25, maxDistance);
			Vec3 offset = direction.scale(distance);
			AABB box = MC.player.getBoundingBox().move(offset);
			
			if(!BlockPos.betweenClosedStream(box.deflate(1e-7))
				.allMatch(MC.level::isLoaded))
				return null;
				
			// noEntityCollision() only rejects entities with solid collision,
			// like boats. Normal entity collisions are allowed.
			if(MC.level.noBlockCollision(MC.player, box)
				&& MC.level.noEntityCollision(MC.player, box))
				return MC.player.position().add(offset);
		}
		
		return null;
	}
}
