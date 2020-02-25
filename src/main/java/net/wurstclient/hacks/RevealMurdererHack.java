/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;

@SearchTags({"murder mystery"})
public final class RevealMurdererHack extends Hack
	implements PacketInputListener
{
	private static Set<Item> knifeItems = Stream.of(
		Items.IRON_SWORD,
		Items.STONE_SWORD,
		Items.IRON_SHOVEL,
		Items.STICK,
		Items.WOODEN_AXE,
		Items.WOODEN_SWORD,
		Item.fromBlock(Blocks.DEAD_BUSH),
		Items.STONE_SHOVEL,
		Items.BLAZE_ROD,
		Items.DIAMOND_SHOVEL,
		Items.FEATHER,
		Items.PUMPKIN_PIE,
		Items.GOLDEN_PICKAXE,
		Items.APPLE,
		Items.NAME_TAG,
		Item.fromBlock(Blocks.SPONGE),
		Items.CARROT_ON_A_STICK,
		Items.BONE,
		Items.CARROT,
		Items.GOLDEN_CARROT,
		Items.COOKIE,
		Items.DIAMOND_AXE,
		Items.GOLDEN_SWORD,
		Items.DIAMOND_SWORD,
		Items.DIAMOND_HOE,
		Items.SHEARS,
		Items.SALMON,
		Item.fromBlock(Blocks.REDSTONE_TORCH)
	).collect(Collectors.toSet());

	private Set<PlayerEntity> murderers = new HashSet<>();

	public RevealMurdererHack()
	{
		super("RevealMurderer", "Shows the murderer name when a murderer holds a knife.\n"
			+ "Made for Hypixel Murder Mystery.\n"
			+ "Might not work on other servers.");
		
		setCategory(Category.OTHER);
	}
	
	@Override
	public void onEnable()
	{
		murderers.clear();

		EVENTS.add(PacketInputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		murderers.clear();

		EVENTS.remove(PacketInputListener.class, this);
	}
	
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		if(event.getPacket() instanceof EntityEquipmentUpdateS2CPacket)
		{
			EntityEquipmentUpdateS2CPacket packet = (EntityEquipmentUpdateS2CPacket)event.getPacket();
			Entity entity = MC.world.getEntityById(packet.getId());

			if(entity == null) return;
			if(!(entity instanceof PlayerEntity)) return;
			if(murderers.contains(entity)) return;

			if(packet.getSlot() != EquipmentSlot.MAINHAND) return;
			Item item = packet.getStack().getItem();
			if(!knifeItems.contains(item)) return;

			PlayerEntity murderer = (PlayerEntity)entity;
			murderers.add(murderer);
			ChatUtils.message(murderer.getName().asString() + " is a murderer. (knife: " + item.getName().asString()  + ")");
		}
		else if(event.getPacket() instanceof PlayerRespawnS2CPacket)
		{
			murderers.clear();
		}
	}
}
