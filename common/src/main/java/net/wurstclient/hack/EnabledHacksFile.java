/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hack;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.wurstclient.util.profiles.ProfileFileReader;

public final class EnabledHacksFile extends ProfileFileReader
{
	public EnabledHacksFile(Path path) {
		super(path);
	}


	public TreeMap<String, Boolean> loadHacks() {
		TreeMap<String, Boolean> hackSet = new TreeMap<>();
		JsonObject fileContents = load().toJsonObject();
		for (Map.Entry<String, JsonElement> entry : fileContents.entrySet()) {
			String keyName = entry.getKey();
			hackSet.put(keyName, Boolean.parseBoolean(String.valueOf(entry.getValue())));
		}
		return hackSet;
	}

}
