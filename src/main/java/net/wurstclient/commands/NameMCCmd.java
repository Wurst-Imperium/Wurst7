/*
 * TeaClient feature, 2023
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class NameMCCmd extends Command {
	private static final String mojangApiUrl =
			"https://api.mojang.com/users/profiles/minecraft/";
	private static final String namemcApiUrl =
			"https://api.mojang.com/users/profiles/minecraft/";
	private Gson gson = new Gson();
	
	public NameMCCmd() {
		super("namemc", "Does a lookup on NameMC and prints past names to chat.", ".namemc <username>");
	}

	@Override
	public void call(String[] args) throws CmdException {
		if (args.length < 1) throw new CmdSyntaxError();

		final String target = String.join("", args);
		final String uuid = playerNameToUUID(target);
		final ArrayList<String> names; // ...and this is where I realized there is no NameMC API.
		// at least, not for getting name history.
	}

	private String playerNameToUUID(String name) throws CmdException {
		try {
			return fetchJson(mojangApiUrl + name).get("id").getAsString();
		} catch (Exception e) {
			throw new CmdError("Could not query Mojang API...");
		}
	}
	
	private JsonObject fetchJson(String target) throws Exception {
		return parseJson(fetch(target));
	}
	
	private JsonObject parseJson(String input) {
		return gson.fromJson(input, JsonObject.class);
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
}
