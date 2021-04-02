/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.block.Material;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.events.KeyPressListener.KeyPressEvent;
import net.wurstclient.hacks.AutoStealHack;
import net.wurstclient.hacks.AutoMineHack;
import net.wurstclient.hacks.ShulkerDupeHack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Mixin(ShulkerBoxScreen.class)
public abstract class ShulkerBoxScreenMixin
	extends HandledScreen<ShulkerBoxScreenHandler>
	implements ScreenHandlerProvider<ShulkerBoxScreenHandler>
{
	private final int rows = 3;
	
	private final AutoStealHack autoSteal =
		WurstClient.INSTANCE.getHax().autoStealHack;
	private final AutoMineHack autoMine =
		WurstClient.INSTANCE.getHax().autoMineHack;
	private final ShulkerDupeHack shulkerDupe =
		WurstClient.INSTANCE.getHax().shulkerDupeHack;
	private int mode;
	
	
	public ShulkerBoxScreenMixin(WurstClient wurst,
		ShulkerBoxScreenHandler container, PlayerInventory playerInventory,
		Text name)
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
			addButton(new ButtonWidget(x + backgroundWidth - 108, y + 4, 50, 12,
				new LiteralText("Steal"), b -> steal()));
			
			addButton(new ButtonWidget(x + backgroundWidth - 56, y + 4, 50, 12,
				new LiteralText("Store"), b -> store()));
		}
		if(shulkerDupe.areButtonsVisible())
		{
			addButton(new ButtonWidget(x + backgroundWidth - 108, y + 70, 50, 12,
			new LiteralText("Dupe"), b -> dupe()));
		}
		
		if(autoSteal.isEnabled())
			steal();
		else if(shulkerDupe.isEnabled())
			dupe();
	}
	
	private void dupe()
	{
		runInThread(() -> dupeAction(1));
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
			
			waitForDelay(autoSteal.getDelay());
			if(this.mode != mode || client.currentScreen == null)
				break;
			
			onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
		}
	}
	
	private void waitForDelay(long delaytime)
	{
		try
		{
			Thread.sleep(delaytime);
			
		}catch(InterruptedException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void swapSlots(int from, int hotbar)
	{
			Slot slot = handler.slots.get(from);
			onMouseClick(slot, slot.id, hotbar, SlotActionType.SWAP);
	}

	private void dupeAction(int mode)
	{
		this.mode = mode;
		if((handler.slots.get(0).getStack().isEmpty() && WurstClient.MC.player.getInventory().getStack(0).isEmpty()) || (!handler.slots.get(0).getStack().isEmpty() && !WurstClient.MC.player.getInventory().getStack(0).isEmpty()))
		{
				shulkerDupe.setEnabled(false);
		}
		autoMine.setEnabled(true);
		for(int i =0; i<999; i++)
		{
			if(this.mode != mode || client.currentScreen == null)
				break;
			swapSlots(0, 0);
			waitForDelay(shulkerDupe.getDelay());
		}
		autoMine.setEnabled(false);
		waitForDelay(1200L);
		boolean hasBox = false;
		for(int slot = 0; slot < 9; slot++)
		{
			ItemStack stack = WurstClient.MC.player.getInventory().getStack(slot);
			if(stack.getItem() == Items.SHULKER_BOX || stack.getItem() == Items.BLACK_SHULKER_BOX || stack.getItem() == Items.BLUE_SHULKER_BOX || stack.getItem() == Items.BROWN_SHULKER_BOX || stack.getItem() == Items.CYAN_SHULKER_BOX || stack.getItem() == Items.GRAY_SHULKER_BOX || stack.getItem() == Items.GREEN_SHULKER_BOX || stack.getItem() == Items.LIGHT_BLUE_SHULKER_BOX || stack.getItem() == Items.LIGHT_GRAY_SHULKER_BOX || stack.getItem() == Items.LIME_SHULKER_BOX || stack.getItem() == Items.MAGENTA_SHULKER_BOX || stack.getItem() == Items.ORANGE_SHULKER_BOX || stack.getItem() == Items.PINK_SHULKER_BOX || stack.getItem() == Items.PURPLE_SHULKER_BOX || stack.getItem() == Items.RED_SHULKER_BOX || stack.getItem() == Items.WHITE_SHULKER_BOX || stack.getItem() == Items.YELLOW_SHULKER_BOX)
			{
				WurstClient.MC.player.getInventory().selectedSlot = slot;
				hasBox = true;
			}
		}
		
		if(hasBox){
		WurstClient.IMC.getInteractionManager().rightClickBlock(WurstClient.MC.player.getBlockPos().add(1,0,0),
			Direction.UP, Vec3d.ofCenter(WurstClient.MC.player.getBlockPos()).add(Vec3d.of(Direction.UP.getVector()).multiply(0.5)));
		WurstClient.MC.player.swingHand(Hand.MAIN_HAND);
		WurstClient.IMC.setItemUseCooldown(4);
		WurstClient.IMC.getInteractionManager().rightClickBlock(WurstClient.MC.player.getBlockPos().add(1,0,0),
		Direction.UP, Vec3d.ofCenter(WurstClient.MC.player.getBlockPos()).add(Vec3d.of(Direction.UP.getVector()).multiply(0.5)));
		}
	}
}
