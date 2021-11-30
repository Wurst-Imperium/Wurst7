/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.screen.ScreenHandler;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class AutoStealHandledScreenMixin<T extends ScreenHandler>
	extends Screen
	implements ScreenHandlerProvider<T>
{
	@Shadow
	private int backgroundWidth;

	@Shadow
	private int backgroundHeight;

	@Shadow
	private int x;

	@Shadow
	private int y;

	@Shadow
	@Final
	private T handler;

	@Shadow
	private void shadow$onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {}

	private final AutoStealHack autoSteal =
		WurstClient.INSTANCE.getHax().autoStealHack;
	private int mode;

	public AutoStealHandledScreenMixin(Text name)
	{
		super(name);
	}

	private boolean isTypeEnabled() {
		Screen screenObj = (Screen)this;
		return screenObj instanceof GenericContainerScreen || screenObj instanceof ShulkerBoxScreen;
	}

	@Inject(at = @At("TAIL"), method = "init")
	protected void init(CallbackInfo info)
	{
		if(!WurstClient.INSTANCE.isEnabled() || !isTypeEnabled())
			return;
		
		if(autoSteal.areButtonsVisible())
		{
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
		runInThread(() -> shiftClickSlots(0, handler.slots.size() - 36, 1));
	}
	
	private void store()
	{
		runInThread(() -> shiftClickSlots(handler.slots.size() - 36, handler.slots.size(), 2));
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

			shadow$onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
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
}
