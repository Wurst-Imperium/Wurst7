/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.StopUsingItemListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"arrow dmg", "ArrowDamage", "arrow damage"})
public final class ArrowDmgHack extends Hack implements StopUsingItemListener
{
	private final SliderSetting strength = new SliderSetting("Strength",
		"description.wurst.setting.arrowdmg.strength", 10, 0.1, 10, 0.1,
		ValueDisplay.DECIMAL);
	
	private final CheckboxSetting yeetTridents =
		new CheckboxSetting("Trident yeet mode",
			"description.wurst.setting.arrowdmg.trident_yeet_mode", false);
	
	public ArrowDmgHack()
	{
		super("ArrowDMG");
		setCategory(Category.COMBAT);
		addSetting(strength);
		addSetting(yeetTridents);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(StopUsingItemListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(StopUsingItemListener.class, this);
	}
	
	@Override
	public void onStopUsingItem()
	{
		LocalPlayer player = MC.player;
		ClientPacketListener netHandler = player.connection;
		
		if(!isValidItem(player.getMainHandItem().getItem()))
			return;
		
		netHandler.send(
			new ServerboundPlayerCommandPacket(player, Action.START_SPRINTING));
		
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		
		// See ServerPlayNetworkHandler.onPlayerMove()
		// for why it's using these numbers.
		// Also, let me know if you find a way to bypass that check in 1.21.
		double adjustedStrength = strength.getValue() / 10.0 * Math.sqrt(500);
		Vec3 lookVec = player.getViewVector(1).scale(adjustedStrength);
		for(int i = 0; i < 4; i++)
			sendPos(x, y, z, true);
		sendPos(x - lookVec.x, y, z - lookVec.z, true);
		sendPos(x, y, z, false);
	}
	
	private void sendPos(double x, double y, double z, boolean onGround)
	{
		ClientPacketListener netHandler = MC.player.connection;
		netHandler
			.send(new Pos(x, y, z, onGround, MC.player.horizontalCollision));
	}
	
	private boolean isValidItem(Item item)
	{
		if(yeetTridents.isChecked() && item == Items.TRIDENT)
			return true;
		
		return item == Items.BOW;
	}
}
