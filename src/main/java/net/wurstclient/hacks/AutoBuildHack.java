/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.FileSetting;

public final class AutoBuildHack extends Hack implements UpdateListener
{
	private final FileSetting template = new FileSetting("Template", "",
		"autobuild", folder -> createDefaultTemplates(folder));
	
	public AutoBuildHack()
	{
		super("AutoBuild", "Builds things automatically.");
		setCategory(Category.BLOCKS);
		addSetting(template);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		
	}
	
	private void createDefaultTemplates(Path folder)
	{
		try
		{
			Files.createFile(folder.resolve("test.txt"));
			
		}catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
