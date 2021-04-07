/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({ "spambot", "chatspam", "spammer" })
public final class SpambotHack extends Hack implements UpdateListener {
	private int timer;
	
	private final SliderSetting spamDelay = new SliderSetting("Speed",
		"The message send rate of the spambot.\n"
		+ "Lower = faster.",
		6, 0, 60, 0.5, ValueDisplay.DECIMAL);

	public SpambotHack() {
		super("SpambotHack", "Spams the chat!\n\n"
				+ "Make sure you're using .say if your message.\n"
				+ "starts with a dot.");
		setCategory(Category.CHAT);
		addSetting(spamDelay)
	}

	@Override
	public void onEnable() {
		EVENTS.add(UpdateListener.class, this);
		timer = spamDelay.getValueI()
	}

	@Override
	public void onDisable() {
		// force close spambot window, stop spamming
		EVENTS.remove(UpdateListener.class, this);
	}

	@Override
	public void onUpdate() {
		if (timer > 0) {
			timer--; return
		} sendMessage("hi");
	}

	public void sendMessage(String message) {
		ChatMessageC2SPacket packet = new ChatMessageC2SPacket(message);
		MC.getNetworkHandler().sendPacket(packet);
	}
}
