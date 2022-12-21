/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.StopUsingItemListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"arrow dmg", "ArrowDamage", "arrow damage"})
public final class ArrowDmgHack extends Hack implements StopUsingItemListener, UseItemCallback
{
	private final SliderSetting packets = new SliderSetting("Packets",
		"description.wurst.setting.arrowdmg.packets", 200, 2, 2000, 2,
		ValueDisplay.INTEGER);

	private final CheckboxSetting useoffhand =
			new CheckboxSetting("Use Offhand",
			"Whether to also use items in Offhand", false);
	
	private final CheckboxSetting yeetTridents =
		new CheckboxSetting("Trident yeet mode",
			"description.wurst.setting.arrowdmg.trident_yeet_mode", false);

	private final CheckboxSetting throwables =
		new CheckboxSetting(
				"Accelerate Throwables",
				"""
						Accelerates thrown items.
						(Enderpearls, Slpash Potions, Snowballs, Eggs, XP-Bottles)

						§c§lWARNING:§r§f It is recommended to use these with Packets-slider set to below
						50, otherwise the projectiles tend to break midair, in the player or go
						out of render distance and the accuracy is reduced greatly.
						§7§o(which sometimes is just visual?)§r§f""",
				false);
	
	public ArrowDmgHack()
	{
		super("ArrowDMG");
		setCategory(Category.COMBAT);
		addSetting(packets);
		addSetting(useoffhand);
		addSetting(yeetTridents);
		addSetting(throwables);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(StopUsingItemListener.class, this);
		UseItemCallback.EVENT.register(this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(StopUsingItemListener.class, this);
		//TODO: maybe create a Listener to be able to unregister the callbacks of UseItemCallback;
	}
	
	@Override
	public void onStopUsingItem()
	{
		ClientPlayerEntity player = MC.player;
		ClientPlayNetworkHandler netHandler = player.networkHandler;
		
		if(!isValidItem(player.getMainHandStack().getItem()) && (!useoffhand.isChecked() && !isValidItem(player.getOffHandStack().getItem())))
			return;

		sendSprintPackets(player, netHandler);
	}

	private void sendSprintPackets(ClientPlayerEntity player, ClientPlayNetworkHandler netHandler)
	{
		netHandler.sendPacket(
				new ClientCommandC2SPacket(player, Mode.START_SPRINTING));

		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();

		for(int i = 0; i < packets.getValueI() / 2; i++)
		{
			netHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x,
					y - 1e-10, z, true));
			netHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x,
					y + 1e-10, z, false));
		}
	}
	
	private boolean isValidItem(Item item)
	{
		if(yeetTridents.isChecked() && item == Items.TRIDENT)
			return true;
		
		return item == Items.BOW;
	}

	private boolean isValidThrowable(Item item)
	{
		return  item == Items.ENDER_PEARL ||
				item == Items.SNOWBALL ||
				item == Items.EGG ||
				item == Items.EXPERIENCE_BOTTLE ||
				item == Items.SPLASH_POTION;
	}

	@Override
	public TypedActionResult<ItemStack> interact(PlayerEntity player, World world, Hand hand)
	{
		if(!throwables.isChecked() || !isValidThrowable(player.getStackInHand(hand).getItem()) || !this.isEnabled())
			return TypedActionResult.pass(player.getStackInHand(hand));

		this.sendSprintPackets(MC.player, MC.player.networkHandler);
		return TypedActionResult.pass(player.getStackInHand(hand));
	}
}
