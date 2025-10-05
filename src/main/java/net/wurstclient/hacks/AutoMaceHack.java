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
	
	/* Settings */
	private final TextFieldSetting windBurstName = new TextFieldSetting(
		"Wind burst item",
		"Substring to locate wind burst item (case-insensitive). Example: \"wind\"",
		"wind");
	
	private final CheckboxSetting elytraInAir = new CheckboxSetting(
		"Equip Elytra while airborne",
		"If enabled: when you’re in the air, equip Elytra.\nIf disabled: keep/restore armor.",
		true);
	
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
	
	/* Runtime state */
	private int lastHeldSlot = -1;
	private long lastActionNs = 0L;
	private boolean didBurstThisJump = false;
	private boolean wasOnGround = true;
	
	public AutoMaceHack()
	{
		super("AutoMace");
		setCategory(Category.COMBAT);
		addSetting(windBurstName);
		addSetting(elytraInAir);
		addSetting(autoHitOnDrop);
		addSetting(suppressMovementDuringBurst);
		addSetting(ignorePlayers);
		addSetting(actionDelayMs);
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
		
		boolean onGround = MC.player.isOnGround();
		if(onGround)
		{
			// Reset “one burst per jump”
			didBurstThisJump = false;
		}
		
		// Elytra/Armor policy: maintain based on "elytraInAir" and airborne
		// state
		maintainChestItem(onGround);
		
		// Throttle
		long now = System.nanoTime();
		if(now - lastActionNs < TimeUnit.MILLISECONDS
			.toNanos(actionDelayMs.getValueI()))
			return;
		
		// Pause if someone to ignore is nearby
		if(isIgnoredPlayerNearby())
			return;
		
		// Optional single tap while falling
		if(!onGround && autoHitOnDrop.isChecked()
			&& MC.player.getVelocity().y < -0.15)
			tryAutoHitCrosshair();
		
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
	
	public void dequipAndRestore()
	{
		if(MC.player == null || MC.world == null)
			return;
		// Force armor (user asked: de-equip should bring armor back & mace)
		ensureArmorEquipped();
		if(lastHeldSlot >= 0)
			MC.player.getInventory().setSelectedSlot(lastHeldSlot);
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
	
	/** Maintain Elytra vs Armor based on settings and airborne state. */
	private void maintainChestItem(boolean onGround)
	{
		ItemStack chest = MC.player.getEquippedStack(EquipmentSlot.CHEST);
		
		if(!onGround && elytraInAir.isChecked())
		{
			// Airborne + Elytra policy: ensure Elytra equipped
			if(chest.getItem() != Items.ELYTRA)
			{
				int elytraSlot = InventoryUtils.indexOf(Items.ELYTRA);
				if(elytraSlot != -1)
				{
					try
					{
						int net = InventoryUtils.toNetworkSlot(elytraSlot);
						IMC.getInteractionManager().windowClick_QUICK_MOVE(net);
					}catch(Throwable ignored)
					{}
				}
			}
		}else
		{
			// Grounded or Elytra policy disabled: ensure any chestplate is
			// equipped (not Elytra)
			// if wearing Elytra or empty, force switch to Netherite chestplate
			// if available
			if(chest.isEmpty() || chest.getItem() == Items.ELYTRA)
			{
				int chestSlot =
					InventoryUtils.indexOf(Items.NETHERITE_CHESTPLATE);
				if(chestSlot == -1)
					chestSlot =
						InventoryUtils.indexOf(Items.DIAMOND_CHESTPLATE);
				if(chestSlot == -1)
					chestSlot = InventoryUtils.indexOf(Items.IRON_CHESTPLATE);
				if(chestSlot != -1)
				{
					try
					{
						int net = InventoryUtils.toNetworkSlot(chestSlot);
						IMC.getInteractionManager().windowClick_QUICK_MOVE(net);
					}catch(Throwable ignored)
					{}
				}
			}
			
		}
	}
	
	private void ensureArmorEquipped()
	{
		// Prefer best available: Netherite -> Diamond -> Chain -> Iron -> Gold
		// -> Leather
		int slot = InventoryUtils.indexOf(Items.NETHERITE_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.DIAMOND_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.CHAINMAIL_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.IRON_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.GOLDEN_CHESTPLATE);
		if(slot == -1)
			slot = InventoryUtils.indexOf(Items.LEATHER_CHESTPLATE);
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
