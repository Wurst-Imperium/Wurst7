/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.BlockHitResult;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.InteractSwingSetting.InteractSwing;

/**
 * A utility class to turn right-clicking a block into a simple one-liner,
 * without sacrificing anti-cheat resistance or customizability.
 *
 * <p>
 * Accurately replicates {@link Minecraft#startUseItem()} as of 26.3-snapshot-7
 * while being much easier to read and adding convenient ways to change parts of
 * the behavior.
 */
public enum InteractionSimulator
{
	;
	
	private static final Minecraft MC = WurstClient.MC;
	
	/**
	 * @see #rightClickBlock(BlockHitResult, InteractSwing)
	 */
	public static void rightClickBlock(BlockHitResult hitResult)
	{
		rightClickBlock(hitResult, InteractSwing.CLIENT);
	}
	
	/**
	 * Right-clicks the block at the given hit result, which may end up placing
	 * a block, interacting with an existing block, or using an equipped item.
	 *
	 * <p>
	 * This method automatically decides which hand to use in order to match
	 * vanilla behavior as closely as possible. If you need to force a specific
	 * hand, use
	 * {@link #rightClickBlock(BlockHitResult, InteractionHand, InteractSwing)}
	 * instead.
	 *
	 * <p>
	 * To fully match vanilla behavior, do the following before calling this
	 * method:
	 * <ol>
	 * <li>Face the block and ensure that there are no other blocks or entities
	 * preventing line of sight.</li>
	 * <li>Ensure that {@code MC.gameMode.isDestroying()} returns
	 * {@code false}.</li>
	 * <li>Set {@code MC.rightClickDelay} to 4 ticks. (Yes, even if subsequent
	 * checks fail and the interaction doesn't happen.)</li>
	 * <li>Ensure that {@code MC.player.isHandsBusy()} returns
	 * {@code false}.</li>
	 * </ol>
	 */
	public static void rightClickBlock(BlockHitResult hitResult,
		InteractSwing swing)
	{
		for(InteractionHand hand : InteractionHand.values())
		{
			ItemStack stack = MC.player.getItemInHand(hand);
			if(!stack.isItemEnabled(MC.level.enabledFeatures()))
				return;
			
			if(useItemOnAndSwing(hitResult, swing, hand, stack))
				return;
			
			if(useItemAndSwing(stack, swing, hand))
				return;
		}
	}
	
	/**
	 * @see #rightClickBlock(BlockHitResult, InteractionHand, InteractSwing)
	 */
	public static void rightClickBlock(BlockHitResult hitResult,
		InteractionHand hand)
	{
		rightClickBlock(hitResult, hand, InteractSwing.CLIENT);
	}
	
	/**
	 * Right-clicks the block at the given hit result, which may end up placing
	 * a block, interacting with an existing block, or using an equipped item.
	 *
	 * <p>
	 * This method forces the specified hand to be used, which would not be
	 * possible in vanilla. For a more realistic right-click simulation, use
	 * {@link #rightClickBlock(BlockHitResult, InteractSwing)} instead.
	 */
	public static void rightClickBlock(BlockHitResult hitResult,
		InteractionHand hand, InteractSwing swing)
	{
		ItemStack stack = MC.player.getItemInHand(hand);
		if(useItemOnAndSwing(hitResult, swing, hand, stack))
			return;
		
		useItemAndSwing(stack, swing, hand);
	}
	
	/**
	 * Calls {@code useItemOn()} and swings the hand if the game would normally
	 * do that.
	 *
	 * @return {@code true} if this call should consume the click and prevent
	 *         any further block/item interactions
	 */
	private static boolean useItemOnAndSwing(BlockHitResult hitResult,
		InteractSwing swing, InteractionHand hand, ItemStack stack)
	{
		// save animation and old stack size, then call useItemOn()
		SwingAnimation swingAnimation = stack.getInteractAnimation();
		int oldCount = stack.getCount();
		InteractionResult result =
			MC.gameMode.useItemOn(MC.player, hand, hitResult);
		
		// swing hand and notify the item-in-hand renderer
		if(result instanceof InteractionResult.Success success
			&& success.swingSource() == InteractionResult.SwingSource.PREDICTED)
		{
			swing.swing(hand, swingAnimation);
			
			if(!stack.isEmpty() && (stack.getCount() != oldCount
				|| MC.player.hasInfiniteMaterials()))
				MC.gameRenderer.itemInHandRenderer.itemUsed(hand);
		}
		
		return result instanceof InteractionResult.Success
			|| result instanceof InteractionResult.Fail;
	}
	
	/**
	 * Calls {@code useItem()} and swings the hand if the game would normally do
	 * that.
	 *
	 * @return {@code true} if this call should consume the click and prevent
	 *         any further block/item interactions
	 */
	private static boolean useItemAndSwing(ItemStack stack, InteractSwing swing,
		InteractionHand hand)
	{
		// pass if hand is empty
		if(stack.isEmpty())
			return false;
		
		// save animation and call useItem()
		SwingAnimation swingAnimation = stack.getInteractAnimation();
		InteractionResult result = MC.gameMode.useItem(MC.player, hand);
		
		if(!(result instanceof InteractionResult.Success success))
			return false;
		
		// swing hand
		if(success.swingSource() == InteractionResult.SwingSource.PREDICTED)
			swing.swing(hand, swingAnimation);
		
		// notify the item-in-hand renderer
		MC.gameRenderer.itemInHandRenderer.itemUsed(hand);
		return true;
	}
}
