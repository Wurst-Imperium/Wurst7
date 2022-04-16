
/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;

@SearchTags({ "spammer", "spam", "text variation", "repeat", "chat" })
public final class SpamCmd extends Command {
	public SpamCmd() {
		super("spam",
			"Spams the given chat message while adding randomized characters.\n"
						+ "<count> represents the times <message> is to be spammed.\n"
						+ "<ms_delay> represents the time between <message> sending in milliseconds.\n"
						+ "<replace_percent> represents the amount of characters replaced in <message>.\n"
						+ "<message> represents the message you want to send. <message> can have spaces in it.\n"
						+ "off Stops spamming\n",
				".spam <count> <delay> <replace> <message>\n"
						+ ".spam off"
		);
	}

	boolean stop_spamming;

	static Map<String, String> map = new HashMap<String, String>();
	static {
		// Mapping for Character Replacement
		// Upper Case Mapping

		map.put("A", "Ã");
		map.put("B", "Β");
		map.put("C", "Ç");
		map.put("D", "Ð");
		map.put("E", "Ë");
		map.put("F", "f");
		map.put("G", "Ĝ");
		map.put("H", "Ĥ");
		map.put("I", "Ì");
		map.put("J", "Ĵ");
		map.put("K", "Ķ");
		map.put("L", "Ĺ");
		map.put("M", "m");
		map.put("N", "Ń");
		map.put("O", "Ô");
		map.put("P", "p");
		map.put("Q", "q");
		map.put("R", "Ř");
		map.put("S", "Ś");
		map.put("T", "Ţ");
		map.put("U", "Ù");
		map.put("V", "Λ");
		map.put("W", "Ŵ");
		map.put("X", "×");
		map.put("Y", "Ý");
		map.put("Z", "Ž");

		// Lower Case Mapping

		map.put("a", "ä");
		map.put("b", "Β");
		map.put("c", "ç");
		map.put("d", "đ");
		map.put("e", "ê");
		map.put("f", "F");
		map.put("g", "ğ");
		map.put("h", "ĥ");
		map.put("i", "ï");
		map.put("j", "ĵ");
		map.put("k", "ķ");
		map.put("l", "ĺ");
		map.put("m", "M");
		map.put("n", "ń");
		map.put("o", "ö");
		map.put("p", "P");
		map.put("q", "Q");
		map.put("r", "ŕ");
		map.put("s", "Ś");
		map.put("t", "†");
		map.put("u", "ü");
		map.put("v", "ν");
		map.put("w", "ŵ");
		map.put("x", "X");
		map.put("y", "ỳ");
		map.put("z", "ž");
		
		// Number Mapping
		
		map.put("1", "¹");
		map.put("2", "²");
		map.put("3", "³");
		map.put("4", "⁴");
		map.put("5", "⁵");
		map.put("6", "⁶");
		map.put("7", "⁷");
		map.put("8", "⁸");
		map.put("9", "⁹");
		map.put("0", "⁰");
		
		// Symbol Mapping
		
		map.put("?", "¿");
		map.put("!", "¡");
		map.put("&", "⅋");
		map.put(".", "˙");
		
		// Going To Add More Later
	}

	@Override
	public void call(String[] args) throws CmdException, Exception {

		// Warnings

		if (args.length < 4) {
			if (args.length == 1 && args[0].toString().equals("off")){
				stop_spamming = true;
			}
			else {
				throw new CmdSyntaxError("Too few Arguments - Do .help spam for more info");
			}
		}
		else {
			String toChange1 = "";

			StringBuilder message = new StringBuilder();
			// Combine all arguments after i (3)

			for (int i = 3; i < args.length; i++) {
				message.append(" ").append(args[i]);
			}

			toChange1 = message.toString();

			if (!isInteger(args[0]))
				throw new CmdSyntaxError("First Argument is Count - Should be an Integer");

			if (!isInteger(args[1]))
				throw new CmdSyntaxError("Second Argument is Delay in Milliseconds - Should be an Integer");

			String chanceInputString1 = args[2];

			if (chanceInputString1.contains("%")) {
				chanceInputString1 = chanceInputString1.replace("%", "");
			}

			if (!isInteger(chanceInputString1))
				throw new CmdSyntaxError("Third Argument is Replace Percent - Should be an Integer");

			String chanceInputString = chanceInputString1;

			int chanceInput = Integer.parseInt(chanceInputString);
			if (chanceInput < 0 || chanceInput > 100){
				throw new CmdSyntaxError("Third Argument is Replace Percent - Between 0 and 100%");
			}
			double chance = ((double) chanceInput) / 100;

			int repeatLenght = Integer.parseInt(args[0]);
			int repeatDelay = Integer.parseInt(args[1]);

			final String toChange = toChange1;

			// Starts the program in a new tread
			stop_spamming = false;
			Thread running_thread = new Thread() {
				public void run() {
					System.out.println("Spam Thread Running");
					while (!stop_spamming) {
						for (int j = 0; j < repeatLenght; j++) {
							if (stop_spamming){
								break;
							}
							String temp = toChange;
							for (int i = 0; i < temp.length(); i++) {
								if ((Math.random() < chance)) {
									try {
										temp = temp.replace(temp.charAt(i), findMapping(temp.charAt(i)));
									} catch (Exception e) {
										// Do Nothing because above has a 100% probability of happening
										e.printStackTrace();
									}
								}
							}
							String message = String.join(" ", temp);
							ChatMessageC2SPacket packet = new ChatMessageC2SPacket(message);
							MC.getNetworkHandler().sendPacket(packet);
							try {
								TimeUnit.MILLISECONDS.sleep(repeatDelay);
							} catch (InterruptedException e) {
								// Do Nothing because above has a 100% probability of happening.
								e.printStackTrace();
							}
						}
						stop_spamming = true;
					}
				}
			};
			running_thread.start();
		}
	}

	// Function findMapping()

	private static char findMapping(char c) throws Exception {
		for (Map.Entry<String, String> entry : map.entrySet()) {

			// Checks if a mapping key matches with the character

			if (entry.getKey().equals(Character.toString(c))) {

				// Returns the value from the mapping for replacement

				return entry.getValue().charAt(0);
			}
		}

		// Returns the original character if there is no mapping for the character

		return c;
	}

	private static boolean isInteger(String s) {

		try {
			Integer.parseInt(s);

			// Detects if it is not an int

		} catch (NumberFormatException e) {
			return false;

			// Detects if it is an int

		} catch (NullPointerException e) {
			return false;
		}
		// Only got here if we didn't return false, backup failsafe.
		return true;
	}

}
