/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.TextFieldSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMinecraftClient;

@SearchTags({"auto mace", "wind burst", "auto-elytra", "auto-use"})
public final class AutoMaceHack extends Hack
	implements UpdateListener, HandleInputListener, RenderListener
{
	private static final WurstClient WURST = WurstClient.INSTANCE;
	private static final MinecraftClient MC = WurstClient.MC;
	private static final IMinecraftClient IMC = WurstClient.IMC;
	
	/* Settings */
	private final TextFieldSetting windBurstName = new TextFieldSetting(
		"Wind burst item",
		"Substring to locate wind burst item (case-insensitive). Example: \"wind\"",
		"wind");
	
	private final CheckboxSetting autoEquipEnabled =
		new CheckboxSetting("Auto-equip (enable/disable)",
			"Turn on/off automatic chest item switching.", true);
	
	private final CheckboxSetting preferElytraAir = new CheckboxSetting(
		"Prefer Elytra in air", "When airborne, equip Elytra.", true);
	
	// NEW: global preference to hold fireworks in hand
	private final CheckboxSetting preferFireworks = new CheckboxSetting(
		"Prefer Fireworks in hand",
		"If ON, always try to hold Firework Rockets in hand in all scenarios.",
		false);
	
	private final SliderSetting hitDelayMs = new SliderSetting("Hit delay (ms)",
		"Minimum delay between auto-hits while falling.", 220, 0, 1000, 10,
		ValueDisplay.INTEGER);
	
	private final CheckboxSetting hitPlayersOnly = new CheckboxSetting(
		"Hit players only",
		"If ON, only attacks players while dropping. If OFF, attacks any living entity.",
		false);
	
	private final SliderSetting hitRange =
		new SliderSetting("Hit range", "Range for auto-hit while dropping.",
			4.5, 1.0, 6.0, 0.1, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting autoHitOnDrop = new CheckboxSetting(
		"Auto-hit while falling",
		"Tap attack once on the entity under crosshair while falling.", true);
	
	private final CheckboxSetting suppressMovementDuringBurst =
		new CheckboxSetting("Avoid player input during burst",
			"Temporarily releases movement keys during the burst.", true);
	
	private final TextFieldSetting ignorePlayers = new TextFieldSetting(
		"Ignore players",
		"Comma-separated usernames. If any are nearby, AutoMace pauses.", "");
	
	private final SliderSetting actionDelayMs =
		new SliderSetting("Action delay (ms)", "Delay between internal steps.",
			120, 0, 1000, 10, ValueDisplay.INTEGER);
	
	// Runtime
	private int lastHeldSlot = -1;
	private long lastBurstNs = 0L; // throttle only the wind-burst
	private long lastHitNs = 0L;
	
	public AutoMaceHack()
	{
		super("AutoMace");
		setCategory(Category.COMBAT);
		addSetting(windBurstName);
		addSetting(autoHitOnDrop);
		addSetting(suppressMovementDuringBurst);
		addSetting(ignorePlayers);
		addSetting(actionDelayMs);
		
		addSetting(autoEquipEnabled);
		addSetting(preferElytraAir);
		addSetting(preferFireworks); // NEW
		
		addSetting(hitPlayersOnly);
		addSetting(hitRange);
		addSetting(hitDelayMs);
	}
	
	@Override
	protected void onEnable()
	{
		lastBurstNs = 0L;
		lastHitNs = 0L; // keep if you want a clean start for hits too
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(HandleInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
		ChatUtils.message("AutoMace enabled");
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(HandleInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		if(MC.player != null && lastHeldSlot >= 0)
			MC.player.getInventory().setSelectedSlot(lastHeldSlot);
		lastHeldSlot = -1;
		ChatUtils.message("AutoMace disabled");
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.player == null || MC.world == null || MC.isPaused())
			return;
		
		final boolean onGround = MC.player.isOnGround();
		final long now = System.nanoTime();
		
		// === Settings coupling (Elytra has priority over Fireworks) ===
		// If Elytra is OFF while Fireworks is ON, turn Fireworks OFF (allow
		// user to disable Elytra).
		if(!preferElytraAir.isChecked() && preferFireworks.isChecked())
			preferFireworks.setChecked(false);
		// NOTE: We no longer auto-enable Elytra when Fireworks is enabled.
		// Users can enable Fireworks first, but it will be auto-turned off
		// unless Elytra is also turned on.
		
		// === Auto-equip chest item ===
		if(autoEquipEnabled.isChecked())
		{
			if(!onGround)
			{
				if(preferElytraAir.isChecked())
				{
					if(MC.player.getEquippedStack(EquipmentSlot.CHEST)
						.getItem() != Items.ELYTRA)
						equipElytraByUse();
				}else
				{
					if(MC.player.getEquippedStack(EquipmentSlot.CHEST)
						.getItem() == Items.ELYTRA
						|| MC.player.getEquippedStack(EquipmentSlot.CHEST)
							.isEmpty())
						ensureArmorEquippedByUse();
				}
			}else
			{
				if(preferElytraAir.isChecked())
				{
					// Keep whatever the player chose on ground (including
					// Elytra)
				}else
				{
					if(MC.player.getEquippedStack(EquipmentSlot.CHEST)
						.getItem() == Items.ELYTRA
						|| MC.player.getEquippedStack(EquipmentSlot.CHEST)
							.isEmpty())
						ensureArmorEquippedByUse();
				}
			}
		}
		
		// Decide what to HOLD in hand
		chooseHandItem(onGround);
		
		// Build ignore set (for targeting only)
		final Set<String> ignoreSet =
			Arrays.stream(ignorePlayers.getValue().split(",")).map(String::trim)
				.filter(s -> !s.isEmpty()).map(String::toLowerCase)
				.collect(Collectors.toSet());
		
		// Optional single tap while falling (KillAura-style targeting with
		// delay)
		if(!onGround && autoHitOnDrop.isChecked()
			&& MC.player.getVelocity().y < -0.15)
			tryAutoHitNearby(ignoreSet);
			
		// === Repeating wind burst while standing on ground (cooldown =
		// actionDelayMs) ===
		if(onGround)
		{
			long burstCooldownNs =
				TimeUnit.MILLISECONDS.toNanos(actionDelayMs.getValueI());
			if(now - lastBurstNs >= burstCooldownNs)
			{
				if(tryWindBurstAtFeet())
				{
					lastBurstNs = now;
					
					// After burst: only switch back to Mace if fireworks are
					// NOT preferred
					if(!preferFireworks.isChecked())
					{
						Predicate<ItemStack> isMace = s -> s != null
							&& !s.isEmpty()
							&& (s.getItem() instanceof MaceItem || s.getName()
								.getString().toLowerCase().contains("mace"));
						InventoryUtils.selectItem(isMace, 36, true);
					}
				}
			}
		}
	}
	
	/**
	 * Equip Netherite->Diamond->Iron by selecting it into hand and
	 * right-clicking (server swaps chest).
	 */
	private void ensureArmorEquippedByUse()
	{
		int slot = InventoryUtils.indexOf(Items.NETHERITE_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.DIAMOND_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.IRON_CHESTPLATE);
		if(slot == -1)
			return;
		
		// Select into hand (so right-click will equip it into the chest slot)
		boolean ok = InventoryUtils.selectItem(s -> s != null && !s.isEmpty()
			&& (s.isOf(Items.NETHERITE_CHESTPLATE)
				|| s.isOf(Items.DIAMOND_CHESTPLATE)
				|| s.isOf(Items.IRON_CHESTPLATE)),
			36, true);
		if(!ok)
			return;
		
		// Right-click to equip (whatever was worn goes back to inventory)
		try
		{
			IMC.getInteractionManager().rightClickItem();
		}catch(Throwable t)
		{
			try
			{
				MC.interactionManager.interactItem(MC.player, Hand.MAIN_HAND);
			}catch(Throwable ignored)
			{}
		}
	}
	
	private void equipElytraByUse()
	{
		if(InventoryUtils.selectItem(
			s -> s != null && !s.isEmpty() && s.isOf(Items.ELYTRA), 36, true))
		{
			try
			{
				IMC.getInteractionManager().rightClickItem();
			}catch(Throwable t)
			{
				try
				{
					MC.interactionManager.interactItem(MC.player,
						Hand.MAIN_HAND);
				}catch(Throwable ignored)
				{}
			}
		}
	}
	
	@Override
	public void onHandleInput()
	{}
	
	@Override
	public void onRender(MatrixStack matrices, float partialTicks)
	{}
	
	public void toggleAutoMace()
	{
		setEnabled(!isEnabled());
	}
	
	private void chooseHandItem(boolean onGround)
	{
		// NEW highest priority: Prefer Fireworks in hand (all scenarios)
		if(preferFireworks.isChecked())
		{
			InventoryUtils.selectItem(
				s -> s != null && !s.isEmpty() && s.isOf(Items.FIREWORK_ROCKET),
				36, false);
			return;
		}
		
		// If user prefers Elytra in air, we STILL HOLD MACE (as requested)
		if(preferElytraAir.isChecked())
		{
			InventoryUtils.selectItem(
				s -> s != null && !s.isEmpty()
					&& ((s.getItem() instanceof MaceItem) || s.getName()
						.getString().toLowerCase().contains("mace")),
				36, false);
			return;
		}
		
		// Otherwise (default): hold Mace
		InventoryUtils
			.selectItem(
				s -> s != null && !s.isEmpty()
					&& ((s.getItem() instanceof MaceItem) || s.getName()
						.getString().toLowerCase().contains("mace")),
				36, false);
	}
	
	/** Use wind burst by clicking the block directly below the player. */
	private boolean tryWindBurstAtFeet()
	{
		if(MC.player == null)
			return false;
		
		final String search = windBurstName.getValue().toLowerCase();
		Predicate<ItemStack> isWind =
			s -> s != null && !s.isEmpty() && (s.isOf(Items.WIND_CHARGE)
				|| s.getName().getString().toLowerCase().contains(search));
		
		lastHeldSlot = MC.player.getInventory().getSelectedSlot();
		boolean selected = InventoryUtils.selectItem(isWind, 36, true);
		if(!selected)
			return false;
		
		if(suppressMovementDuringBurst.isChecked())
		{
			GameOptions gs = MC.options;
			KeyBinding[] bindings = {gs.forwardKey, gs.backKey, gs.leftKey,
				gs.rightKey, gs.jumpKey, gs.sneakKey};
			for(KeyBinding b : bindings)
				b.setPressed(false);
		}
		
		BlockPos below = MC.player.getBlockPos().down();
		Vec3d hitVec = Vec3d.ofCenter(below);
		try
		{
			IMC.getInteractionManager().rightClickBlock(below, Direction.UP,
				hitVec);
		}catch(Throwable t)
		{
			return false;
		}
		return true;
	}
	
	private void tryAutoHitNearby(Set<String> ignoreSet)
	{
		if(MC.interactionManager == null || MC.player == null
			|| MC.world == null)
			return;
		
		long now = System.nanoTime();
		if(now - lastHitNs < TimeUnit.MILLISECONDS
			.toNanos(hitDelayMs.getValueI()))
			return;
		
		final double range = hitRange.getValue();
		var box = MC.player.getBoundingBox().expand(range, range, range);
		var candidates = MC.world.getOtherEntities(MC.player, box, e -> {
			if(!e.isAlive())
				return false;
			if(e.squaredDistanceTo(MC.player) > range * range)
				return false;
			if(e instanceof PlayerEntity p)
			{
				String name = getPlayerNameLower(p);
				if(name != null && ignoreSet.contains(name))
					return false;
				if(hitPlayersOnly.isChecked())
					return true;
			}else if(hitPlayersOnly.isChecked())
			{
				return false;
			}
			return e.isAttackable();
		});
		
		var target = candidates.stream()
			.min((a, b) -> Double.compare(a.squaredDistanceTo(MC.player),
				b.squaredDistanceTo(MC.player)))
			.orElse(null);
		
		if(target == null)
			return;
		
		try
		{
			MC.interactionManager.attackEntity(MC.player, target);
			MC.player.swingHand(Hand.MAIN_HAND);
			lastHitNs = System.nanoTime();
		}catch(Throwable ignored)
		{}
	}
	
	private static String getPlayerNameLower(PlayerEntity p)
	{
		try
		{
			String n = p.getName().getString();
			return n != null ? n.toLowerCase() : null;
		}catch(Throwable ignored)
		{
			return null;
		}
	}
	
	// === getters for HUD ===
	public boolean isPreferElytraAir()
	{
		return preferElytraAir.isChecked();
	}
	
	public boolean isPreferFireworks()
	{
		return preferFireworks.isChecked();
	}
}
