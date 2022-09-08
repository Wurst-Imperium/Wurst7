/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;

@SearchTags({"crystal aura"})
public final class CrystalAuraHack extends Hack implements UpdateListener
{
	private final SliderSetting range = new SliderSetting("Range",
		"Determines how far CrystalAura will reach\n"
			+ "to place and detonate crystals.",
		6, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting autoPlace =
		new CheckboxSetting("Auto-place crystals",
			"When enabled, CrystalAura will automatically\n"
				+ "place crystals near valid entities.\n"
				+ "When disabled, CrystalAura will only\n"
				+ "detonate manually placed crystals.",
			true);
	
	private final EnumSetting<FaceBlocks> faceBlocks =
		new EnumSetting<>("Face crystals",
			"Whether or not CrystalAura should face\n"
				+ "the correct direction when placing and\n"
				+ "left-clicking end crystals.\n\n"
				+ "Slower but can help with anti-cheat\n" + "plugins.",
			FaceBlocks.values(), FaceBlocks.OFF);
	
	private final CheckboxSetting checkLOS =
		new CheckboxSetting("Check line of sight",
			"Ensures that you don't reach through\n"
				+ "blocks when placing or left-clicking\n" + "end crystals.\n\n"
				+ "Slower but can help with anti-cheat\n" + "plugins.",
			false);
	
	private final EnumSetting<TakeItemsFrom> takeItemsFrom =
		new EnumSetting<>("Take items from", "Where to look for end crystals.",
			TakeItemsFrom.values(), TakeItemsFrom.INVENTORY);
	
	private final CheckboxSetting filterPlayers =
		new CheckboxSetting("Filter players",
			"Won't target other players\n" + "when auto-placing crystals.\n\n"
				+ "They can still take damage if\n"
				+ "they get too close to a valid\n"
				+ "target or an existing crystal.",
			false);
	
	private final CheckboxSetting filterMonsters =
		new CheckboxSetting("Filter monsters",
			"Won't target zombies, creepers, etc.\n"
				+ "when auto-placing crystals.\n\n"
				+ "They can still take damage if\n"
				+ "they get too close to a valid\n"
				+ "target or an existing crystal.",
			true);
	
	private final CheckboxSetting filterAnimals = new CheckboxSetting(
		"Filter animals",
		"Won't target pigs, cows, etc.\n" + "when auto-placing crystals.\n\n"
			+ "They can still take damage if\n"
			+ "they get too close to a valid\n"
			+ "target or an existing crystal.",
		true);
	
	private final CheckboxSetting filterTraders =
		new CheckboxSetting("Filter traders",
			"Won't target villagers, wandering traders, etc.\n"
				+ "when auto-placing crystals.\n\n"
				+ "They can still take damage if\n"
				+ "they get too close to a valid\n"
				+ "target or an existing crystal.",
			true);
	
	private final CheckboxSetting filterGolems =
		new CheckboxSetting("Filter golems",
			"Won't target iron golems,\n" + "snow golems and shulkers.\n"
				+ "when auto-placing crystals.\n\n"
				+ "They can still take damage if\n"
				+ "they get too close to a valid\n"
				+ "target or an existing crystal.",
			true);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"Filter invisible",
		"Won't target invisible entities\n" + "when auto-placing crystals.\n\n"
			+ "They can still take damage if\n"
			+ "they get too close to a valid\n"
			+ "target or an existing crystal.",
		false);
	
	private final CheckboxSetting filterNamed =
		new CheckboxSetting("Filter named",
			"Won't target name-tagged entities\n"
				+ "when auto-placing crystals.\n\n"
				+ "They can still take damage if\n"
				+ "they get too close to a valid\n"
				+ "target or an existing crystal.",
			false);
	
	private final CheckboxSetting filterStands =
		new CheckboxSetting("Filter armor stands",
			"Won't target armor stands.\n" + "when auto-placing crystals.\n\n"
				+ "They can still take damage if\n"
				+ "they get too close to a valid\n"
				+ "target or an existing crystal.",
			true);
	
	public CrystalAuraHack()
	{
		super("CrystalAura", "Automatically places (optional) and\n"
			+ "detonates end crystals to kill\n" + "entities around you.");
		
		setCategory(Category.COMBAT);
		addSetting(range);
		addSetting(autoPlace);
		addSetting(faceBlocks);
		addSetting(checkLOS);
		addSetting(takeItemsFrom);
		
		addSetting(filterPlayers);
		addSetting(filterMonsters);
		addSetting(filterAnimals);
		addSetting(filterTraders);
		addSetting(filterGolems);
		addSetting(filterInvisible);
		addSetting(filterNamed);
		addSetting(filterStands);
	}
	
	@Override
	public void onEnable()
	{
		// disable other killauras
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().triggerBotHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ArrayList<Entity> crystals = getNearbyCrystals();
		
		if(!crystals.isEmpty())
		{
			detonate(crystals);
			return;
		}
		
		if(!autoPlace.isChecked()
			|| !hasItem(item -> item == Items.END_CRYSTAL))
			return;
		
		ArrayList<Entity> targets = getNearbyTargets();
		placeCrystalsNear(targets);
	}
	
	private ArrayList<BlockPos> placeCrystalsNear(ArrayList<Entity> targets)
	{
		ArrayList<BlockPos> newCrystals = new ArrayList<>();
		
		boolean shouldSwing = false;
		for(Entity target : targets)
		{
			ArrayList<BlockPos> freeBlocks = getFreeBlocksNear(target);
			
			for(BlockPos pos : freeBlocks)
				if(placeCrystal(pos))
				{
					shouldSwing = true;
					newCrystals.add(pos);
					
					// TODO optional speed limit(?)
					break;
				}
		}
		
		if(shouldSwing)
			MC.player.swingHand(Hand.MAIN_HAND);
		
		return newCrystals;
	}
	
	private void detonate(ArrayList<Entity> crystals)
	{
		for(Entity e : crystals)
		{
			faceBlocks.getSelected().face(e.getBoundingBox().getCenter());
			MC.interactionManager.attackEntity(MC.player, e);
		}
		
		if(!crystals.isEmpty())
			MC.player.swingHand(Hand.MAIN_HAND);
	}
	
	private boolean selectItem(Predicate<Item> item)
	{
		PlayerInventory inventory = MC.player.inventory;
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		int maxInvSlot = takeItemsFrom.getSelected().maxInvSlot;
		
		for(int slot = 0; slot < maxInvSlot; slot++)
		{
			ItemStack stack = inventory.getStack(slot);
			if(!item.test(stack.getItem()))
				continue;
			
			if(slot < 9)
				inventory.selectedSlot = slot;
			else if(inventory.getEmptySlot() < 9)
				im.windowClick_QUICK_MOVE(slot);
			else if(inventory.getEmptySlot() != -1)
			{
				im.windowClick_QUICK_MOVE(inventory.selectedSlot + 36);
				im.windowClick_QUICK_MOVE(slot);
			}else
			{
				im.windowClick_PICKUP(inventory.selectedSlot + 36);
				im.windowClick_PICKUP(slot);
				im.windowClick_PICKUP(inventory.selectedSlot + 36);
			}
			
			return true;
		}
		
		return false;
	}
	
	private boolean hasItem(Predicate<Item> item)
	{
		PlayerInventory inventory = MC.player.inventory;
		int maxInvSlot = takeItemsFrom.getSelected().maxInvSlot;
		
		for(int slot = 0; slot < maxInvSlot; slot++)
		{
			ItemStack stack = inventory.getStack(slot);
			if(!item.test(stack.getItem()))
				continue;
			
			return true;
		}
		
		return false;
	}
	
	private boolean placeCrystal(BlockPos pos)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		double rangeSq = Math.pow(range.getValue(), 2);
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.offset(side);
			
			// check if neighbor can be right clicked
			if(!isClickableNeighbor(neighbor))
				continue;
			
			Vec3d dirVec = Vec3d.of(side.getVector());
			Vec3d hitVec = posVec.add(dirVec.multiply(0.5));
			
			// check if hitVec is within range
			if(eyesPos.squaredDistanceTo(hitVec) > rangeSq)
				continue;
			
			// check if side is visible (facing away from player)
			if(distanceSqPosVec > eyesPos.squaredDistanceTo(posVec.add(dirVec)))
				continue;
			
			if(checkLOS.isChecked() && MC.world
				.raycast(new RaycastContext(eyesPos, hitVec,
					RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, MC.player))
				.getType() != HitResult.Type.MISS)
				continue;
			
			if(!selectItem(item -> item == Items.END_CRYSTAL))
				return false;
			
			faceBlocks.getSelected().face(hitVec);
			
			// place block
			IMC.getInteractionManager().rightClickBlock(neighbor,
				side.getOpposite(), hitVec);
			
			return true;
		}
		
		return false;
	}
	
	private ArrayList<Entity> getNearbyCrystals()
	{
		ClientPlayerEntity player = MC.player;
		double rangeSq = Math.pow(range.getValue(), 2);
		
		Comparator<Entity> furthestFromPlayer = Comparator
			.<Entity> comparingDouble(e -> MC.player.squaredDistanceTo(e))
			.reversed();
		
		return StreamSupport.stream(MC.world.getEntities().spliterator(), true)
			.filter(e -> e instanceof EndCrystalEntity).filter(e -> !e.removed)
			.filter(e -> player.squaredDistanceTo(e) <= rangeSq)
			.sorted(furthestFromPlayer)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private ArrayList<Entity> getNearbyTargets()
	{
		double rangeSq = Math.pow(range.getValue(), 2);
		
		Comparator<Entity> furthestFromPlayer = Comparator
			.<Entity> comparingDouble(e -> MC.player.squaredDistanceTo(e))
			.reversed();
		
		Stream<Entity> stream =
			StreamSupport.stream(MC.world.getEntities().spliterator(), false)
				.filter(e -> !e.removed)
				.filter(e -> e instanceof LivingEntity
					&& ((LivingEntity)e).getHealth() > 0)
				.filter(e -> e != MC.player)
				.filter(e -> !(e instanceof FakePlayerEntity))
				.filter(e -> !WURST.getFriends().contains(e.getEntityName()))
				.filter(e -> MC.player.squaredDistanceTo(e) <= rangeSq);
		
		if(filterPlayers.isChecked())
			stream = stream.filter(e -> !(e instanceof PlayerEntity));
		
		if(filterMonsters.isChecked())
			stream = stream.filter(e -> !(e instanceof Monster));
		
		if(filterAnimals.isChecked())
			stream = stream.filter(
				e -> !(e instanceof AnimalEntity || e instanceof AmbientEntity
					|| e instanceof WaterCreatureEntity));
		
		if(filterTraders.isChecked())
			stream = stream.filter(e -> !(e instanceof MerchantEntity));
		
		if(filterGolems.isChecked())
			stream = stream.filter(e -> !(e instanceof GolemEntity));
		
		if(filterInvisible.isChecked())
			stream = stream.filter(e -> !e.isInvisible());
		
		if(filterNamed.isChecked())
			stream = stream.filter(e -> !e.hasCustomName());
		
		if(filterStands.isChecked())
			stream = stream.filter(e -> !(e instanceof ArmorStandEntity));
		
		return stream.sorted(furthestFromPlayer)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private ArrayList<BlockPos> getFreeBlocksNear(Entity target)
	{
		Vec3d eyesVec = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		double rangeD = range.getValue();
		double rangeSq = Math.pow(rangeD + 0.5, 2);
		int rangeI = 2;
		
		BlockPos center = target.getBlockPos();
		BlockPos min = center.add(-rangeI, -rangeI, -rangeI);
		BlockPos max = center.add(rangeI, rangeI, rangeI);
		Box targetBB = target.getBoundingBox();
		
		Vec3d targetEyesVec =
			target.getPos().add(0, target.getEyeHeight(target.getPose()), 0);
		
		Comparator<BlockPos> closestToTarget =
			Comparator.<BlockPos> comparingDouble(
				pos -> targetEyesVec.squaredDistanceTo(Vec3d.ofCenter(pos)));
		
		return BlockUtils.getAllInBoxStream(min, max)
			.filter(pos -> eyesVec.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(this::isReplaceable).filter(this::hasCrystalBase)
			.filter(pos -> !targetBB.intersects(new Box(pos)))
			.sorted(closestToTarget)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private boolean isReplaceable(BlockPos pos)
	{
		return BlockUtils.getState(pos).getMaterial().isReplaceable();
	}
	
	private boolean hasCrystalBase(BlockPos pos)
	{
		Block block = BlockUtils.getBlock(pos.down());
		return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN;
	}
	
	private boolean isClickableNeighbor(BlockPos pos)
	{
		return BlockUtils.canBeClicked(pos)
			&& !BlockUtils.getState(pos).getMaterial().isReplaceable();
	}
	
	private enum FaceBlocks
	{
		OFF("Off", v -> {}),
		
		SERVER("Server-side",
			v -> WURST.getRotationFaker().faceVectorPacket(v)),
		
		CLIENT("Client-side",
			v -> WURST.getRotationFaker().faceVectorClient(v)),
		
		SPAM("Packet spam", v -> {
			Rotation rotation = RotationUtils.getNeededRotations(v);
			PlayerMoveC2SPacket.LookOnly packet =
				new PlayerMoveC2SPacket.LookOnly(rotation.getYaw(),
					rotation.getPitch(), MC.player.isOnGround());
			MC.player.networkHandler.sendPacket(packet);
		});
		
		private String name;
		private Consumer<Vec3d> face;
		
		private FaceBlocks(String name, Consumer<Vec3d> face)
		{
			this.name = name;
			this.face = face;
		}
		
		public void face(Vec3d v)
		{
			face.accept(v);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	private enum TakeItemsFrom
	{
		HOTBAR("Hotbar", 9),
		
		INVENTORY("Inventory", 36);
		
		private final String name;
		private final int maxInvSlot;
		
		private TakeItemsFrom(String name, int maxInvSlot)
		{
			this.name = name;
			this.maxInvSlot = maxInvSlot;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
