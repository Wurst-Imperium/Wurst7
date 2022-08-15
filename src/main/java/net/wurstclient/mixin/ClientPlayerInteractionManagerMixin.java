/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.BlockBreakingProgressListener.BlockBreakingProgressEvent;
import net.wurstclient.events.StopUsingItemListener.StopUsingItemEvent;
import net.wurstclient.hack.HackList;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin
	implements IClientPlayerInteractionManager
{
	@Shadow
	private MinecraftClient client;
	@Shadow
	private float currentBreakingProgress;
	@Shadow
	private boolean breakingBlock;
	
	/**
	 * blockHitDelay
	 */
	@Shadow
	private int blockBreakingCooldown;
	
	@Inject(at = {@At(value = "INVOKE",
		target = "Lnet/minecraft/client/network/ClientPlayerEntity;getId()I",
		ordinal = 0)},
		method = {
			"updateBlockBreakingProgress(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"})
	private void onPlayerDamageBlock(BlockPos blockPos_1, Direction direction_1,
		CallbackInfoReturnable<Boolean> cir)
	{
		BlockBreakingProgressEvent event =
			new BlockBreakingProgressEvent(blockPos_1, direction_1);
		EventManager.fire(event);
	}
	
	@Inject(at = {@At("HEAD")},
		method = {"getReachDistance()F"},
		cancellable = true)
	private void onGetReachDistance(CallbackInfoReturnable<Float> ci)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.reachHack.isEnabled())
			return;
		
		ci.setReturnValue(hax.reachHack.getReachDistance());
	}
	
	@Inject(at = {@At("HEAD")},
		method = {"hasExtendedReach()Z"},
		cancellable = true)
	private void hasExtendedReach(CallbackInfoReturnable<Boolean> cir)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.reachHack.isEnabled())
			return;
		
		cir.setReturnValue(true);
	}
	
	@Inject(at = {@At("HEAD")},
		method = "stopUsingItem(Lnet/minecraft/entity/player/PlayerEntity;)V")
	private void onStopUsingItem(PlayerEntity player, CallbackInfo ci)
	{
		EventManager.fire(StopUsingItemEvent.INSTANCE);
	}
	
	@Override
	public float getCurrentBreakingProgress()
	{
		return currentBreakingProgress;
	}
	
	@Override
	public void setBreakingBlock(boolean breakingBlock)
	{
		this.breakingBlock = breakingBlock;
	}
	
	@Override
	public void windowClick_PICKUP(int slot)
	{
		clickSlot(0, slot, 0, SlotActionType.PICKUP, client.player);
	}
	
	@Override
	public void windowClick_QUICK_MOVE(int slot)
	{
		clickSlot(0, slot, 0, SlotActionType.QUICK_MOVE, client.player);
	}
	
	@Override
	public void windowClick_THROW(int slot)
	{
		clickSlot(0, slot, 1, SlotActionType.THROW, client.player);
	}
	
	@Override
	public void rightClickItem()
	{
		interactItem(client.player, Hand.MAIN_HAND);
	}
	
	@Override
	public void rightClickBlock(BlockPos pos, Direction side, Vec3d hitVec)
	{
		interactBlock(client.player, Hand.MAIN_HAND,
			new BlockHitResult(hitVec, side, pos, false));
		interactItem(client.player, Hand.MAIN_HAND);
	}
	
	@Override
	public void sendPlayerActionC2SPacket(Action action, BlockPos blockPos,
		Direction direction)
	{
		sendSequencedPacket(client.world,
			i -> new PlayerActionC2SPacket(action, blockPos, direction, i));
	}
	
	@Override
	public void sendPlayerInteractBlockPacket(Hand hand,
		BlockHitResult blockHitResult)
	{
		sendSequencedPacket(client.world,
			i -> new PlayerInteractBlockC2SPacket(hand, blockHitResult, i));
	}
	
	@Override
	public void setBlockHitDelay(int delay)
	{
		blockBreakingCooldown = delay;
	}
	
	@Shadow
	private void sendSequencedPacket(ClientWorld world,
		SequencedPacketCreator packetCreator)
	{
		
	}
	
	@Shadow
	public abstract ActionResult interactBlock(
		ClientPlayerEntity clientPlayerEntity_1, Hand hand_1,
		BlockHitResult blockHitResult_1);
	
	@Shadow
	public abstract ActionResult interactItem(PlayerEntity playerEntity_1,
		Hand hand_1);
	
	@Shadow
	public abstract void clickSlot(int syncId, int slotId, int clickData,
		SlotActionType actionType, PlayerEntity playerEntity);
}
