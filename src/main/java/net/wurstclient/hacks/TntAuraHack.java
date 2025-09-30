/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.chunk.ChunkUtils;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMinecraftClient;

@SearchTags({"tnt aura", "tnt-aura", "dispenser refill", "refill dispensers"})
public final class TntAuraHack extends Hack
	implements UpdateListener, RenderListener
{
	private final SliderSetting radius = new SliderSetting("Radius",
		"How far (blocks) to search for dispensers.", 6, 1, 32, 0.25,
		ValueDisplay.DECIMAL);
	
	private final SliderSetting amount = new SliderSetting("Amount",
		"How much TNT to move into each dispenser per refill.", 16, 1, 64, 1,
		ValueDisplay.INTEGER);
	
	private final CheckboxSetting checkLOS = new CheckboxSetting(
		"Check line of sight",
		"Only refill dispensers that are visible to the player (raycast to block center).",
		true);
	
	private final SliderSetting tickDelay = new SliderSetting("Tick delay",
		"Ticks between scans.", 10, 1, 100, 1, ValueDisplay.INTEGER);
	
	private final SliderSetting maxPerRun =
		new SliderSetting("Max/Run", "How many dispensers to refill per scan.",
			2, 1, 16, 1, ValueDisplay.INTEGER);
	
	private final CheckboxSetting previewOnly =
		new CheckboxSetting("Preview only",
			"Don't actually move items, only highlight dispensers.", false);
	
	private final Path configFile = Path.of("./tnt_aura.cfg");
	
	private int tickCounter = 0;
	private final MinecraftClient mc = MinecraftClient.getInstance();
	private static final IMinecraftClient IMC = WurstClient.IMC;
	
	// Pending operation state (we only process one open at a time to keep logic
	// simple)
	private BlockPos pendingPos = null;
	private int pendingTicks = 0;
	private int pendingPlayerSlot = -1;
	private int pendingToMove = 0;
	private final java.util.Set<BlockPos> processing =
		new java.util.HashSet<>();
	
	public TntAuraHack()
	{
		super("TntAura");
		setCategory(Category.OTHER);
		
		addSetting(radius);
		addSetting(amount);
		addSetting(checkLOS);
		addSetting(tickDelay);
		addSetting(maxPerRun);
		addSetting(previewOnly);
	}
	
	@Override
	protected void onEnable()
	{
		tickCounter = 0;
		loadConfig();
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		if(mc.player != null)
			mc.player.sendMessage(Text.of("TntAura enabled"), false);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		saveConfig();
		if(mc.player != null)
			mc.player.sendMessage(Text.of("TntAura disabled"), false);
	}
	
	@Override
	public void onUpdate()
	{
		// handle any scheduled transfer first (will spawn a worker thread)
		if(pendingPos != null)
			performPendingTransfer();
		
		if(mc.player == null || mc.world == null || mc.isPaused())
			return;
		
		tickCounter++;
		if(tickCounter < (int)tickDelay.getValue())
			return;
		tickCounter = 0;
		
		BlockPos origin = mc.player.getBlockPos();
		double r = radius.getValue();
		
		// ChunkUtils.getLoadedBlockEntities() returns a Stream<BlockEntity>
		List<BlockPos> dispensers = ChunkUtils.getLoadedBlockEntities()
			.filter(be -> be instanceof DispenserBlockEntity)
			.map(be -> be.getPos())
			.filter(p -> origin.getSquaredDistance(p) <= r * r)
			.collect(Collectors.toCollection(ArrayList::new));
		
		if(checkLOS.isChecked())
			dispensers = dispensers.stream().filter(this::canSeeBlock)
				.collect(Collectors.toList());
		
		if(dispensers.isEmpty())
			return;
		
		int handled = 0;
		for(BlockPos pos : dispensers)
		{
			if(handled >= (int)maxPerRun.getValue())
				break;
			if(!dispenserNeedsTnt(pos))
				continue;
			
			if(previewOnly.isChecked())
			{
				if(mc.player != null)
					mc.player
						.sendMessage(Text.of("TNT Aura preview: dispenser at "
							+ pos.toShortString()), false);
				handled++;
				continue;
			}
			
			boolean ok = tryRefillDispenser(pos, (int)amount.getValue());
			if(ok)
			{
				handled++;
				if(mc.player != null)
					mc.player
						.sendMessage(
							Text.of(
								"Refilled dispenser at " + pos.toShortString()
									+ " (+" + (int)amount.getValue() + " TNT)"),
							false);
			}
		}
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(mc.player == null || mc.world == null)
			return;
		
		BlockPos origin = mc.player.getBlockPos();
		double r = radius.getValue();
		
		List<BlockPos> nearby = ChunkUtils.getLoadedBlockEntities()
			.filter(be -> be instanceof DispenserBlockEntity)
			.map(be -> be.getPos())
			.filter(p -> origin.getSquaredDistance(p) <= r * r)
			.collect(Collectors.toCollection(ArrayList::new));
		
		if(checkLOS.isChecked())
			nearby = nearby.stream().filter(this::canSeeBlock)
				.collect(Collectors.toList());
		
		int color = 0xFFFF8800;
		for(BlockPos p : nearby)
		{
			Box b = new Box(p).contract(1 / 16.0);
			RenderUtils.drawOutlinedBox(matrixStack, b, color, false);
		}
	}
	
	private boolean canSeeBlock(BlockPos pos)
	{
		Vec3d eye = mc.player.getCameraPosVec(0F);
		Vec3d target = Vec3d.ofCenter(pos);
		var hit = mc.world.raycast(
			new RaycastContext(eye, target, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, mc.player));
		return hit.getBlockPos().equals(pos);
	}
	
	private boolean dispenserNeedsTnt(BlockPos pos)
	{
		BlockEntity be = mc.world.getBlockEntity(pos);
		if(!(be instanceof DispenserBlockEntity))
			return false;
		DispenserBlockEntity d = (DispenserBlockEntity)be;
		
		for(int i = 0; i < d.size(); i++)
		{
			try
			{
				ItemStack s = d.getStack(i);
				if(s == null || s.isEmpty())
					return true;
				if(s.getItem() == Items.TNT && s.getCount() < s.getMaxCount())
					return true;
			}catch(Exception ignored)
			{}
		}
		return false;
	}
	
	/**
	 * Schedule-based refill:
	 * - schedule the open and wait N ticks for the handler to appear
	 * - actual transfer is performed in performPendingTransfer() which spawns a
	 * background worker
	 * - returns true if we scheduled work (so caller can mark it as being
	 * processed),
	 * otherwise false (e.g. no TNT, already processing, etc.)
	 */
	private boolean tryRefillDispenser(BlockPos pos, int amountToPlace)
	{
		// don't start if we're already processing this pos
		if(processing.contains(pos))
			return false;
		
		// find TNT in inventory (local player slot index)
		int playerSlot = InventoryUtils.indexOf(Items.TNT);
		if(playerSlot == -1)
		{
			if(mc.player != null)
				mc.player.sendMessage(Text.of("[TntAura] no TNT in inventory"),
					false);
			return false;
		}
		
		// interact (open) the dispenser UI
		mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
			new BlockHitResult(Vec3d.ofCenter(pos),
				net.minecraft.util.math.Direction.UP, pos, false));
		
		// schedule it and mark as processing
		pendingPos = pos;
		pendingTicks = 2; // wait a couple of ticks for server to open the UI
		pendingPlayerSlot = playerSlot;
		pendingToMove = amountToPlace;
		processing.add(pos);
		
		if(mc.player != null)
			mc.player.sendMessage(Text.of("[TntAura] scheduled refill at "
				+ pos.toShortString() + " in " + pendingTicks + " ticks."),
				false);
		return true;
	}
	
	/**
	 * Called from onUpdate to process the scheduled transfer once ticks elapsed
	 * and the handler is open. This method will spawn a background worker
	 * thread
	 * to do GUI clicks (so we never block the main thread).
	 */
	private void performPendingTransfer()
	{
		if(pendingPos == null)
			return;
		if(mc.player == null)
		{
			cleanupPending();
			return;
		}
		
		// wait ticks before attempting to read handler
		if(pendingTicks > 0)
		{
			pendingTicks--;
			return;
		}
		
		ScreenHandler sh = mc.player.currentScreenHandler;
		if(sh == null)
		{
			// not open yet; try again next tick
			pendingTicks = 1;
			return;
		}
		
		// find a container slot that belongs to the dispenser (stop when player
		// inventory slots start)
		int targetSlot = -1;
		for(int i = 0; i < sh.slots.size(); i++)
		{
			Slot s = sh.getSlot(i);
			if(s == null)
				continue;
			if(s.inventory == mc.player.getInventory())
				break; // reached player slots
			try
			{
				if(!s.hasStack())
				{
					targetSlot = i;
					break;
				}
				ItemStack st = s.getStack();
				if(st != null && st.getItem() == Items.TNT
					&& st.getCount() < st.getMaxCount())
				{
					targetSlot = i;
					break;
				}
			}catch(Exception ignored)
			{}
		}
		
		if(targetSlot == -1)
		{
			// nothing to place into; close and cleanup
			mc.player.networkHandler
				.sendPacket(new CloseHandledScreenC2SPacket(sh.syncId));
			if(mc.player != null)
				mc.player.sendMessage(
					Text.of("[TntAura] dispenser appears full"), false);
			processing.remove(pendingPos);
			pendingPos = null;
			return;
		}
		
		int availableBefore = InventoryUtils.count(Items.TNT);
		int toMove = Math.min(pendingToMove, availableBefore);
		if(toMove <= 0)
		{
			mc.player.networkHandler
				.sendPacket(new CloseHandledScreenC2SPacket(sh.syncId));
			if(mc.player != null)
				mc.player.sendMessage(Text.of("[TntAura] nothing to move"),
					false);
			processing.remove(pendingPos);
			pendingPos = null;
			return;
		}
		
		// attempt to map player slot object inside the opened handler by
		// matching stack contents
		Slot playerSlotObj = null;
		ItemStack playerStack =
			mc.player.getInventory().getStack(pendingPlayerSlot);
		for(Slot s : sh.slots)
		{
			try
			{
				// prefer slots that reference the player inventory
				if(s.inventory == mc.player.getInventory())
				{
					ItemStack ss = s.getStack();
					if(ss != null && playerStack != null
						&& ss.getItem() == playerStack.getItem()
						&& ss.getCount() == playerStack.getCount())
					{
						playerSlotObj = s;
						break;
					}
				}
			}catch(Throwable ignored)
			{}
		}
		
		final BlockPos pos = pendingPos;
		final int finalTargetSlot = targetSlot;
		final Slot finalPlayerSlotObj = playerSlotObj;
		final ScreenHandler finalSh = sh;
		final int availableBeforeFinal = availableBefore;
		final int toMoveFinal = toMove;
		final int pendingPlayerSlotFinal = pendingPlayerSlot;
		
		// clear pending so we don't re-enter; processing set still prevents
		// duplicates
		pendingPos = null;
		
		// spawn worker
		Thread.ofPlatform().name("TntAura-Refill").daemon().start(() -> {
			boolean success = false;
			int attempts = 0;
			final int maxAttempts = 6;
			
			// --- read dispenser now and determine how much we actually need to
			// add ---
			int currentTotal = 0;
			try
			{
				var beInit = mc.world.getBlockEntity(pos);
				if(beInit instanceof DispenserBlockEntity)
				{
					DispenserBlockEntity dbeInit = (DispenserBlockEntity)beInit;
					for(int i = 0; i < dbeInit.size(); i++)
					{
						ItemStack s = dbeInit.getStack(i);
						if(s != null && !s.isEmpty()
							&& s.getItem() == Items.TNT)
							currentTotal += s.getCount();
					}
				}
			}catch(Throwable ignored)
			{}
			
			int targetAmount = (int)amount.getValue();
			if(currentTotal >= targetAmount)
			{
				// nothing to do — close and cleanup
				try
				{
					Thread.sleep(40);
				}catch(InterruptedException ignored)
				{}
				if(mc.player != null)
					mc.player.networkHandler.sendPacket(
						new CloseHandledScreenC2SPacket(finalSh.syncId));
				if(mc.player != null)
					mc.player
						.sendMessage(Text.of("[TntAura] dispenser already has "
							+ currentTotal + " TNT (>= target " + targetAmount
							+ "), skipping"), false);
				processing.remove(pos);
				return;
			}
			int needed = targetAmount - currentTotal;
			
			// Build list of container (dispenser) slot indices and prefer
			// partial slots first.
			int firstPlayerSlotIndex = finalSh.slots.size();
			for(int i = 0; i < finalSh.slots.size(); i++)
			{
				Slot s = finalSh.getSlot(i);
				if(s != null && s.inventory == mc.player.getInventory())
				{
					firstPlayerSlotIndex = i;
					break;
				}
			}
			final java.util.List<Integer> containerIndices =
				new java.util.ArrayList<>();
			for(int i = 0; i < firstPlayerSlotIndex; i++)
				containerIndices.add(i);
				
			// sort containerIndices by current slot count ascending (so
			// partially filled slots are filled first)
			containerIndices.sort((a, b) -> {
				int ca = 0, cb = 0;
				try
				{
					ItemStack sa = finalSh.getSlot(a).getStack();
					if(sa != null && !sa.isEmpty())
						ca = sa.getCount();
				}catch(Throwable ignored)
				{}
				try
				{
					ItemStack sb = finalSh.getSlot(b).getStack();
					if(sb != null && !sb.isEmpty())
						cb = sb.getCount();
				}catch(Throwable ignored)
				{}
				return Integer.compare(ca, cb);
			});
			
			// Primary: explicit pickup->place->pickup into container slots
			// until needed satisfied or until we run out of TNT
			try
			{
				for(int idx : containerIndices)
				{
					if(needed <= 0)
						break;
					Slot targetSlotObj = finalSh.getSlot(idx);
					if(targetSlotObj == null)
						continue;
						
					// keep trying on this slot until it's full or we've
					// satisfied the need
					while(needed > 0 && InventoryUtils.count(Items.TNT) > 0)
					{
						attempts++;
						try
						{
							// validate screen still matches
							if(mc.currentScreen == null
								|| !(mc.currentScreen instanceof HandledScreen<?>))
								throw new IllegalStateException(
									"screen closed");
							HandledScreen<?> cur =
								(HandledScreen<?>)mc.currentScreen;
							if(cur.getScreenHandler().syncId != finalSh.syncId)
								throw new IllegalStateException(
									"different handler");
								
							// If we have a mapped player slot object, use it,
							// else try to find a fresh mapping
							Slot playerObj = finalPlayerSlotObj;
							if(playerObj == null)
							{
								// attempt to find by stack contents each
								// iteration (fresh mapping)
								ItemStack plStack = mc.player.getInventory()
									.getStack(pendingPlayerSlotFinal);
								for(Slot s : finalSh.slots)
								{
									try
									{
										if(s.inventory == mc.player
											.getInventory())
										{
											ItemStack ss = s.getStack();
											if(ss != null && plStack != null
												&& ss.getItem() == plStack
													.getItem()
												&& ss.getCount() == plStack
													.getCount())
											{
												playerObj = s;
												break;
											}
										}
									}catch(Throwable ignored)
									{}
								}
							}
							
							if(playerObj == null)
							{
								// fallback to IMC pickup sequence if we can't
								// map slot object
								int pickupNetwork = InventoryUtils
									.toNetworkSlot(pendingPlayerSlotFinal);
								try
								{
									IMC.getInteractionManager()
										.windowClick_PICKUP(pickupNetwork);
								}catch(Throwable ignored)
								{}
								try
								{
									IMC.getInteractionManager()
										.windowClick_PICKUP(idx);
								}catch(Throwable ignored)
								{}
								try
								{
									IMC.getInteractionManager()
										.windowClick_PICKUP(pickupNetwork);
								}catch(Throwable ignored)
								{}
							}else
							{
								// do GUI pickup->place->pickup
								cur.onMouseClick(playerObj, playerObj.id, 0,
									SlotActionType.PICKUP);
								Thread.sleep(80);
								cur.onMouseClick(targetSlotObj,
									targetSlotObj.id, 0, SlotActionType.PICKUP);
								Thread.sleep(80);
								cur.onMouseClick(playerObj, playerObj.id, 0,
									SlotActionType.PICKUP);
								Thread.sleep(120);
							}
							
						}catch(InterruptedException ie)
						{
							Thread.currentThread().interrupt();
							break;
						}catch(Throwable t)
						{
							// if mapping failed or screen changed, break out
							break;
						}
						
						// re-read dispenser contents & recompute needed
						int newTotal = 0;
						try
						{
							var beCheck = mc.world.getBlockEntity(pos);
							if(beCheck instanceof DispenserBlockEntity)
							{
								DispenserBlockEntity dbeCheck =
									(DispenserBlockEntity)beCheck;
								for(int j = 0; j < dbeCheck.size(); j++)
								{
									ItemStack s = dbeCheck.getStack(j);
									if(s != null && !s.isEmpty()
										&& s.getItem() == Items.TNT)
										newTotal += s.getCount();
								}
							}
						}catch(Throwable ignored)
						{}
						needed = Math.max(0, targetAmount - newTotal);
						
						// safety: break if too many attempts
						if(attempts >= 20)
							break;
					}
					// if overall attempts exhausted, stop
					if(attempts >= 20)
						break;
				}
			}catch(Throwable ignored)
			{}
			
			// close and final diagnostic
			try
			{
				Thread.sleep(60);
			}catch(InterruptedException ignored)
			{}
			if(mc.player != null)
				mc.player.networkHandler.sendPacket(
					new CloseHandledScreenC2SPacket(finalSh.syncId));
			try
			{
				Thread.sleep(150);
			}catch(InterruptedException ignored)
			{}
			
			int movedAfter = 0;
			try
			{
				var beFin = mc.world.getBlockEntity(pos);
				if(beFin instanceof DispenserBlockEntity)
				{
					DispenserBlockEntity dbeFin = (DispenserBlockEntity)beFin;
					StringBuilder sb = new StringBuilder(
						"[TntAura] dispenser slots after fill:");
					for(int i = 0; i < dbeFin.size(); i++)
					{
						ItemStack s = dbeFin.getStack(i);
						int c = (s == null || s.isEmpty()) ? 0 : s.getCount();
						sb.append(" [").append(i).append("]=").append(c);
						movedAfter += c;
					}
					if(mc.player != null)
						mc.player.sendMessage(Text.of(sb.toString()), false);
				}
			}catch(Throwable ignored)
			{}
			
			int availableAfter = InventoryUtils.count(Items.TNT);
			if(mc.player != null)
				mc.player
					.sendMessage(
						Text.of("[TntAura] before=" + availableBeforeFinal
							+ " after=" + availableAfter
							+ " movedIntoDispenser(after)=" + movedAfter),
						false);
			
			processing.remove(pos);
		});
		
	}
	
	private void cleanupPending()
	{
		pendingPos = null;
		pendingTicks = 0;
		pendingPlayerSlot = -1;
		pendingToMove = 0;
	}
	
	private void saveConfig()
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			sb.append("radius=").append(radius.getValue()).append("\n");
			sb.append("amount=").append((int)amount.getValue()).append("\n");
			sb.append("checkLOS=").append(checkLOS.isChecked()).append("\n");
			sb.append("tickDelay=").append((int)tickDelay.getValue())
				.append("\n");
			sb.append("maxPerRun=").append((int)maxPerRun.getValue())
				.append("\n");
			sb.append("previewOnly=").append(previewOnly.isChecked())
				.append("\n");
			Files.writeString(configFile, sb.toString(),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING);
		}catch(IOException ignored)
		{}
	}
	
	private void loadConfig()
	{
		try
		{
			if(!Files.exists(configFile))
				return;
			List<String> lines = Files.readAllLines(configFile);
			for(String line : lines)
			{
				if(line.startsWith("radius="))
					radius.setValue(Double.parseDouble(line.split("=", 2)[1]));
				if(line.startsWith("amount="))
					amount.setValue(Double.parseDouble(line.split("=", 2)[1]));
				if(line.startsWith("checkLOS="))
					checkLOS.setChecked(
						Boolean.parseBoolean(line.split("=", 2)[1]));
				if(line.startsWith("tickDelay="))
					tickDelay
						.setValue(Double.parseDouble(line.split("=", 2)[1]));
				if(line.startsWith("maxPerRun="))
					maxPerRun
						.setValue(Double.parseDouble(line.split("=", 2)[1]));
				if(line.startsWith("previewOnly="))
					previewOnly.setChecked(
						Boolean.parseBoolean(line.split("=", 2)[1]));
			}
		}catch(IOException | NumberFormatException ignored)
		{}
	}
}
