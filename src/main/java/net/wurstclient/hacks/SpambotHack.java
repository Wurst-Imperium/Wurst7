/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.util.ChatUtil;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.LocalDateTime;

import net.minecraft.client.network.PlayerListEntry;

@SearchTags({ "spambot", "chatspam", "spammer" })
public final class SpambotHack extends Hack implements UpdateListener {
	private Random rand = new Random();
	private int timer;
	public static String message = "If you see this, then the K-Client Spambot is working! "
			+ "Make sure you check out the command help for .spam to customize the message.";

	private final SliderSetting spamDelay = new SliderSetting("Speed",
			"The message send rate of the spambot.\n" + "Lower = faster.", 6, 0, 60, 0.5, ValueDisplay.DECIMAL);

	public SpambotHack() {
		super("SpambotHack",
				"Spams the chat!\n\n" + "Make sure you're using .say if your message.\n" + "starts with a dot.");
		setCategory(Category.CHAT);
		addSetting(spamDelay);
	}

	@Override
	public void onEnable() {
		EVENTS.add(UpdateListener.class, this);
		timer = spamDelay.getValueI();
	}

	@Override
	public void onDisable() {
		// force close spambot window, stop spamming
		EVENTS.remove(UpdateListener.class, this);
	}

	@Override
	public void onUpdate() {
		if (timer > 0) {
			timer--;
			return;
		}

		sendMessage(evaluateMessage(message));
		timer = spamDelay.getValueI();
	}

	public void sendMessage(String message) {
		ChatMessageC2SPacket packet = new ChatMessageC2SPacket(message);
		MC.getNetworkHandler().sendPacket(packet);
	}

	public String evaluateMessage(String message) {
		// Turns all pseudocode into a real message
		// str.replaceAll("(First)[^&]*(Second)", "$1foo$2");
		String newMessage = message.replaceAll("%%", "%")
				.replaceAll("%fulldate%", getDate("MM/dd/yyyy HH:mm:ss"))
				.replaceAll("%date%", getDate("MM/dd/yyyy"))
				.replaceAll("%time%", getDate("HH:mm:ss"))
				.replaceAll("%rand%", "" + rand.nextInt(1000))
				.replaceAll("%user%", MC.getSession().getProfile().getName())
				.replaceAll("%ruser%", getRandomPlayer());
		return newMessage;
	}

	public String getDate(String format) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
		LocalDateTime now = LocalDateTime.now();
		return dtf.format(now);
	}
	
	private String getRandomPlayer() {
		final ArrayList<String> players = new ArrayList<>();
		for(PlayerListEntry info : MC.player.networkHandler.getPlayerList()) {
			String name = info.getProfile().getName();
			
			players.add(name);
		} return players.get(rand.nextInt(players.size()));
	}
}
