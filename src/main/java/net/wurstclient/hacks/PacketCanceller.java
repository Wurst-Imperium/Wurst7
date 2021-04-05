/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"packet","cancel"})
public final class PacketCanceller extends Hack implements PacketOutputListener
{
	private CheckboxSetting playerInputC2SPacket = new CheckboxSetting("PlayerInputC2SPacket",
		"",
		false);
	
	private CheckboxSetting playerMoveC2SPacket = new CheckboxSetting("PlayerMoveC2SPacket",
		"",
		false);
	
	private CheckboxSetting playerActionC2SPacket = new CheckboxSetting("PlayerActionC2SPacket",
		"",
		false);
	
	private CheckboxSetting updatePlayerAbilitiesC2SPacket = new CheckboxSetting("UpdatePlayerAbilitiesC2SPacket",
		"",
		false);
	
	private CheckboxSetting playerInteractBlockC2SPacket = new CheckboxSetting("PlayerInteractBlockC2SPacket",
		"",
		false);
	
	private CheckboxSetting playerInteractEntityC2SPacket = new CheckboxSetting("PlayerInteractEntityC2SPacket",
		"",
		false);
	
	private CheckboxSetting playerInteractItemC2SPacket = new CheckboxSetting("PlayerInteractItemC2SPacket",
		"",
		false);
	
	private CheckboxSetting vehicleMoveC2SPacket = new CheckboxSetting("VehicleMoveC2SPacket",
		"",
		false);
	
	private CheckboxSetting teleportConfirmC2SPacket = new CheckboxSetting("TeleportConfirmC2SPacket",
		"",
		false);
	
	public PacketCanceller()
	{
		super("PacketCanceller",
			"Packet Canceller");
		setCategory(Category.OTHER);
		addSetting(playerInputC2SPacket);
		addSetting(playerMoveC2SPacket);
		addSetting(playerActionC2SPacket);
		addSetting(updatePlayerAbilitiesC2SPacket);
		addSetting(playerInteractBlockC2SPacket);
		addSetting(playerInteractEntityC2SPacket);
		addSetting(playerInteractItemC2SPacket);
		addSetting(vehicleMoveC2SPacket);
		addSetting(teleportConfirmC2SPacket);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if ((event.getPacket() instanceof PlayerInputC2SPacket && playerInputC2SPacket.isChecked()) ||
			(event.getPacket() instanceof PlayerMoveC2SPacket && playerMoveC2SPacket.isChecked()) ||
			(event.getPacket() instanceof PlayerActionC2SPacket && playerActionC2SPacket.isChecked()) ||
			(event.getPacket() instanceof UpdatePlayerAbilitiesC2SPacket && updatePlayerAbilitiesC2SPacket.isChecked()) ||
			(event.getPacket() instanceof PlayerInteractBlockC2SPacket && playerInteractBlockC2SPacket.isChecked()) ||
			(event.getPacket() instanceof PlayerInteractEntityC2SPacket && playerInteractEntityC2SPacket.isChecked()) ||
			(event.getPacket() instanceof PlayerInteractItemC2SPacket && playerInteractItemC2SPacket.isChecked()) ||
			(event.getPacket() instanceof VehicleMoveC2SPacket && vehicleMoveC2SPacket.isChecked()) ||
			(event.getPacket() instanceof TeleportConfirmC2SPacket && teleportConfirmC2SPacket.isChecked()))
        {
			event.cancel();
        }
	}
}
