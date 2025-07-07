/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.*;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.InteractionSimulator;
import net.wurstclient.util.RenderUtils;

@DontSaveState
@SearchTags({"free camera", "spectator"})
public final class FreecamHack extends Hack implements UpdateListener,
	PacketOutputListener, IsPlayerInWaterListener, AirStrafingSpeedListener,
	IsPlayerInLavaListener, CameraTransformViewBobbingListener,
	IsNormalCubeListener, SetOpaqueCubeListener, RenderListener,
	RightClickListener, HandleBlockBreakingListener
{
	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting tracer = new CheckboxSetting("Tracer",
		"Draws a line to your character's actual position.", false);
	
	private final ColorSetting color =
		new ColorSetting("Tracer color", Color.WHITE);
	
	private final CheckboxSetting lockInteraction = new CheckboxSetting(
		"Lock Interaction",
		"Locks interactions to the real player position instead of the FreeCam's",
		false);
	
	private FakePlayerEntity fakePlayer;
	
	public FreecamHack()
	{
		super("Freecam");
		setCategory(Category.RENDER);
		addSetting(speed);
		addSetting(lockInteraction);
		addSetting(tracer);
		addSetting(color);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
		EVENTS.add(IsPlayerInLavaListener.class, this);
		EVENTS.add(AirStrafingSpeedListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(IsNormalCubeListener.class, this);
		EVENTS.add(SetOpaqueCubeListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(RightClickListener.class, this);
		EVENTS.add(HandleBlockBreakingListener.class, this);
		
		fakePlayer = new FakePlayerEntity();
		
		GameOptions opt = MC.options;
		KeyBinding[] bindings = {opt.forwardKey, opt.backKey, opt.leftKey,
			opt.rightKey, opt.jumpKey, opt.sneakKey};
		
		for(KeyBinding binding : bindings)
			IKeyBinding.get(binding).resetPressedState();
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
		EVENTS.remove(IsPlayerInLavaListener.class, this);
		EVENTS.remove(AirStrafingSpeedListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(IsNormalCubeListener.class, this);
		EVENTS.remove(SetOpaqueCubeListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
		EVENTS.remove(HandleBlockBreakingListener.class, this);
		
		fakePlayer.resetPlayerPosition();
		fakePlayer.despawn();
		
		ClientPlayerEntity player = MC.player;
		player.setVelocity(Vec3d.ZERO);
		
		MC.worldRenderer.reload();
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		player.setVelocity(Vec3d.ZERO);
		player.getAbilities().flying = false;
		
		player.setOnGround(false);
		Vec3d velocity = player.getVelocity();
		
		if(MC.options.jumpKey.isPressed())
			player.setVelocity(velocity.add(0, speed.getValue(), 0));
		
		if(MC.options.sneakKey.isPressed())
			player.setVelocity(velocity.subtract(0, speed.getValue(), 0));
	}
	
	@Override
	public void onGetAirStrafingSpeed(AirStrafingSpeedEvent event)
	{
		event.setSpeed(speed.getValueF());
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof PlayerMoveC2SPacket)
			event.cancel();
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		event.setInWater(false);
	}
	
	@Override
	public void onIsPlayerInLava(IsPlayerInLavaEvent event)
	{
		event.setInLava(false);
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(tracer.isChecked())
			event.cancel();
	}
	
	@Override
	public void onIsNormalCube(IsNormalCubeEvent event)
	{
		event.cancel();
	}
	
	@Override
	public void onSetOpaqueCube(SetOpaqueCubeEvent event)
	{
		event.cancel();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(fakePlayer == null || !tracer.isChecked())
			return;
		
		int colorI = color.getColorI(0x80);
		
		// box
		double extraSize = 0.05;
		Box box = fakePlayer.getBoundingBox().offset(0, extraSize, 0)
			.expand(extraSize);
		RenderUtils.drawOutlinedBox(matrixStack, box, colorI, false);
		
		// line
		RenderUtils.drawTracer(matrixStack, partialTicks,
			fakePlayer.getBoundingBox().getCenter(), colorI, false);
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		if(!lockInteraction.isChecked())
			return;
		
		event.cancel();
		
		if(MC.player.getItemUseTime() > 0)
			return;
		
		MC.itemUseCooldown = 4;
		
		HitResult hitResult = fakePlayer
			.raycast(5.0/* MC.player.getEntityInteractionRange() */, 0, false);
		
		if(hitResult.getType() == HitResult.Type.ENTITY)
		{
			EntityHitResult entityHitResult = (EntityHitResult)hitResult;
			for(Hand hand : Hand.values())
			{
				// general interaction
				ActionResult result = MC.interactionManager.interactEntity(
					MC.player, entityHitResult.getEntity(), hand);
				if(result.isAccepted())
				{
					MC.player.swingHand(hand);
					return;
				}
				// specific hit interaction
				result =
					MC.interactionManager.interactEntityAtLocation(MC.player,
						entityHitResult.getEntity(), entityHitResult, hand);
				if(result.isAccepted())
				{
					MC.player.swingHand(hand);
					return;
				}
			}
		}
		
		BlockHitResult blockHitResult =
			hitResult.getType() == HitResult.Type.BLOCK
				? (BlockHitResult)hitResult
				: new BlockHitResult(hitResult.getPos(), Direction.UP,
					BlockPos.ofFloored(hitResult.getPos()), false);
		
		InteractionSimulator.rightClickBlock(blockHitResult);
	}
	
	@Override
	public void onHandleBlockBreaking(HandleBlockBreakingEvent event)
	{
		if(!lockInteraction.isChecked())
			return;
		
		event.cancel();
		
		if(!MC.options.attackKey.isPressed() || MC.player.isUsingItem())
		{
			MC.interactionManager.cancelBlockBreaking();
			return;
		}
		HitResult hitResult = fakePlayer
			.raycast(5.0/* MC.player.getEntityInteractionRange() */, 0, false);
		if(hitResult.getType() != HitResult.Type.BLOCK)
		{
			MC.interactionManager.cancelBlockBreaking();
			return;
		}
		
		BlockHitResult blockHitResult = (BlockHitResult)hitResult;
		BlockPos pos = blockHitResult.getBlockPos();
		MC.interactionManager.updateBlockBreakingProgress(pos,
			blockHitResult.getSide());
	}
}
