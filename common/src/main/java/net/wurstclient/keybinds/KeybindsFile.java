/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import net.wurstclient.util.profiles.ProfileFileReader;

public final class KeybindsFile extends ProfileFileReader{

    public KeybindsFile(Path path) {
        super(path);
    }

    public Set<Keybind> loadKeybinds() {
        Set<Keybind> newKeybinds = new HashSet<>();

        for (Entry<String, String> entry : load().getAllStrings().entrySet()) {
            String keyName = entry.getKey();
            String commands = entry.getValue();

            if (isInvalidKeyName(keyName))
                continue;

            Keybind keybind = new Keybind(keyName, commands);
            newKeybinds.add(keybind);
        }
        return newKeybinds;
    }

}
