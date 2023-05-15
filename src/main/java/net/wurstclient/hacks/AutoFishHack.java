/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.autofish.AutoFishDebugDraw;
import net.wurstclient.hacks.autofish.AutoFishRodSelector;
import net.wurstclient.mixinterface.IFishingBobberEntity;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;

@SearchTags({"AutoFishing", "auto fishing", "AutoFisher", "auto fisher",
	"AFKFishBot", "afk fish bot", "AFKFishingBot", "afk fishing bot",
	"AFKFisherBot", "afk fisher bot"})
public final class AutoFishHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final SliderSetting validRange = new SliderSetting("Valid range",
		"Any bites that occur outside of this range will be ignored.\n\n"
			+ "Increase your range if bites are not being detected, decrease it if other people's bites are being detected as yours.",
		1.5, 0.25, 8, 0.25, ValueDisplay.DECIMAL);
	
	private final AutoFishDebugDraw debugDraw =
		new AutoFishDebugDraw(validRange);
	private final AutoFishRodSelector rodSelector =
		new AutoFishRodSelector(this);
	
	private int castRodTimer;
	private int reelInTimer;
	
	private boolean wasOpenWater;
	
	public AutoFishHack()
	{
		super("AutoFish");
		setCategory(Category.OTHER);
		
		addSetting(validRange);
		debugDraw.getSettings().forEach(this::addSetting);
		rodSelector.getSettings().forEach(this::addSetting);
	}
	
	@Override
	public String getRenderName()
	{
		if(!rodSelector.hasARod())
			return getName() + " [out of rods]";
		
		return getName();
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().airPlaceHack.setEnabled(false);
		
		castRodTimer = 0;
		reelInTimer = -1;
		rodSelector.reset();
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
		// update timers
		if(castRodTimer > 0)
			castRodTimer--;
		if(reelInTimer > 0)
			reelInTimer--;
		
		// select fishing rod
		if(!rodSelector.isBestRodAlreadySelected())
		{
			rodSelector.selectBestRod();
			return;
		}
		
		// if not fishing, cast rod
		if(!isFishing())
		{
			if(castRodTimer > 0)
				return;
			
			IMC.rightClick();
			castRodTimer = 15;
			reelInTimer = 1200;
			return;
		}
		
		// otherwise, reel in when it's time
		if(reelInTimer == 0)
		{
			reelInTimer--;
			IMC.rightClick();
			castRodTimer = 15;
		}
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		// check packet type
		if(!(event.getPacket() instanceof PlaySoundS2CPacket))
			return;
		
		// check sound type
		PlaySoundS2CPacket sound = (PlaySoundS2CPacket)event.getPacket();
		if(!SoundEvents.ENTITY_FISHING_BOBBER_SPLASH
			.equals(sound.getSound().value()))
			return;
		
		// check if player is fishing
		if(!isFishing())
			return;
		
		// check if player is holding a fishing rod
		ClientPlayerEntity player = MC.player;
		if(!player.getMainHandStack().isOf(Items.FISHING_ROD))
			return;
		
		debugDraw.updateSoundPos(sound);
		
		// check sound position
		FishingBobberEntity bobber = player.fishHook;
		if(Math.abs(sound.getX() - bobber.getX()) > validRange.getValue()
			|| Math.abs(sound.getZ() - bobber.getZ()) > validRange.getValue())
			return;
		
		// check open water
		boolean isOpenWater = ((IFishingBobberEntity)bobber)
			.checkOpenWaterAround(bobber.getBlockPos());
		if(!isOpenWater && wasOpenWater)
		{
			ChatUtils.warning("You are currently fishing in shallow water.");
			ChatUtils.message(
				"You can't get any treasure items while fishing like this.");
			
			if(!WURST.getHax().openWaterEspHack.isEnabled())
				ChatUtils.message("Use OpenWaterESP to find open water.");
		}
		wasOpenWater = isOpenWater;
		
		// catch fish
		reelInTimer = 0;
		castRodTimer = 15;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		debugDraw.render(matrixStack, partialTicks);
	}
	
	private boolean isFishing()
	{
		ClientPlayerEntity player = MC.player;
		return player != null && player.fishHook != null
			&& !player.fishHook.isRemoved();
	}
}
