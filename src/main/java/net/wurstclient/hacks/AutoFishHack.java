/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;

@SearchTags({"FishBot", "auto fish", "fish bot", "fishing"})
public final class AutoFishHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final SliderSetting validRange = new SliderSetting("Valid range",
		"Any bites that occur outside of this range\n" + "will be ignored.\n\n"
			+ "Increase your range if bites are not being\n"
			+ "detected, decrease it if other people's\n"
			+ "bites are being detected as yours.",
		1.5, 0.25, 8, 0.25, ValueDisplay.DECIMAL);
	
	private CheckboxSetting debugDraw = new CheckboxSetting("Debug draw",
		"Shows where bites are occurring and where\n"
			+ "they will be detected. Useful for optimizing\n"
			+ "your 'Valid range' setting.",
		false);
	
	private int bestRodValue;
	private int bestRodSlot;
	
	private int castRodTimer;
	private int reelInTimer;
	
	private int box;
	private int cross;
	
	private int scheduledWindowClick;
	private Vec3d lastSoundPos;
	
	public AutoFishHack()
	{
		super("AutoFish", "Automatically catches fish using your\n"
			+ "best fishing rod. If it finds a better\n"
			+ "rod while fishing, it will automatically\n" + "switch to it.");
		
		setCategory(Category.OTHER);
		addSetting(validRange);
		addSetting(debugDraw);
	}
	
	@Override
	public void onEnable()
	{
		bestRodValue = -1;
		bestRodSlot = -1;
		castRodTimer = 0;
		reelInTimer = -1;
		scheduledWindowClick = -1;
		lastSoundPos = null;
		
		box = GL11.glGenLists(1);
		
		cross = GL11.glGenLists(1);
		GL11.glNewList(cross, GL11.GL_COMPILE);
		GL11.glColor4f(1, 0, 0, 0.5F);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(-0.125, 0, -0.125);
		GL11.glVertex3d(0.125, 0, 0.125);
		GL11.glVertex3d(0.125, 0, -0.125);
		GL11.glVertex3d(-0.125, 0, 0.125);
		GL11.glEnd();
		GL11.glEndList();
		
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
		
		GL11.glDeleteLists(box, 1);
		GL11.glDeleteLists(cross, 1);
	}
	
	@Override
	public void onUpdate()
	{
		updateDebugDraw();
		
		if(reelInTimer > 0)
			reelInTimer--;
		
		ClientPlayerEntity player = MC.player;
		PlayerInventory inventory = player.inventory;
		
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
			ChatUtils.message("Out of fishing rods.");
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
		if(player.fishHook == null)
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
	
	private void updateDebugDraw()
	{
		if(debugDraw.isChecked())
		{
			GL11.glNewList(box, GL11.GL_COMPILE);
			Box box = new Box(-validRange.getValue(), -1 / 16.0,
				-validRange.getValue(), validRange.getValue(), 1 / 16.0,
				validRange.getValue());
			GL11.glColor4f(1, 0, 0, 0.5F);
			RenderUtils.drawOutlinedBox(box);
			GL11.glEndList();
		}
	}
	
	private void updateBestRod()
	{
		PlayerInventory inventory = MC.player.inventory;
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
		PlayerInventory inventory = MC.player.inventory;
		
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
		
		if(debugDraw.isChecked())
			lastSoundPos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
		
		// check position
		FishingBobberEntity bobber = player.fishHook;
		if(Math.abs(sound.getX() - bobber.getX()) > validRange.getValue()
			|| Math.abs(sound.getZ() - bobber.getZ()) > validRange.getValue())
			return;
		
		// catch fish
		rightClick();
		castRodTimer = 15;
	}
	
	private void rightClick()
	{
		// check held item
		ItemStack stack = MC.player.inventory.getMainHandStack();
		if(stack.isEmpty() || !(stack.getItem() instanceof FishingRodItem))
			return;
		
		// right click
		IMC.rightClick();
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		if(!debugDraw.isChecked())
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		FishingBobberEntity bobber = MC.player.fishHook;
		if(bobber != null)
		{
			GL11.glPushMatrix();
			GL11.glTranslated(bobber.getX(), bobber.getY(), bobber.getZ());
			GL11.glCallList(box);
			GL11.glPopMatrix();
		}
		
		if(lastSoundPos != null)
		{
			GL11.glPushMatrix();
			GL11.glTranslated(lastSoundPos.x, lastSoundPos.y, lastSoundPos.z);
			GL11.glCallList(cross);
			GL11.glPopMatrix();
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
}
