/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.addon;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import net.wurstclient.WurstClient;

public final class AddonLoader
{
	private AddonLoader()
	{
		// Prevent instantiation
	}
	
	/**
	 * Loads all addons using Java's ServiceLoader. Call this during
	 * WurstClient initialization.
	 */
	public static void loadAddons()
	{
		Set<Class<?>> loadedAddonClasses = new HashSet<>();
		Set<Class<?>> loadedHackAddonClasses = new HashSet<>();
		Set<Class<?>> loadedCommandAddonClasses = new HashSet<>();
		
		ServiceLoader<Addon> addonLoader = ServiceLoader.load(Addon.class);
		for(Addon addon : addonLoader)
		{
			loadAddon(addon);
			loadedAddonClasses.add(addon.getClass());
			loadedHackAddonClasses.add(addon.getClass());
			loadedCommandAddonClasses.add(addon.getClass());
		}
		
		ServiceLoader<HackAddon> hackAddonLoader =
			ServiceLoader.load(HackAddon.class);
		for(HackAddon hackAddon : hackAddonLoader)
		{
			if(loadedHackAddonClasses.contains(hackAddon.getClass()))
				continue;
			
			loadHackAddon(hackAddon);
			loadedHackAddonClasses.add(hackAddon.getClass());
		}
		
		ServiceLoader<CommandAddon> commandAddonLoader =
			ServiceLoader.load(CommandAddon.class);
		for(CommandAddon commandAddon : commandAddonLoader)
		{
			if(loadedCommandAddonClasses.contains(commandAddon.getClass()))
				continue;
			
			loadCommandAddon(commandAddon);
			loadedCommandAddonClasses.add(commandAddon.getClass());
		}
		
		if(loadedAddonClasses.isEmpty() && loadedHackAddonClasses.isEmpty()
			&& loadedCommandAddonClasses.isEmpty())
		{
			System.out.println("[Wurst] No addons found via ServiceLoader.");
		}
	}
	
	private static void loadAddon(Addon addon)
	{
		try
		{
			WurstClient wurst = WurstClient.INSTANCE;
			
			wurst.getHax().registerHackAddon(addon);
			wurst.getCmds().registerCommandAddon(addon);
			
			System.out.println("[Wurst] Loaded addon: " + addon.getAddonName()
				+ " (" + addon.getHacks().length + " hacks, "
				+ addon.getCommands().length + " commands)");
			
		}catch(Exception e)
		{
			System.err.println("[Wurst] Failed to load addon "
				+ addon.getAddonName() + ": " + e);
			e.printStackTrace();
		}
	}
	
	private static void loadHackAddon(HackAddon addon)
	{
		try
		{
			WurstClient wurst = WurstClient.INSTANCE;
			wurst.getHax().registerHackAddon(addon);
			
			System.out
				.println("[Wurst] Loaded hack addon: " + addon.getAddonName()
					+ " (" + addon.getHacks().length + " hacks)");
			
		}catch(Exception e)
		{
			System.err.println("[Wurst] Failed to load hack addon "
				+ addon.getAddonName() + ": " + e);
			e.printStackTrace();
		}
	}
	
	private static void loadCommandAddon(CommandAddon addon)
	{
		try
		{
			WurstClient wurst = WurstClient.INSTANCE;
			wurst.getCmds().registerCommandAddon(addon);
			
			System.out
				.println("[Wurst] Loaded command addon: " + addon.getAddonName()
					+ " (" + addon.getCommands().length + " commands)");
			
		}catch(Exception e)
		{
			System.err.println("[Wurst] Failed to load command addon "
				+ addon.getAddonName() + ": " + e);
			e.printStackTrace();
		}
	}
}
