/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class KeybindList
{
	public static final Set<Keybind> DEFAULT_KEYBINDS = createDefaultKeybinds();
	
	private final KeybindsFile keybindsFile;
	private final ArrayList<Keybind> keybinds = new ArrayList<>();
	
	public KeybindList(Path keybindsFile)
	{
		this.keybindsFile = new KeybindsFile(keybindsFile);
		this.keybindsFile.load(this);
	}
	
	public String getCommands(String key)
	{
		for(Keybind keybind : keybinds)
		{
			if(!key.equals(keybind.getKey()))
				continue;
			
			return keybind.getCommands();
		}
		
		return null;
	}
	
	public List<Keybind> getAllKeybinds()
	{
		return Collections.unmodifiableList(keybinds);
	}
	
	public void add(String key, String commands)
	{
		keybinds.removeIf(keybind -> key.equals(keybind.getKey()));
		keybinds.add(new Keybind(key, commands));
		keybinds.sort(null);
		keybindsFile.save(this);
	}
	
	public void setKeybinds(Set<Keybind> keybinds)
	{
		this.keybinds.clear();
		this.keybinds.addAll(keybinds);
		this.keybinds.sort(null);
		keybindsFile.save(this);
	}
	
	public void remove(String key)
	{
		keybinds.removeIf(keybind -> key.equals(keybind.getKey()));
		keybindsFile.save(this);
	}
	
	public void removeAll()
	{
		keybinds.clear();
		keybindsFile.save(this);
	}
	
	private static Set<Keybind> createDefaultKeybinds()
	{
		Set<Keybind> set = new LinkedHashSet<>();
		addKB(set, "b", "fastplace;fastbreak");
		addKB(set, "b", "fastplace;fastbreak");
		addKB(set, "c", "fullbright");
		addKB(set, "g", "flight");
		addKB(set, "semicolon", "speednuker");
		addKB(set, "h", "say /home");
		addKB(set, "j", "jesus");
		addKB(set, "k", "multiaura");
		addKB(set, "n", "nuker");
		addKB(set, "r", "killaura");
		addKB(set, "right.shift", "navigator");
		addKB(set, "right.control", "clickgui");
		addKB(set, "u", "freecam");
		addKB(set, "x", "x-ray");
		addKB(set, "y", "sneak");
		return Collections.unmodifiableSet(set);
	}
	
	private static void addKB(Set<Keybind> set, String key, String cmds)
	{
		set.add(new Keybind("key.keyboard." + key, cmds));
	}
}
