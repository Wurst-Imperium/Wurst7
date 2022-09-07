/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IFishingBobberEntity;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;

@SearchTags({"FishBot", "auto fish", "fish bot", "fishing"})
public final class AutoFishHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final SliderSetting validRange = new SliderSetting("Valid range",
		"Any bites that occur outside of this range will be ignored.\n\n"
			+ "Increase your range if bites are not being detected, decrease it if other people's bites are being detected as yours.",
		1.5, 0.25, 8, 0.25, ValueDisplay.DECIMAL);
	
	private CheckboxSetting debugDraw = new CheckboxSetting("Debug draw",
		"Shows where bites are occurring and where they will be detected. Useful for optimizing your 'Valid range' setting.",
		false);
	
	private final ColorSetting ddColor = new ColorSetting("DD color",
		"Color of the debug draw, if enabled.", Color.RED);
	
	private int bestRodValue;
	private int bestRodSlot;
	
	private int castRodTimer;
	private int reelInTimer;
	private int scheduledWindowClick;
	private Vec3d lastSoundPos;
	
	private Box validRangeBox;
	
	private boolean wasOpenWater;
	
	public AutoFishHack()
	{
		super("AutoFish");
		
		setCategory(Category.OTHER);
		addSetting(validRange);
		addSetting(debugDraw);
		addSetting(ddColor);
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
		updateDebugDraw();
		
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
	
	private void updateDebugDraw()
	{
		if(debugDraw.isChecked())
			validRangeBox = new Box(-validRange.getValue(), -1 / 16.0,
				-validRange.getValue(), validRange.getValue(), 1 / 16.0,
				validRange.getValue());
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
		
		if(debugDraw.isChecked())
			lastSoundPos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
		
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
		if(!debugDraw.isChecked())
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		FishingBobberEntity bobber = MC.player.fishHook;
		if(bobber != null)
			drawValidRange(matrixStack, bobber, regionX, regionZ);
		
		drawLastBite(matrixStack, regionX, regionZ);
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private void drawValidRange(MatrixStack matrixStack,
		FishingBobberEntity bobber, int regionX, int regionZ)
	{
		matrixStack.push();
		matrixStack.translate(bobber.getX() - regionX, bobber.getY(),
			bobber.getZ() - regionZ);
		float[] colorF = ddColor.getColorF();
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
		RenderUtils.drawOutlinedBox(validRangeBox, matrixStack);
		matrixStack.pop();
	}
	
	private void drawLastBite(MatrixStack matrixStack, int regionX, int regionZ)
	{
		Matrix4f matrix = matrixStack.peek().getModel();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		if(lastSoundPos != null)
		{
			matrixStack.push();
			matrixStack.translate(lastSoundPos.x - regionX, lastSoundPos.y,
				lastSoundPos.z - regionZ);
			float[] colorF = ddColor.getColorF();
			RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
			bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
				VertexFormats.POSITION);
			bufferBuilder.vertex(matrix, (float)-0.125, 0, (float)-0.125)
				.next();
			bufferBuilder.vertex(matrix, (float)0.125, 0, (float)0.125).next();
			bufferBuilder.vertex(matrix, (float)0.125, 0, (float)-0.125).next();
			bufferBuilder.vertex(matrix, (float)-0.125, 0, (float)0.125).next();
			bufferBuilder.end();
			BufferRenderer.draw(bufferBuilder);
			matrixStack.pop();
		}
	}
	
	private boolean isInOpenWater(FishingBobberEntity bobber)
	{
		return ((IFishingBobberEntity)bobber)
			.checkOpenWaterAround(bobber.getBlockPos());
	}
}
