/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.List;
import java.util.stream.IntStream;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"auto steal", "ChestStealer", "chest stealer",
	"steal store buttons", "Steal/Store buttons"})
public final class AutoStealHack extends Hack
{
	private final SliderSetting delay = new SliderSetting("Delay",
		"Delay between moving stacks of items.\n"
			+ "Should be at least 70ms for NoCheat+ servers.",
		100, 0, 500, 10, ValueDisplay.INTEGER.withSuffix("ms"));
	
	private final CheckboxSetting buttons =
		new CheckboxSetting("Steal/Store buttons", true);
	
	private final CheckboxSetting reverseSteal =
		new CheckboxSetting("Reverse steal order", false);
	
	private Thread thread;
	
	public AutoStealHack()
	{
		super("AutoSteal");
		setCategory(Category.ITEMS);
		addSetting(buttons);
		addSetting(delay);
		addSetting(reverseSteal);
	}
	
	public void steal(HandledScreen<?> screen, int rows)
	{
		startClickingSlots(screen, 0, rows * 9, true);
	}
	
	public void store(HandledScreen<?> screen, int rows)
	{
		startClickingSlots(screen, rows * 9, rows * 9 + 36, false);
	}
	
	private void startClickingSlots(HandledScreen<?> screen, int from, int to,
		boolean steal)
	{
		if(thread != null && thread.isAlive())
			thread.interrupt();
		
		thread = Thread.ofPlatform().name("AutoSteal")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace()).daemon()
			.start(() -> shiftClickSlots(screen, from, to, steal));
	}
	
	private void shiftClickSlots(HandledScreen<?> screen, int from, int to,
		boolean steal)
	{
		List<Slot> slots = IntStream.range(from, to)
			.mapToObj(i -> screen.getScreenHandler().slots.get(i)).toList();
		
		if(reverseSteal.isChecked() && steal)
			slots = slots.reversed();
		
		for(Slot slot : slots)
			try
			{
				if(slot.getStack().isEmpty())
					continue;
				
				Thread.sleep(delay.getValueI());
				
				if(MC.currentScreen == null)
					break;
				
				screen.onMouseClick(slot, slot.id, 0,
					SlotActionType.QUICK_MOVE);
				
			}catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
				break;
			}
	}
	
	public boolean areButtonsVisible()
	{
		return buttons.isChecked();
	}
	
	// See GenericContainerScreenMixin and ShulkerBoxScreenMixin
}
