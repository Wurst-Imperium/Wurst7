/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.BlockListEditButton;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonArray;

public final class BlockListSetting extends Setting
{
	private final ArrayList<String> blockNames = new ArrayList<>();
	private final String[] defaultNames;
	
	public BlockListSetting(String name, String description, String... blocks)
	{
		super(name, description);
		
		Arrays.stream(blocks).parallel()
			.map(s -> Registry.BLOCK.get(new Identifier(s)))
			.filter(Objects::nonNull).map(BlockUtils::getName).distinct()
			.sorted().forEachOrdered(s -> blockNames.add(s));
		defaultNames = blockNames.toArray(new String[0]);
	}
	
	public List<String> getBlockNames()
	{
		return Collections.unmodifiableList(blockNames);
	}
	
	public void add(Block block)
	{
		String name = BlockUtils.getName(block);
		if(Collections.binarySearch(blockNames, name) >= 0)
			return;
		
		blockNames.add(name);
		Collections.sort(blockNames);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void remove(int index)
	{
		if(index < 0 || index >= blockNames.size())
			return;
		
		blockNames.remove(index);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void resetToDefaults()
	{
		blockNames.clear();
		blockNames.addAll(Arrays.asList(defaultNames));
		WurstClient.INSTANCE.saveSettings();
	}
	
	@Override
	public Component getComponent()
	{
		return new BlockListEditButton(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		try
		{
			WsonArray wson = JsonUtils.getAsArray(json);
			blockNames.clear();
			
			wson.getAllStrings().parallelStream()
				.map(s -> Registry.BLOCK.get(new Identifier(s)))
				.filter(Objects::nonNull).map(BlockUtils::getName).distinct()
				.sorted().forEachOrdered(s -> blockNames.add(s));
			
		}catch(JsonException e)
		{
			e.printStackTrace();
			resetToDefaults();
		}
	}
	
	@Override
	public JsonElement toJson()
	{
		JsonArray json = new JsonArray();
		blockNames.forEach(s -> json.add(s));
		return json;
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		return new LinkedHashSet<>();
	}
}
