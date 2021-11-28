/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.command.CmdProcessor;
import net.wurstclient.command.Command;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IMiningToolItem;
import net.wurstclient.mixinterface.ISwordItem;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"oninvfull", "inventory", "full", "automatic"})
public final class OnInvFullHack extends Hack implements UpdateListener {
	public static String action = ".t OnInvFull";
	private CheckboxSetting debounce = new CheckboxSetting("Debounce",
			"Disable this to run the action every tick repeatedly until inventory is no longer full.", true);
	private boolean lastCheckFull = false;
	
	public OnInvFullHack() {
		super("OnInvFull");
		
		setCategory(Category.OTHER);
		addSetting(debounce);
	}
	
	@Override
	public void onEnable() {
		lastCheckFull = false;
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable() {
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate() {
		if (debounce.isChecked() && lastCheckFull) return;
		if (MC.player.getInventory().getEmptySlot() == -1) {
			//
		}
	}
	
	// Send message to chat
	private void sendMessage(String message) {
		if (message.startsWith(".")) {
			// Message is Wurst command
			WURST.getCmdProcessor().process(message.substring(1));
		} else {
			// Otherwise, send to chat
			ChatMessageC2SPacket packet = new ChatMessageC2SPacket(message);
			MC.getNetworkHandler().sendPacket(packet);
		}
	}
}
