/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoStealHack;

import java.util.*;

@Mixin(GenericContainerScreen.class)
public abstract class ContainerScreen54Mixin
	extends HandledScreen<GenericContainerScreenHandler>
	implements ScreenHandlerProvider<GenericContainerScreenHandler>
{
	@Shadow
	@Final
	private int rows;
	
	private final AutoStealHack autoSteal =
		WurstClient.INSTANCE.getHax().autoStealHack;
	private int mode;
	
	public ContainerScreen54Mixin(WurstClient wurst,
		GenericContainerScreenHandler container,
		PlayerInventory playerInventory, Text name)
	{
		super(container, playerInventory, name);
	}
	
	@Override
	protected void init()
	{
		super.init();
		
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		if(autoSteal.areButtonsVisible())
		{
			addDrawableChild(new ButtonWidget(x + backgroundWidth - 160, y + 4,
				50, 12, new LiteralText("Sort"), b -> sort()));

			addDrawableChild(new ButtonWidget(x + backgroundWidth - 108, y + 4,
				50, 12, new LiteralText("Steal"), b -> steal()));
			
			addDrawableChild(new ButtonWidget(x + backgroundWidth - 56, y + 4,
				50, 12, new LiteralText("Store"), b -> store()));
		}
		
		if(autoSteal.isEnabled())
			steal();
	}
	
	private void steal()
	{
		runInThread(() -> shiftClickSlots(0, rows * 9, 1));
	}
	
	private void store()
	{
		runInThread(() -> shiftClickSlots(rows * 9, rows * 9 + 44, 2));
	}
	
	private void runInThread(Runnable r)
	{
		new Thread(() -> {
			try
			{
				r.run();
				
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}).start();
	}
	
	private void shiftClickSlots(int from, int to, int mode)
	{
		this.mode = mode;
		
		for(int i = from; i < to; i++)
		{
			Slot slot = handler.slots.get(i);
			if(slot.getStack().isEmpty())
				continue;
			
			waitForDelay();
			if(this.mode != mode || client.currentScreen == null)
				break;
			
			onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
		}
	}
	
	private void waitForDelay()
	{
		try
		{
			Thread.sleep(autoSteal.getDelay());
			
		}catch(InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}


	private void sort()
	{
		runInThread(() -> sortInThread(0, rows * 9, 3));
	}

	private void sortInThread(int from, int to, int mode)
	{
		this.mode = mode;

		final List<Integer> orderedSlots = getOrderedSlots(from, to);
		final Set<Integer> remainingSlots = new HashSet<>(orderedSlots);
		final Map<Integer, Integer> oldPosToNewPosMapping = getOldPosToNewPosMapping(orderedSlots);

		if(remainingSlots.stream().findFirst().isPresent())
			runInThread(() -> replaceSlot(remainingSlots, oldPosToNewPosMapping, -1));
	}

	/**
	 * Recursive method to sort a single Slot
	 * The break condition is `remainingSlots` being empty.
	 *
	 * @param remainingSlots A set of remaining slot positions that are not sorted yet
	 * @param oldPosToNewPosMapping A map binding old slot positions to their new sorted one
	 * @param cursorOldPosition The slot position from which the current Cursor Stack originated
	 */
	private void replaceSlot(final Set<Integer> remainingSlots, final Map<Integer, Integer> oldPosToNewPosMapping,
							 final int cursorOldPosition) {

		final int sourcePosition;
		final int targetPosition;
		ItemStack cursorStack = handler.getCursorStack();

		if(cursorStack.isEmpty())
		{
			final Optional<Integer> remainingSlotOpt = remainingSlots.stream().findFirst();
			if(remainingSlotOpt.isEmpty())
				return;

			final int nextSourcePosition = remainingSlotOpt.get();

			sourcePosition = nextSourcePosition;
			targetPosition = oldPosToNewPosMapping.get(nextSourcePosition);
		}
		else
		{
			sourcePosition = cursorOldPosition;
			targetPosition = oldPosToNewPosMapping.get(cursorOldPosition);
		}

		if(sourcePosition != targetPosition)
		{
			if(cursorStack.isEmpty()) {
				if(pickUpSlotWithDelay(sourcePosition))
					return;
			}
			if(pickUpSlotWithDelay(targetPosition))
				return;
		}

		remainingSlots.remove(sourcePosition);
		replaceSlot(remainingSlots, oldPosToNewPosMapping, targetPosition);
	}

	/**
	 * Consumes a list of ordered slot positions and produces a mapping of their current positions to their new one
	 *
	 * @param orderedSlots value example: [0, 1, 5, 6, 11, 12, 13, 15, 16, 17, 20, 22, 23]
	 * @return value example: {0=0, 1=1, 5=2, 6=3, 11=4, 12=5, 13=6, 15=7, 16=8, 17=9, 20=10, 22=11, 23=12}
	 */
	private Map<Integer, Integer> getOldPosToNewPosMapping(final List<Integer> orderedSlots) {
		final Map<Integer, Integer> oldPosToNewPosMapping = new HashMap<>();
		int counter = 0;

		for(int slotNum : orderedSlots)
		{
			oldPosToNewPosMapping.put(slotNum, counter);
			++counter;
		}

		return oldPosToNewPosMapping;
	}

	/**
	 * Produces a list of ordered slot positions containing items, sorted by item names.
	 *
	 * @param from Lower slot position bound (included)
	 * @param to Upper slot position bound (excluded)
	 * @return list of ordered slots
	 */
	private List<Integer> getOrderedSlots(final int from, final int to)
	{
		Map<String, List<Integer>> slotMap = getItemNameToSlotPositionsMap(from, to);
		List<Integer> orderedSlots = new LinkedList<>();

		for(String itemName : slotMap.keySet())
		{
			final List<Integer> sameItemNameSlotPositions = slotMap.get(itemName);

			for(int slot : sameItemNameSlotPositions)
				orderedSlots.add(slot);
		}

		return orderedSlots;
	}

	/**
	 *
	 * @param from Lower slot position bound (included)
	 * @param to Upper slot position bound (excluded)
	 * @return map of slot positions, sorted by item names
	 */
	private Map<String, List<Integer>> getItemNameToSlotPositionsMap(final int from, final int to)
	{
		Map<String, List<Integer>> itemNameToSlotPositionsMap = new HashMap<>();
		for(int i = from; i < to; ++i)
		{
			final Slot slot = handler.slots.get(i);

			if(slot.getStack().isEmpty())
				continue;

			final String itemName = slot.getStack().getItem().getName().getString();

			if(itemNameToSlotPositionsMap.containsKey(itemName))
				itemNameToSlotPositionsMap.get(itemName).add(i);
			else
				itemNameToSlotPositionsMap.put(itemName, new LinkedList<>(List.of(i)));
		}

		return itemNameToSlotPositionsMap;
	}

	/**
	 * Wrapper for pickup slot action with added delay and cancellation detection
	 *
	 * @param sourcePosition
	 * @return
	 */
	private boolean pickUpSlotWithDelay(int sourcePosition) {
		Slot oldSlot = handler.slots.get(sourcePosition);

		waitForDelay();

		if(mustCancelOperation(mode))
			return true;

		onMouseClick(oldSlot, oldSlot.id, 0, SlotActionType.PICKUP);
		return false;
	}

	/**
	 * The user chose a new action among the available ones (Sort, Steal, Sort), or closed the Container GUI.
	 *
	 * @param mode
	 * @return
	 */
	private boolean mustCancelOperation(int mode) {
		return this.mode != mode || client.currentScreen == null;
	}

}
