package net.mersid.hacks;

import net.mersid.events.LeftUpEventListener;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.events.BlockBreakingProgressListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.WItem;

public class AutoToolHack extends Hack implements BlockBreakingProgressListener, LeftUpEventListener {
	
	private final CheckboxSetting useSwords = new CheckboxSetting("Use swords",
			"Uses swords to break\n" + "leaves, cobwebs, etc.", false);
		private final CheckboxSetting useHands =
			new CheckboxSetting(
				"Use hands", "Uses an empty hand or a\n"
					+ "non-damageable item when\n" + "no applicable tool is found.",
				true);
		private final CheckboxSetting repairMode = new CheckboxSetting(
			"Repair mode", "Won't use tools that are about to break.", false);
		
		private final CheckboxSetting ignoreOnSwordEquipped = new CheckboxSetting("Blacklist swords", "Disable when swords are equipped.", false);
		private final CheckboxSetting ignoreOnAxeEquipped = new CheckboxSetting("Blacklist axes", "Disable when axes are equipped.", false);
		//private final CheckboxSetting attackWithSlotOne = new CheckboxSetting("Attack with tool 1", "Attack with the tool in the first slot on the hotbar", false);
		
		private Integer defaultInventorySlot; // Default inventory slot before autotool hack is active. Null if not in use (not breaking anything)
		private boolean isBreakingBlock; // Tracker for whether player is currently breaking blocks
		private BlockPos lastBlockPos; // Tracker for the last block pos. If it is not the same as the event's, then different blocks, so allow event to go through.
		
	
	public AutoToolHack() {
		super("AutoTool",
				"Automatically equips the fastest\n"
					+ "applicable tool in your hotbar\n"
					+ "when you try to break a block.");
		setCategory(Category.BLOCKS);
		addSetting(useSwords);
		addSetting(useHands);
		addSetting(repairMode);
		addSetting(ignoreOnSwordEquipped);
		addSetting(ignoreOnAxeEquipped);
		//addSetting(attackWithSlotOne);
		
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(BlockBreakingProgressListener.class, this);
		EVENTS.add(LeftUpEventListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(BlockBreakingProgressListener.class, this);
		EVENTS.remove(LeftUpEventListener.class, this);
	}
	
	/**
	 * Runs constantly when a block is being broken
	 */
	@Override
	public void onBlockBreakingProgress(BlockBreakingProgressEvent event)
	{
		if (isBreakingBlock && (lastBlockPos == null || lastBlockPos == event.getBlockPos())) return; // 1.14.4 refactor - This method is called every tick
		PlayerEntity player = MC.player;
		Item mainHandItem = player.getMainHandStack().getItem();
		
		System.out.println(mainHandItem);
		if (ignoreOnSwordEquipped.isChecked() && mainHandItem instanceof SwordItem) return;
		if (ignoreOnAxeEquipped.isChecked() && mainHandItem instanceof AxeItem) return;
		
		if (defaultInventorySlot == null) defaultInventorySlot = player.inventory.selectedSlot;
		
		equipBestTool(event.getBlockPos(), useSwords.isChecked(),
			useHands.isChecked(), repairMode.isChecked());
		isBreakingBlock = true;
		lastBlockPos = event.getBlockPos();
	}
	
	@Override
	public void onLeftUp(LeftUpEvent event) {
		if (isBreakingBlock)
		{
			MC.player.inventory.selectedSlot = defaultInventorySlot;
			isBreakingBlock = false;
		}
		defaultInventorySlot = null;
		
	}
	
	// Following methods taken from 1.7.10 Wurst Client, ported by Mersid.
	public void equipBestTool(BlockPos pos, boolean useSwords, boolean useHands,
			boolean repairMode)
	{
		ClientPlayerEntity player = MC.player;
		if(player.abilities.creativeMode)
			return;
		
		BlockState state = BlockUtils.getState(pos);
		
		ItemStack heldItem = player.getMainHandStack();
		float bestSpeed = getDestroySpeed(heldItem, state);
		int bestSlot = -1;
		
		int fallbackSlot = -1;
		PlayerInventory inventory = player.inventory;
		
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == inventory.selectedSlot)
				continue;
			
			ItemStack stack = inventory.getInvStack(slot);
			
			if(fallbackSlot == -1 && !isDamageable(stack))
				fallbackSlot = slot;
			
			float speed = getDestroySpeed(stack, state);
			if(speed <= bestSpeed)
				continue;
			
			if(!useSwords && stack.getItem() instanceof SwordItem)
				continue;
			
			if(isTooDamaged(stack, repairMode))
				continue;
			
			bestSpeed = speed;
			bestSlot = slot;
		}
		
		boolean useFallback =
			isDamageable(heldItem) && (isTooDamaged(heldItem, repairMode)
				|| useHands && getDestroySpeed(heldItem, state) <= 1);
		
		if(bestSlot != -1)
		{
			inventory.selectedSlot = bestSlot;
			return;
		}
		
		if(!useFallback)
			return;
		
		if(fallbackSlot != -1)
		{
			inventory.selectedSlot = fallbackSlot;
			return;
		}
		
		if(isTooDamaged(heldItem, repairMode))
			if(inventory.selectedSlot == 8)
				inventory.selectedSlot = 0;
			else
				inventory.selectedSlot++;
	}
	
	private float getDestroySpeed(ItemStack stack, BlockState state)
	{
		float speed = WItem.getDestroySpeed(stack, state);
		
		if(speed > 1)
		{
			int efficiency = EnchantmentHelper
				.getLevel(Enchantments.EFFICIENCY, stack);
			if(efficiency > 0 && !WItem.isNullOrEmpty(stack))
				speed += efficiency * efficiency + 1;
		}
		
		return speed;
	}
	
	private boolean isDamageable(ItemStack stack)
	{
		return !WItem.isNullOrEmpty(stack) && stack.getItem().isDamageable();
	}
	
	private boolean isTooDamaged(ItemStack stack, boolean repairMode)
	{
		return repairMode && stack.getMaxDamage() - stack.getDamage() <= 4;
	}

}
