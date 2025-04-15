/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

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
	
	private Mode mode;
	
	public AutoStealHack()
	{
		super("AutoSteal");
		setCategory(Category.ITEMS);
		addSetting(buttons);
		addSetting(delay);
	}
	
	public void steal(HandledScreen<?> screen, int rows)
	{
		runInThread(() -> shiftClickSlots(screen, 0, rows * 9, Mode.STEAL));
	}
	
	public void store(HandledScreen<?> screen, int rows)
	{
		runInThread(
			() -> shiftClickSlots(screen, rows * 9, rows * 9 + 44, Mode.STORE));
	}
	
	private void runInThread(Runnable r)
	{
		Thread.ofVirtual().name("AutoSteal")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace()).start(r);
	}
	
	private void shiftClickSlots(HandledScreen<?> screen, int from, int to,
		Mode mode)
	{
		this.mode = mode;
		
		for(int i = from; i < to; i++)
		{
			Slot slot = screen.getScreenHandler().slots.get(i);
			if(slot.getStack().isEmpty())
				continue;
			
			waitForDelay();
			if(this.mode != mode || MC.currentScreen == null)
				break;
			
			screen.onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
		}
	}
	
	private void waitForDelay()
	{
		try
		{
			Thread.sleep(delay.getValueI());
			
		}catch(InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public boolean areButtonsVisible()
	{
		return buttons.isChecked();
	}
	
	private enum Mode
	{
		STEAL,
		STORE;
	}
	
	// See GenericContainerScreenMixin and ShulkerBoxScreenMixin
}
