/*
 * TeaClient Spambot (for Wurst7)
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.LocalDateTime;

import net.minecraft.client.network.PlayerListEntry;

@SearchTags({ "spambot", "chat spam", "chatspam", "spammer", "flooder"})
public final class SpambotHack extends Hack implements UpdateListener {
	private Random rand = new Random();
	private int timer;
	public static String message = "";

	private final SliderSetting spamDelay = new SliderSetting("Speed",
			"The message send rate of the spambot.\n" + "Lower = faster.", 6, 0, 60, 0.5, ValueDisplay.DECIMAL);

	public SpambotHack() {
		super("Spambot");
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
		if (message.equals("")) {
			ChatUtils.error("You don't have a spam message set!" +
							"Use .spam <message> to set one, then re-enable the hack.");
		} else if (message.startsWith(".")) {
			// Message is Wurst command
			WURST.getCmdProcessor().process(message.substring(1));
		} else {
			// Otherwise, send to chat
			MC.getNetworkHandler().sendChatMessage(message);
		}
	}

	public String evaluateMessage(String message) {
		// Turns all pseudocode into a real message
		// str.replaceAll("(First)[^&]*(Second)", "$1foo$2");
		String newMessage = message.replaceAll("%%", "%")
				.replaceAll("%fulldate%", getDate("MM/dd/yyyy HH:mm:ss"))
				.replaceAll("%date%", getDate("MM/dd/yyyy"))
				.replaceAll("%time%", getDate("HH:mm:ss"))
				.replaceAll("%rand%", "" + rand.nextInt(10000))
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
