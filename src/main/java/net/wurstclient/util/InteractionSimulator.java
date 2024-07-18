/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.SwingHandSetting.SwingHand;

/**
 * A utility class to turn right-clicking a block into a simple one-liner,
 * without sacrificing anti-cheat resistance or customizability.
 *
 * <p>
 * Accurately replicates {@link MinecraftClient#doItemUse()} as of 1.20.2, while
 * being much easier to read and adding convenient ways to change parts of the
 * behavior.
 */
public enum InteractionSimulator
{
	;
	
	private static final MinecraftClient MC = WurstClient.MC;
	
	/**
	 * @see #rightClickBlock(BlockHitResult, SwingHand)
	 */
	public static void rightClickBlock(BlockHitResult hitResult)
	{
		rightClickBlock(hitResult, SwingHand.CLIENT);
	}
	
	/**
	 * Right-clicks the block at the given hit result, which may end up placing
	 * a block, interacting with an existing block, or using an equipped item.
	 *
	 * <p>
	 * This method automatically decides which hand to use in order to match
	 * vanilla behavior as closely as possible. If you need to force a specific
	 * hand, use {@link #rightClickBlock(BlockHitResult, Hand, SwingHand)}
	 * instead.
	 *
	 * <p>
	 * To fully match vanilla behavior, do the following before calling this
	 * method:
	 * <ol>
	 * <li>Face the block and ensure that there are no other blocks or entities
	 * preventing line of sight.</li>
	 * <li>Ensure that {@code MC.interactionManager.isBreakingBlock()} returns
	 * {@code false}.</li>
	 * <li>Set the item use cooldown to 4 ticks. (Yes, even if subsequent checks
	 * fail and the interaction doesn't happen.)</li>
	 * <li>Ensure that {@code MC.player.isRiding()} returns {@code false}.</li>
	 * </ol>
	 */
	public static void rightClickBlock(BlockHitResult hitResult,
		SwingHand swing)
	{
		for(Hand hand : Hand.values())
		{
			ItemStack stack = MC.player.getStackInHand(hand);
			if(interactBlockAndSwing(hitResult, swing, hand, stack))
				return;
			
			if(interactItemAndSwing(stack, swing, hand))
				return;
		}
	}
	
	/**
	 * @see #rightClickBlock(BlockHitResult, Hand, SwingHand)
	 */
	public static void rightClickBlock(BlockHitResult hitResult, Hand hand)
	{
		rightClickBlock(hitResult, hand, SwingHand.CLIENT);
	}
	
	/**
	 * Right-clicks the block at the given hit result, which may end up placing
	 * a block, interacting with an existing block, or using an equipped item.
	 *
	 * <p>
	 * This method forces the specified hand to be used, which would not be
	 * possible in vanilla. For a more realistic right-click simulation, use
	 * {@link #rightClickBlock(BlockHitResult, SwingHand)} instead.
	 */
	public static void rightClickBlock(BlockHitResult hitResult, Hand hand,
		SwingHand swing)
	{
		ItemStack stack = MC.player.getStackInHand(hand);
		if(interactBlockAndSwing(hitResult, swing, hand, stack))
			return;
		
		interactItemAndSwing(stack, swing, hand);
	}
	
	/**
	 * Calls {@code interactBlock()} and swings the hand if the game would
	 * normally do that.
	 *
	 * @return {@code true} if this call should consume the click and prevent
	 *         any further block/item interactions
	 */
	private static boolean interactBlockAndSwing(BlockHitResult hitResult,
		SwingHand swing, Hand hand, ItemStack stack)
	{
		// save old stack size and call interactBlock()
		int oldCount = stack.getCount();
		ActionResult result =
			MC.interactionManager.interactBlock(MC.player, hand, hitResult);
		
		// swing hand and reset equip animation
		if(result.shouldSwingHand())
		{
			swing.swing(hand);
			
			if(!stack.isEmpty() && (stack.getCount() != oldCount
				|| MC.interactionManager.hasCreativeInventory()))
				MC.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
		}
		
		return result != ActionResult.PASS;
	}
	
	/**
	 * Calls {@code interactItem()} and swings the hand if the game would
	 * normally do that.
	 *
	 * @return {@code true} if this call should consume the click and prevent
	 *         any further block/item interactions
	 */
	private static boolean interactItemAndSwing(ItemStack stack,
		SwingHand swing, Hand hand)
	{
		// pass if hand is empty
		if(stack.isEmpty())
			return false;
		
		// call interactItem()
		ActionResult result =
			MC.interactionManager.interactItem(MC.player, hand);
		
		// swing hand
		if(result.shouldSwingHand())
			swing.swing(hand);
		
		// reset equip animation
		if(result.isAccepted())
		{
			MC.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
			return true;
		}
		
		return false;
	}
}
