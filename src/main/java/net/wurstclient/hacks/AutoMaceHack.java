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
import net.minecraft.util.hit.EntityHitResult;
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
	
	private long lastHitNs = 0L;
	
	/* Settings */
	private final TextFieldSetting windBurstName = new TextFieldSetting(
		"Wind burst item",
		"Substring to locate wind burst item (case-insensitive). Example: \"wind\"",
		"wind");
	
	private final CheckboxSetting preferChestAir = new CheckboxSetting(
		"Prefer Netherite chestplate in air",
		"When airborne, equip Netherite chestplate (and hold mace).", false);
	
	private final CheckboxSetting autoEquipEnabled =
		new CheckboxSetting("Auto-equip (enable/disable)",
			"Turn on/off automatic chest item switching.", true);
	
	// Replace the old TextFieldSetting airEquipMode with TWO checkboxes
	// (radio-like)
	private final CheckboxSetting preferElytraAir = new CheckboxSetting(
		"Prefer Elytra in air",
		"When airborne, equip Elytra (and hold fireworks/wind burst).", true);
	
	// Make hits slower / configurable
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
	
	// === Runtime state for transition-based equip ===
	private boolean didEquipForThisAir = false;
	private boolean didEquipForThisGround = false;
	private int lastHeldSlot = -1;
	private long lastActionNs = 0L;
	private boolean didBurstThisJump = false;
	private boolean wasOnGround = true;
	
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
		addSetting(preferChestAir);
		
		addSetting(hitPlayersOnly);
		addSetting(hitRange);
		addSetting(hitDelayMs);
	}
	
	@Override
	protected void onEnable()
	{
		lastActionNs = 0;
		didBurstThisJump = false;
		wasOnGround = true;
		
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
		
		// Reset “one burst per jump”
		if(onGround)
		{
			didBurstThisJump = false;
			didEquipForThisAir = false; // allow action next time we go air
		}else
		{
			didEquipForThisGround = false; // allow action next time we land
		}
		
		// === Transition-based auto-equip ===
		// === Enforce desired chest item every tick (no transition gating) ===
		if(autoEquipEnabled.isChecked())
		{
			if(!onGround)
			{
				// Airborne: Elytra if pref ON, otherwise armor
				if(preferElytraAir.isChecked())
				{
					// want Elytra in air
					if(MC.player.getEquippedStack(EquipmentSlot.CHEST)
						.getItem() != Items.ELYTRA)
					{
						equipElytraByUse();
					}
				}else
				{
					// want armor in air
					if(MC.player.getEquippedStack(EquipmentSlot.CHEST)
						.getItem() == Items.ELYTRA
						|| MC.player.getEquippedStack(EquipmentSlot.CHEST)
							.isEmpty())
					{
						ensureArmorEquippedByUse();
					}
				}
			}else
			{
				// Grounded: always armor (if Elytra on, swap to armor)
				if(MC.player.getEquippedStack(EquipmentSlot.CHEST)
					.getItem() == Items.ELYTRA
					|| MC.player.getEquippedStack(EquipmentSlot.CHEST)
						.isEmpty())
				{
					ensureArmorEquippedByUse();
				}
			}
		}
		
		// Choose what to HOLD in hand (fireworks/wind burst vs mace) based on
		// mode/equipment
		chooseHandItem(onGround);
		
		// Throttle general actions
		long now = System.nanoTime();
		if(now - lastActionNs < TimeUnit.MILLISECONDS
			.toNanos(actionDelayMs.getValueI()))
			return;
		
		// Build ignore set (used only for targeting – DOES NOT pause the hack)
		final Set<String> ignoreSet =
			Arrays.stream(ignorePlayers.getValue().split(",")).map(String::trim)
				.filter(s -> !s.isEmpty()).map(String::toLowerCase)
				.collect(Collectors.toSet());
		
		// Optional single tap while falling (KillAura-style targeting with
		// delay)
		if(!onGround && autoHitOnDrop.isChecked()
			&& MC.player.getVelocity().y < -0.15)
			tryAutoHitNearby(ignoreSet);
		
		// Fire wind burst ONLY when on ground, only once per jump
		if(onGround && !didBurstThisJump)
		{
			if(tryWindBurstAtFeet())
			{
				didBurstThisJump = true; // don’t fire again until we land
				lastActionNs = System.nanoTime();
				
				// After burst, try to switch back to mace (no camera change)
				Predicate<ItemStack> isMace = s -> s != null && !s.isEmpty()
					&& (s.getItem() instanceof MaceItem || s.getName()
						.getString().toLowerCase().contains("mace"));
				InventoryUtils.selectItem(isMace, 36, true);
			}
		}
		
		wasOnGround = onGround;
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
	{ /* use commands to toggle/dequip */ }
	
	@Override
	public void onRender(MatrixStack matrices, float partialTicks)
	{ /* no HUD */ }
	
	/* Public API for keybinds/commands */
	public void toggleAutoMace()
	{
		setEnabled(!isEnabled());
	}
	
	/** Called once when we become airborne (if auto-equip enabled). */
	private void equipForAir()
	{
		if(!preferElytraAir.isChecked())
		{
			// user prefers ARMOR in air (implicit)
			ensureArmorEquippedByUse(); // right-click to equip
			return;
		}
		// Elytra in air
		if(MC.player.getEquippedStack(EquipmentSlot.CHEST)
			.getItem() == Items.ELYTRA)
			return;
			
		// Select Elytra into hand and right-click to wear (always replaces
		// chest item server-side)
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
	
	/**
	 * Decide what to hold: fireworks (or wind burst) for Elytra mode; mace for
	 * chestplate mode/ground.
	 */
	/**
	 * Decide what to hold: fireworks (or wind burst) for Elytra pref; mace
	 * otherwise.
	 */
	private void chooseHandItem(boolean onGround)
	{
		if(!onGround && preferElytraAir.isChecked())
		{
			// Elytra mode in air => Fireworks first, else Wind Burst
			boolean haveFireworks = InventoryUtils.selectItem(
				s -> s != null && !s.isEmpty() && s.isOf(Items.FIREWORK_ROCKET),
				36, false);
			if(haveFireworks)
				return;
			
			InventoryUtils.selectItem(
				s -> s != null && !s.isEmpty() && (s.isOf(Items.WIND_CHARGE)
					|| s.getName().getString().toLowerCase().contains("wind")),
				36, false);
			return;
		}
		
		// Otherwise (ground or chestplate-in-air) => hold Mace
		InventoryUtils
			.selectItem(
				s -> s != null && !s.isEmpty()
					&& ((s.getItem() instanceof MaceItem) || s.getName()
						.getString().toLowerCase().contains("mace")),
				36, false);
	}
	
	/** Called once when we land (if auto-equip enabled). */
	private void equipForGround()
	{
		// Always armor on ground
		if(!MC.player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
			&& MC.player.getEquippedStack(EquipmentSlot.CHEST)
				.getItem() != Items.ELYTRA)
			return; // already armor
		ensureArmorEquippedByUse(); // right-click to wear
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
		
		// Select into hand
		boolean ok = InventoryUtils.selectItem(s -> s != null && !s.isEmpty()
			&& (s.isOf(Items.NETHERITE_CHESTPLATE)
				|| s.isOf(Items.DIAMOND_CHESTPLATE)
				|| s.isOf(Items.IRON_CHESTPLATE)),
			36, true);
		if(!ok)
			return;
		
		// Right-click to equip (swaps whatever is on chest back to inventory)
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
	
	/**
	 * Prefer best available: Netherite -> Diamond -> Iron (simple + robust).
	 */
	private void ensureArmorEquipped()
	{
		int slot = InventoryUtils.indexOf(Items.NETHERITE_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.DIAMOND_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.IRON_CHESTPLATE);
		
		if(slot != -1)
		{
			try
			{
				int net = InventoryUtils.toNetworkSlot(slot);
				IMC.getInteractionManager().windowClick_QUICK_MOVE(net);
			}catch(Throwable ignored)
			{}
		}
	}
	
	public void dequipAndRestore()
	{
		if(MC.player == null || MC.world == null)
			return;
		// Force armor (user asked: de-equip should bring armor back & mace)
		ensureArmorEquipped();
		if(lastHeldSlot >= 0)
			MC.player.getInventory().setSelectedSlot(lastHeldSlot);
	}
	
	private void tryAutoHitNearby(Set<String> ignoreSet)
	{
		if(MC.interactionManager == null || MC.player == null
			|| MC.world == null)
			return;
		
		long now = System.nanoTime();
		if(now - lastHitNs < TimeUnit.MILLISECONDS
			.toNanos(hitDelayMs.getValueI()))
			return; // respect user-set hit delay
			
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
	
	/* ===== Core behavior ===== */
	
	/** Use wind burst by clicking the block directly below the player. */
	private boolean tryWindBurstAtFeet()
	{
		if(MC.player == null)
			return false;
		
		// Select wind charge into hand (move/select)
		final String search = windBurstName.getValue().toLowerCase();
		Predicate<ItemStack> isWind =
			s -> s != null && !s.isEmpty() && (s.isOf(Items.WIND_CHARGE)
				|| s.getName().getString().toLowerCase().contains(search));
		
		lastHeldSlot = MC.player.getInventory().getSelectedSlot();
		boolean selected = InventoryUtils.selectItem(isWind, 36, true);
		if(!selected)
			return false;
			
		// Optionally suppress movement keys during the click to avoid input
		// fights
		if(suppressMovementDuringBurst.isChecked())
		{
			GameOptions gs = MC.options;
			KeyBinding[] bindings = {gs.forwardKey, gs.backKey, gs.leftKey,
				gs.rightKey, gs.jumpKey, gs.sneakKey};
			for(KeyBinding b : bindings)
				b.setPressed(false);
		}
		
		// Click the block BELOW feet — no camera rotation, no generic
		// right-click fallback
		BlockPos below = MC.player.getBlockPos().down();
		Vec3d hitVec = Vec3d.ofCenter(below);
		try
		{
			IMC.getInteractionManager().rightClickBlock(below, Direction.UP,
				hitVec);
		}catch(Throwable t)
		{
			return false; // if precise block click fails, we DO NOT fall back
							// to rightClickItem()
		}
		
		// successful burst
		return true;
	}
	
	/* ===== Helpers ===== */
	
	private boolean isIgnoredPlayerNearby()
	{
		Set<String> ignoreSet =
			Arrays.stream(ignorePlayers.getValue().split(",")).map(String::trim)
				.filter(s -> !s.isEmpty()).map(String::toLowerCase)
				.collect(Collectors.toSet());
		if(ignoreSet.isEmpty())
			return false;
		
		return MC.world.getPlayers().stream().filter(p -> p != MC.player)
			.anyMatch(p -> {
				String name = getPlayerNameLower(p);
				return name != null && ignoreSet.contains(name);
			});
	}
	
	private void tryAutoHitCrosshair()
	{
		if(MC.currentScreen != null || MC.interactionManager == null
			|| MC.crosshairTarget == null)
			return;
		if(!(MC.crosshairTarget instanceof EntityHitResult ehr))
			return;
		
		try
		{
			MC.interactionManager.attackEntity(MC.player, ehr.getEntity());
			MC.player.swingHand(Hand.MAIN_HAND);
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
}
