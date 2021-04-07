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
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Random;
import java.time.LocalDateTime;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

@SearchTags({ "spambot", "chatspam", "spammer" })
public final class SpambotHack extends Hack implements UpdateListener {
	private Random rand = new Random();
	private int timer;
	public static String message = "If you see this, then the K-Client Spambot is working!"
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
		String newMessage = message.replaceAll("%fulldate%", getDate("MM/dd/yyyy HH:mm:ss"))
				.replaceAll("%date%", getDate("MM/dd/yyyy"))
				.replaceAll("%time%", getDate("HH:mm:ss"))
				.replaceAll("%rand%", "" + rand.nextInt(1000))
				.replaceAll("%user%", MC.player.getEntityName())
				.replaceAll("%ruser%", ((PlayerEntity) getRandomFrom(MC.player.networkHandler.getPlayerList())).getEntityName());
		return newMessage;
	}

	public String getDate(String format) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
		LocalDateTime now = LocalDateTime.now();
		return dtf.format(now);
	}

	private Object getRandomFrom(Collection<PlayerListEntry> from) {
		return from.toArray()[rand.nextInt(from.size())];
	}
}
