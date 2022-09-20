/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.autofish.AutoFishDebugDraw;
import net.wurstclient.mixinterface.IFishingBobberEntity;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;

@SearchTags({"FishBot", "auto fish", "fish bot", "fishing"})
public final class AutoFishHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final SliderSetting validRange = new SliderSetting("Valid range",
		"Any bites that occur outside of this range will be ignored.\n\n"
			+ "Increase your range if bites are not being detected, decrease it if other people's bites are being detected as yours.",
		1.5, 0.25, 8, 0.25, ValueDisplay.DECIMAL);
	
	private int bestRodValue;
	private int bestRodSlot;
	
	private int castRodTimer;
	private int reelInTimer;
	private int scheduledWindowClick;
	
	private final AutoFishDebugDraw debugDraw = new AutoFishDebugDraw();
	
	private boolean wasOpenWater;
	
	public AutoFishHack()
	{
		super("AutoFish");
		
		setCategory(Category.OTHER);
		addSetting(validRange);
		debugDraw.getSettings().forEach(this::addSetting);
	}
	
	@Override
	public void onEnable()
	{
		bestRodValue = -1;
		bestRodSlot = -1;
		castRodTimer = 0;
		reelInTimer = -1;
		scheduledWindowClick = -1;
		debugDraw.reset();
		wasOpenWater = true;
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		debugDraw.updateValidRange(validRange.getValue());
		
		if(reelInTimer > 0)
			reelInTimer--;
		
		ClientPlayerEntity player = MC.player;
		PlayerInventory inventory = player.getInventory();
		
		if(scheduledWindowClick != -1)
		{
			IMC.getInteractionManager()
				.windowClick_PICKUP(scheduledWindowClick);
			castRodTimer = 15;
			return;
		}
		
		updateBestRod();
		
		if(bestRodSlot == -1)
		{
			ChatUtils.message("AutoFish has run out of fishing rods.");
			setEnabled(false);
			return;
		}
		
		if(bestRodSlot != inventory.selectedSlot)
		{
			selectBestRod();
			return;
		}
		
		// wait for timer
		if(castRodTimer > 0)
		{
			castRodTimer--;
			return;
		}
		
		// cast rod
		if(player.fishHook == null || player.fishHook.isRemoved())
		{
			rightClick();
			castRodTimer = 15;
			reelInTimer = 1200;
		}
		
		// reel in after 60s
		if(reelInTimer == 0)
		{
			reelInTimer--;
			rightClick();
			castRodTimer = 15;
		}
	}
	
	private void updateBestRod()
	{
		PlayerInventory inventory = MC.player.getInventory();
		int selectedSlot = inventory.selectedSlot;
		ItemStack selectedStack = inventory.getStack(selectedSlot);
		
		// start with selected rod
		bestRodValue = getRodValue(selectedStack);
		bestRodSlot = bestRodValue > -1 ? selectedSlot : -1;
		
		// search inventory for better rod
		for(int slot = 0; slot < 36; slot++)
		{
			ItemStack stack = inventory.getStack(slot);
			int rodValue = getRodValue(stack);
			
			if(rodValue > bestRodValue)
			{
				bestRodValue = rodValue;
				bestRodSlot = slot;
			}
		}
	}
	
	private int getRodValue(ItemStack stack)
	{
		if(stack.isEmpty() || !(stack.getItem() instanceof FishingRodItem))
			return -1;
		
		int luckOTSLvl =
			EnchantmentHelper.getLevel(Enchantments.LUCK_OF_THE_SEA, stack);
		int lureLvl = EnchantmentHelper.getLevel(Enchantments.LURE, stack);
		int unbreakingLvl =
			EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);
		int mendingBonus =
			EnchantmentHelper.getLevel(Enchantments.MENDING, stack);
		int noVanishBonus = EnchantmentHelper.hasVanishingCurse(stack) ? 0 : 1;
		
		return luckOTSLvl * 9 + lureLvl * 9 + unbreakingLvl * 2 + mendingBonus
			+ noVanishBonus;
	}
	
	private void selectBestRod()
	{
		PlayerInventory inventory = MC.player.getInventory();
		
		if(bestRodSlot < 9)
		{
			inventory.selectedSlot = bestRodSlot;
			return;
		}
		
		int firstEmptySlot = inventory.getEmptySlot();
		
		if(firstEmptySlot != -1)
		{
			if(firstEmptySlot >= 9)
				IMC.getInteractionManager()
					.windowClick_QUICK_MOVE(36 + inventory.selectedSlot);
			
			IMC.getInteractionManager().windowClick_QUICK_MOVE(bestRodSlot);
			
		}else
		{
			IMC.getInteractionManager().windowClick_PICKUP(bestRodSlot);
			IMC.getInteractionManager()
				.windowClick_PICKUP(36 + inventory.selectedSlot);
			
			scheduledWindowClick = -bestRodSlot;
		}
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		ClientPlayerEntity player = MC.player;
		if(player == null || player.fishHook == null)
			return;
		
		if(!(event.getPacket() instanceof PlaySoundS2CPacket))
			return;
		
		// check sound type
		PlaySoundS2CPacket sound = (PlaySoundS2CPacket)event.getPacket();
		if(!SoundEvents.ENTITY_FISHING_BOBBER_SPLASH.equals(sound.getSound()))
			return;
		
		debugDraw.updateSoundPos(sound);
		
		// check position
		FishingBobberEntity bobber = player.fishHook;
		if(Math.abs(sound.getX() - bobber.getX()) > validRange.getValue()
			|| Math.abs(sound.getZ() - bobber.getZ()) > validRange.getValue())
			return;
		
		// check open water
		boolean isOpenWater = isInOpenWater(bobber);
		if(!isOpenWater && wasOpenWater)
		{
			ChatUtils.warning("You are currently fishing in shallow water.");
			ChatUtils.message(
				"You can't get any treasure items while fishing like this.");
			
			if(!WURST.getHax().openWaterEspHack.isEnabled())
				ChatUtils.message("Use OpenWaterESP to find open water.");
		}
		
		// catch fish
		rightClick();
		castRodTimer = 15;
		wasOpenWater = isOpenWater;
	}
	
	private boolean isInOpenWater(FishingBobberEntity bobber)
	{
		return ((IFishingBobberEntity)bobber)
			.checkOpenWaterAround(bobber.getBlockPos());
	}
	
	private void rightClick()
	{
		// check held item
		ItemStack stack = MC.player.getInventory().getMainHandStack();
		if(stack.isEmpty() || !(stack.getItem() instanceof FishingRodItem))
			return;
		
		// right click
		IMC.rightClick();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		debugDraw.render(matrixStack, partialTicks);
	}
}
