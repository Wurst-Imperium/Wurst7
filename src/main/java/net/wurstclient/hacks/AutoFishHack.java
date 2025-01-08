/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.autofish.AutoFishDebugDraw;
import net.wurstclient.hacks.autofish.AutoFishRodSelector;
import net.wurstclient.hacks.autofish.FishingSpotManager;
import net.wurstclient.hacks.autofish.ShallowWaterWarningCheckbox;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"AutoFishing", "auto fishing", "AutoFisher", "auto fisher",
	"AFKFishBot", "afk fish bot", "AFKFishingBot", "afk fishing bot",
	"AFKFisherBot", "afk fisher bot"})
public final class AutoFishHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final EnumSetting<AutoFishHack.BiteMode> biteMode =
		new EnumSetting<>("Bite mode",
			"\u00a7lSound\u00a7r mode detects bites by listening for the bite sound."
				+ " This method is less accurate, but is more resilient against"
				+ " anti-cheats. See the \"Valid range\" setting.\n\n"
				+ "\u00a7lEntity\u00a7r mode detects bites by checking for the"
				+ " fishing hook's entity update packet. It's more accurate than"
				+ " the sound method, but is less resilient against anti-cheats.",
			AutoFishHack.BiteMode.values(), AutoFishHack.BiteMode.SOUND);
	
	private final SliderSetting validRange = new SliderSetting("Valid range",
		"Any bites that occur outside of this range will be ignored.\n\n"
			+ "Increase your range if bites are not being detected, decrease it"
			+ " if other people's bites are being detected as yours.\n\n"
			+ "This setting has no effect when \"Bite mode\" is set to \"Entity\".",
		1.5, 0.25, 8, 0.25, ValueDisplay.DECIMAL);
	
	private final SliderSetting catchDelay = new SliderSetting("Catch delay",
		"How long AutoFish will wait after a bite before reeling in.", 0, 0, 60,
		1, ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(1, "1 tick"));
	
	private final SliderSetting retryDelay = new SliderSetting("Retry delay",
		"If casting or reeling in the fishing rod fails, this is how long"
			+ " AutoFish will wait before trying again.",
		15, 0, 100, 1,
		ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(1, "1 tick"));
	
	private final SliderSetting patience = new SliderSetting("Patience",
		"How long AutoFish will wait if it doesn't get a bite before reeling in.",
		60, 10, 120, 1, ValueDisplay.INTEGER.withSuffix("s"));
	
	private final ShallowWaterWarningCheckbox shallowWaterWarning =
		new ShallowWaterWarningCheckbox();
	
	private final FishingSpotManager fishingSpots = new FishingSpotManager();
	private final AutoFishDebugDraw debugDraw =
		new AutoFishDebugDraw(validRange, fishingSpots);
	private final AutoFishRodSelector rodSelector =
		new AutoFishRodSelector(this);
	
	private int castRodTimer;
	private int reelInTimer;
	private boolean biteDetected;
	
	public AutoFishHack()
	{
		super("AutoFish");
		setCategory(Category.OTHER);
		addSetting(biteMode);
		addSetting(validRange);
		addSetting(catchDelay);
		addSetting(retryDelay);
		addSetting(patience);
		debugDraw.getSettings().forEach(this::addSetting);
		rodSelector.getSettings().forEach(this::addSetting);
		addSetting(shallowWaterWarning);
		fishingSpots.getSettings().forEach(this::addSetting);
	}
	
	@Override
	public String getRenderName()
	{
		if(rodSelector.isOutOfRods())
			return getName() + " [out of rods]";
		
		return getName();
	}
	
	@Override
	protected void onEnable()
	{
		castRodTimer = 0;
		reelInTimer = 0;
		biteDetected = false;
		rodSelector.reset();
		debugDraw.reset();
		fishingSpots.reset();
		shallowWaterWarning.reset();
		
		WURST.getHax().antiAfkHack.setEnabled(false);
		WURST.getHax().aimAssistHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
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
		
		// update inventory
		if(!rodSelector.update())
			return;
		
		// if not fishing, cast rod
		if(!isFishing())
		{
			if(castRodTimer > 0)
				return;
			
			reelInTimer = 20 * patience.getValueI();
			if(!fishingSpots.onCast())
				return;
			
			MC.doItemUse();
			castRodTimer = retryDelay.getValueI();
			return;
		}
		
		// if a bite was detected, check water type and reel in
		if(biteDetected)
		{
			shallowWaterWarning.checkWaterType();
			reelInTimer = catchDelay.getValueI();
			fishingSpots.onBite(MC.player.fishHook);
			biteDetected = false;
			
			// also reel in if an entity was hooked
		}else if(MC.player.fishHook.getHookedEntity() != null)
			reelInTimer = catchDelay.getValueI();
		
		// otherwise, reel in when the timer runs out
		if(reelInTimer == 0)
		{
			MC.doItemUse();
			reelInTimer = retryDelay.getValueI();
			castRodTimer = retryDelay.getValueI();
		}
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		switch(biteMode.getSelected())
		{
			case SOUND -> processSoundUpdate(event);
			case ENTITY -> processEntityUpdate(event);
		}
	}
	
	private void processSoundUpdate(PacketInputEvent event)
	{
		// check packet type
		if(!(event.getPacket() instanceof PlaySoundS2CPacket sound))
			return;
		
		// check sound type
		if(!SoundEvents.ENTITY_FISHING_BOBBER_SPLASH
			.equals(sound.getSound().value()))
			return;
		
		// check if player is fishing
		if(!isFishing())
			return;
		
		// register sound position
		debugDraw.updateSoundPos(sound);
		
		// check sound position (Chebyshev distance)
		Vec3d bobber = MC.player.fishHook.getPos();
		double dx = Math.abs(sound.getX() - bobber.getX());
		double dz = Math.abs(sound.getZ() - bobber.getZ());
		if(Math.max(dx, dz) > validRange.getValue())
			return;
		
		biteDetected = true;
	}
	
	private void processEntityUpdate(PacketInputEvent event)
	{
		// check packet type
		if(!(event.getPacket() instanceof EntityTrackerUpdateS2CPacket update))
			return;
		
		// check if the entity is a bobber
		if(!(MC.world
			.getEntityById(update.id()) instanceof FishingBobberEntity bobber))
			return;
		
		// check if it's our bobber
		if(bobber != MC.player.fishHook)
			return;
		
		// check if player is fishing
		if(!isFishing())
			return;
		
		biteDetected = true;
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
			&& !player.fishHook.isRemoved()
			&& player.getMainHandStack().isOf(Items.FISHING_ROD);
	}
	
	private enum BiteMode
	{
		SOUND("Sound"),
		ENTITY("Entity");
		
		private final String name;
		
		private BiteMode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
