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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;

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
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMinecraftClient;

@SearchTags({"auto mace", "wind burst", "auto-elytra", "auto-use"})
public final class AutoMaceHack extends Hack
	implements UpdateListener, HandleInputListener, RenderListener
{
	private static final WurstClient WURST = WurstClient.INSTANCE;
	private static final Minecraft MC = WurstClient.MC;
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
	
	private final SliderSetting legitPreDelayMs = new SliderSetting(
		"Legit pre-switch delay (ms)",
		"Wait this long after selecting a hotbar slot before using the item.",
		120, 0, 600, 10, ValueDisplay.INTEGER);
	
	private final SliderSetting legitPostDwellMs =
		new SliderSetting("Legit post-use dwell (ms)",
			"Keep the switched slot selected this long after using the item.",
			150, 0, 1000, 10, ValueDisplay.INTEGER);
	
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
	
	// If ON, only use items from hotbar (1–9), visibly switch, right-click,
	// etc.
	private final CheckboxSetting legitMode = new CheckboxSetting("Legit mode",
		"Uses only items that are in the hotbar and visibly switches hands.\n"
			+ "Also restricts auto-hits to a 45° cone in front of you.",
		false);
	
	// Runtime
	private int lastHeldSlot = -1;
	private long lastBurstNs = 0L; // throttle only the wind-burst
	private long lastHitNs = 0L;
	
	private enum LegitAction
	{
		NONE,
		WIND,
		EQUIP_ELYTRA,
		EQUIP_ARMOR
	}
	
	private LegitAction pendingLegitAction = LegitAction.NONE;
	private int pendingRestoreSlot = -1;
	private long legitUseAtNs = 0L;
	private long legitRestoreAtNs = 0L;
	private int pendingSelectedSlot = -1; // the slot we switched to for the
											// action
	
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
		addSetting(preferFireworks);
		addSetting(legitMode);
		addSetting(legitPreDelayMs);
		addSetting(legitPostDwellMs);
		
		addSetting(hitPlayersOnly);
		addSetting(hitRange);
		addSetting(hitDelayMs);
	}
	
	@Override
	protected void onEnable()
	{
		lastBurstNs = 0L;
		lastHitNs = 0L;
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(HandleInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
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
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.player == null || MC.level == null || MC.isPaused())
			return;
		
		final boolean onGround = MC.player.onGround();
		final long now = System.nanoTime();
		
		if(legitMode.isChecked())
			driveLegitSequence(now);
		
		// Elytra has priority over Fireworks
		if(!preferElytraAir.isChecked() && preferFireworks.isChecked())
			preferFireworks.setChecked(false);
		
		// === Auto-equip chest item ===
		if(autoEquipEnabled.isChecked()
			&& (!legitMode.isChecked() || !legitSequenceActive()))
		{
			if(!onGround)
			{
				if(preferElytraAir.isChecked())
				{
					if(MC.player.getItemBySlot(EquipmentSlot.CHEST)
						.getItem() != Items.ELYTRA)
						equipElytraByUse();
				}else
				{
					if(MC.player.getItemBySlot(EquipmentSlot.CHEST)
						.getItem() == Items.ELYTRA
						|| MC.player.getItemBySlot(EquipmentSlot.CHEST)
							.isEmpty())
						ensureArmorEquippedByUse();
				}
			}else
			{
				if(!preferElytraAir.isChecked())
				{
					if(MC.player.getItemBySlot(EquipmentSlot.CHEST)
						.getItem() == Items.ELYTRA
						|| MC.player.getItemBySlot(EquipmentSlot.CHEST)
							.isEmpty())
						ensureArmorEquippedByUse();
				}
			}
		}
		
		// 2) Hand choice: don't fight the sequencer while it's switching
		if(!legitMode.isChecked() || !legitSequenceActive())
			chooseHandItem(onGround);
		
		// Build ignore set
		final Set<String> ignoreSet =
			Arrays.stream(ignorePlayers.getValue().split(",")).map(String::trim)
				.filter(s -> !s.isEmpty()).map(String::toLowerCase)
				.collect(Collectors.toSet());
		
		// Optional single tap while falling
		if(!onGround && autoHitOnDrop.isChecked()
			&& MC.player.getDeltaMovement().y < -0.15)
			tryAutoHitNearby(ignoreSet);
		
		// Repeating wind burst while standing on ground
		if(onGround && (!legitMode.isChecked() || !legitSequenceActive()))
		{
			long burstCooldownNs =
				TimeUnit.MILLISECONDS.toNanos(actionDelayMs.getValueI());
			if(now - lastBurstNs >= burstCooldownNs)
			{
				if(tryWindBurstAtFeet())
				{
					if(!legitMode.isChecked())
					{
						// Non-legit: used immediately
						lastBurstNs = now;
						
						if(!preferFireworks.isChecked())
						{
							Predicate<ItemStack> isMace =
								s -> s != null && !s.isEmpty()
									&& (s.getItem() instanceof MaceItem
										|| s.getHoverName().getString()
											.toLowerCase().contains("mace"));
							InventoryUtils.selectItem(isMace, 36, true);
						}
					}
					// Legit mode: cooldown is set after actual use
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
		Predicate<ItemStack> isChest = s -> s != null && !s.isEmpty()
			&& (s.is(Items.NETHERITE_CHESTPLATE)
				|| s.is(Items.DIAMOND_CHESTPLATE)
				|| s.is(Items.IRON_CHESTPLATE));
		
		if(legitMode.isChecked())
		{
			if(legitSequenceActive())
				return;
			int slot = findHotbarSlot(isChest);
			if(slot == -1)
				return;
			startLegitSequence(slot, LegitAction.EQUIP_ARMOR, false);
			return;
		}
		
		// non-legit (old behavior)
		int slot = InventoryUtils.indexOf(Items.NETHERITE_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.DIAMOND_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.IRON_CHESTPLATE);
		if(slot == -1)
			return;
		
		boolean ok = InventoryUtils.selectItem(isChest, 36, true);
		if(!ok)
			return;
		
		try
		{
			IMC.getInteractionManager().rightClickItem();
		}catch(Throwable t)
		{
			try
			{
				if(MC.gameMode != null)
					MC.gameMode.useItem(MC.player, InteractionHand.MAIN_HAND);
			}catch(Throwable ignored)
			{}
		}
	}
	
	private void startLegitSequence(int selectSlot, LegitAction action,
		boolean restoreToMaceIfPossible)
	{
		if(MC.player == null)
			return;
		if(legitSequenceActive())
			return;
		
		pendingSelectedSlot = selectSlot;
		pendingRestoreSlot = MC.player.getInventory().getSelectedSlot();
		selectHotbar(selectSlot);
		
		int preMs = Math.max(legitPreDelayMs.getValueI(), 80);
		int postMs = Math.max(legitPostDwellMs.getValueI(), 120);
		
		long now = System.nanoTime();
		legitUseAtNs = now + TimeUnit.MILLISECONDS.toNanos(preMs);
		legitRestoreAtNs = 0L;
		pendingLegitAction = action;
		
		if(action == LegitAction.WIND && restoreToMaceIfPossible)
			pendingRestoreSlot = -2;
	}
	
	private void driveLegitSequence(long now)
	{
		if(!legitSequenceActive() || MC.player == null)
			return;
		
		// Use phase
		if(legitRestoreAtNs == 0L && now >= legitUseAtNs)
		{
			boolean used = false;
			try
			{
				switch(pendingLegitAction)
				{
					case WIND:
					{
						BlockPos below = MC.player.blockPosition().below();
						Vec3 hitVec = Vec3.atCenterOf(below);
						IMC.getInteractionManager().rightClickBlock(below,
							Direction.UP, hitVec);
						used = true;
						lastBurstNs = now;
						break;
					}
					case EQUIP_ELYTRA:
					case EQUIP_ARMOR:
					{
						try
						{
							IMC.getInteractionManager().rightClickItem();
						}catch(Throwable t)
						{
							if(MC.gameMode != null)
								MC.gameMode.useItem(MC.player,
									InteractionHand.MAIN_HAND);
						}
						used = true;
						break;
					}
					default:
					break;
				}
			}catch(Throwable ignored)
			{}
			
			int postMs = Math.max(legitPostDwellMs.getValueI(), 120);
			long dwell = TimeUnit.MILLISECONDS.toNanos(postMs);
			legitRestoreAtNs =
				now + (used ? dwell : TimeUnit.MILLISECONDS.toNanos(80));
		}
		
		// Restore phase
		if(legitRestoreAtNs != 0L && now >= legitRestoreAtNs)
		{
			boolean restored = false;
			
			if(pendingLegitAction == LegitAction.WIND
				&& !preferFireworks.isChecked())
			{
				if(pendingRestoreSlot == -2)
				{
					restored = selectMaceHotbar();
					if(!restored && pendingSelectedSlot >= 0)
						restored = selectHotbar(pendingSelectedSlot);
				}
			}
			
			if(!restored && pendingRestoreSlot >= 0)
				selectHotbar(pendingRestoreSlot);
			
			pendingLegitAction = LegitAction.NONE;
			pendingRestoreSlot = -1;
			legitUseAtNs = 0L;
			legitRestoreAtNs = 0L;
			pendingSelectedSlot = -1;
		}
	}
	
	private void equipElytraByUse()
	{
		if(legitMode.isChecked())
		{
			if(legitSequenceActive())
				return;
			int slot = findHotbarSlot(s -> s.is(Items.ELYTRA));
			if(slot == -1)
				return;
			startLegitSequence(slot, LegitAction.EQUIP_ELYTRA, false);
			return;
		}
		
		// non-legit (old behavior)
		if(InventoryUtils.selectItem(
			s -> s != null && !s.isEmpty() && s.is(Items.ELYTRA), 36, true))
		{
			try
			{
				IMC.getInteractionManager().rightClickItem();
			}catch(Throwable t)
			{
				try
				{
					if(MC.gameMode != null)
						MC.gameMode.useItem(MC.player,
							InteractionHand.MAIN_HAND);
				}catch(Throwable ignored)
				{}
			}
		}
	}
	
	private int findHotbarSlot(Predicate<ItemStack> pred)
	{
		if(MC.player == null)
			return -1;
		for(int i = 0; i < 9; i++)
		{
			ItemStack s = MC.player.getInventory().getItem(i);
			if(s != null && !s.isEmpty() && pred.test(s))
				return i;
		}
		return -1;
	}
	
	private boolean selectHotbar(int slot)
	{
		if(slot < 0 || slot > 8 || MC.player == null)
			return false;
		MC.player.getInventory().setSelectedSlot(slot);
		return true;
	}
	
	private boolean legitSequenceActive()
	{
		return pendingLegitAction != LegitAction.NONE;
	}
	
	private boolean selectHotbar(Predicate<ItemStack> pred)
	{
		return selectHotbar(findHotbarSlot(pred));
	}
	
	private boolean selectMaceHotbar()
	{
		return selectHotbar(s -> s.getItem() instanceof MaceItem
			|| s.getHoverName().getString().toLowerCase().contains("mace"));
	}
	
	@Override
	public void onHandleInput()
	{}
	
	@Override
	public void onRender(PoseStack poseStack, float partialTicks)
	{}
	
	public void toggleAutoMace()
	{
		setEnabled(!isEnabled());
	}
	
	private void chooseHandItem(boolean onGround)
	{
		if(legitMode.isChecked() && legitSequenceActive())
			return;
		
		if(preferFireworks.isChecked())
		{
			if(legitMode.isChecked())
			{
				if(!selectHotbar(s -> s != null && !s.isEmpty()
					&& s.is(Items.FIREWORK_ROCKET)))
				{
					// optional HUD message
				}
			}else
			{
				InventoryUtils.selectItem(s -> s != null && !s.isEmpty()
					&& s.is(Items.FIREWORK_ROCKET), 36, false);
			}
			return;
		}
		
		// Prefer Elytra in air but still HOLD MACE
		if(preferElytraAir.isChecked())
		{
			if(legitMode.isChecked())
			{
				selectHotbar(s -> s != null && !s.isEmpty()
					&& (s.getItem() instanceof MaceItem || s.getHoverName()
						.getString().toLowerCase().contains("mace")));
			}else
			{
				InventoryUtils.selectItem(
					s -> s != null && !s.isEmpty()
						&& (s.getItem() instanceof MaceItem || s.getHoverName()
							.getString().toLowerCase().contains("mace")),
					36, false);
			}
			return;
		}
		
		// Default: hold Mace
		if(legitMode.isChecked())
		{
			selectHotbar(s -> s != null && !s.isEmpty()
				&& (s.getItem() instanceof MaceItem || s.getHoverName()
					.getString().toLowerCase().contains("mace")));
		}else
		{
			InventoryUtils.selectItem(
				s -> s != null && !s.isEmpty()
					&& (s.getItem() instanceof MaceItem || s.getHoverName()
						.getString().toLowerCase().contains("mace")),
				36, false);
		}
	}
	
	/** Use wind burst by clicking the block directly below the player. */
	private boolean tryWindBurstAtFeet()
	{
		if(MC.player == null)
			return false;
		
		final String search = windBurstName.getValue().toLowerCase();
		Predicate<ItemStack> isWind =
			s -> s != null && !s.isEmpty() && (s.is(Items.WIND_CHARGE)
				|| s.getHoverName().getString().toLowerCase().contains(search));
		
		int oldSel = MC.player.getInventory().getSelectedSlot();
		
		if(legitMode.isChecked())
		{
			if(legitSequenceActive())
				return false;
			
			int windSlot = findHotbarSlot(isWind);
			if(windSlot == -1)
				return false;
			
			if(suppressMovementDuringBurst.isChecked())
			{
				Options opts = MC.options;
				KeyMapping[] bindings = {opts.keyUp, opts.keyDown, opts.keyLeft,
					opts.keyRight, opts.keyJump, opts.keyShift};
				for(KeyMapping b : bindings)
					b.setDown(false);
			}
			
			startLegitSequence(windSlot, LegitAction.WIND,
				!preferFireworks.isChecked());
			return true;
		}
		
		// non-legit
		lastHeldSlot = oldSel;
		boolean selected = InventoryUtils.selectItem(isWind, 36, true);
		if(!selected)
			return false;
		
		if(suppressMovementDuringBurst.isChecked())
		{
			Options opts = MC.options;
			KeyMapping[] bindings = {opts.keyUp, opts.keyDown, opts.keyLeft,
				opts.keyRight, opts.keyJump, opts.keyShift};
			for(KeyMapping b : bindings)
				b.setDown(false);
		}
		
		BlockPos below = MC.player.blockPosition().below();
		Vec3 hitVec = Vec3.atCenterOf(below);
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
		if(MC.gameMode == null || MC.player == null || MC.level == null)
			return;
		
		long now = System.nanoTime();
		if(now - lastHitNs < TimeUnit.MILLISECONDS
			.toNanos(hitDelayMs.getValueI()))
			return;
		
		HitResult hit = MC.hitResult;
		if(hit == null || hit.getType() != HitResult.Type.ENTITY)
			return;
		
		Entity target;
		try
		{
			target = ((EntityHitResult)hit).getEntity();
		}catch(Throwable t)
		{
			return;
		}
		if(target == null || !target.isAlive())
			return;
		
		if(hitPlayersOnly.isChecked() && !(target instanceof Player))
			return;
		
		if(target instanceof Player p)
		{
			String name = getPlayerNameLower(p);
			if(name != null && ignoreSet.contains(name))
				return;
		}
		
		final double range = hitRange.getValue();
		Vec3 eyePos = MC.player.getEyePosition(1.0F);
		Vec3 targetCenter = target.getBoundingBox().getCenter();
		double dist2 = eyePos.distanceToSqr(targetCenter);
		if(dist2 > range * range)
			return;
		
		final double minDot = Math.cos(Math.toRadians(45.0));
		Vec3 look = MC.player.getViewVector(1.0F).normalize();
		Vec3 to = targetCenter.subtract(eyePos).normalize();
		if(look.dot(to) < minDot)
			return;
		
		try
		{
			MC.gameMode.attack(MC.player, target);
			MC.player.swing(InteractionHand.MAIN_HAND);
			lastHitNs = System.nanoTime();
		}catch(Throwable ignored)
		{}
	}
	
	private static String getPlayerNameLower(Player p)
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
