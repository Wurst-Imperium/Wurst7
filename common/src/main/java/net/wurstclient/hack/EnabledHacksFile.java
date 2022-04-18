/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hack;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.wurstclient.WurstClient;
import net.wurstclient.util.profiles.ProfileFileReader;

public final class EnabledHacksFile extends ProfileFileReader
{
	public EnabledHacksFile(Path path) {
		super(path);
	}


	public Set<Hack> loadHacks() {
		Set<Hack> hackSet = new HashSet<>();

		for (Map.Entry<String, String> entry : load().getAllStrings().entrySet()) {
			String keyName = entry.getKey();
			boolean isEnabled = Boolean.parseBoolean(entry.getValue());

			if (isInvalidKeyName(keyName))
				continue;

			Hack hack = WurstClient.INSTANCE.getHax().getHackByName(keyName);
			hack.setEnabled(isEnabled);
			hackSet.add(hack);
		}
		return hackSet;
	}

}
