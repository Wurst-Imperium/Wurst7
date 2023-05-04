/*
 * TeaClient feature, 2023
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import com.google.gson.Gson;

import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class NameMCCmd extends Command {
	private static final String sparkletScraperURL =
			"https://sparklet.org/api/scrapegoat/namemc?username=";
	private Gson gson = new Gson();
	
	public NameMCCmd() {
		super("namemc", "Does a lookup on NameMC and prints past names to chat.", ".namemc <username>");
	}

	@Override
	public void call(String[] args) throws CmdException {
		if (args.length < 1) throw new CmdSyntaxError();

		final String target = String.join("", args);

		try {
			final SparkletNameMCResponse query = fetchSparklet(target);
			
			ChatUtils.message(
					"Opening profile in the browser..."
			);
			
			String link = query.nameHistory[0][1];
			Util.getOperatingSystem().open(link);
			
			/*ChatUtils.message("Old Usernames:");
			
			for (String[] pair : query.nameHistory) {
				ChatUtils.message(pair[1] + " | " + pair[0]);
			}*/
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new CmdError("Failed to fetch Sparklet for NameMC info");
		}
	}
	
	private SparkletNameMCResponse fetchSparklet(String name) throws Exception {
		return parseRes(fetch(sparkletScraperURL + name));
	}
	
	private SparkletNameMCResponse parseRes(String input) {
		return gson.fromJson(input, SparkletNameMCResponse.class);
	}

	private String fetch(String target) throws Exception {
		URL url = new URL(target);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}

		in.close();

		return response.toString();
	}
	

	private class SparkletNameMCResponse {
		public String[][] nameHistory;

		// potentially more data here later
		// we love feature creep! :D <3
	}
}
