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

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.ItemListEditButton;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonArray;

public final class ItemListSetting extends Setting
{
	private final ArrayList<String> itemNames = new ArrayList<>();
	private final String[] defaultNames;
	
	public ItemListSetting(String name, String description, String... items)
	{
		super(name, description);
		
		Arrays.stream(items).parallel()
			.map(s -> Registry.ITEM.get(new Identifier(s)))
			.filter(Objects::nonNull)
			.map(i -> Registry.ITEM.getId(i).toString()).distinct().sorted()
			.forEachOrdered(s -> itemNames.add(s));
		defaultNames = itemNames.toArray(new String[0]);
	}
	
	public List<String> getItemNames()
	{
		return Collections.unmodifiableList(itemNames);
	}
	
	public void add(Item item)
	{
		String name = Registry.ITEM.getId(item).toString();
		if(Collections.binarySearch(itemNames, name) >= 0)
			return;
		
		itemNames.add(name);
		Collections.sort(itemNames);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void remove(int index)
	{
		if(index < 0 || index >= itemNames.size())
			return;
		
		itemNames.remove(index);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void resetToDefaults()
	{
		itemNames.clear();
		itemNames.addAll(Arrays.asList(defaultNames));
		WurstClient.INSTANCE.saveSettings();
	}
	
	@Override
	public Component getComponent()
	{
		return new ItemListEditButton(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		try
		{
			WsonArray wson = JsonUtils.getAsArray(json);
			itemNames.clear();
			
			wson.getAllStrings().parallelStream()
				.map(s -> Registry.ITEM.get(new Identifier(s)))
				.filter(Objects::nonNull)
				.map(i -> Registry.ITEM.getId(i).toString()).distinct().sorted()
				.forEachOrdered(s -> itemNames.add(s));
			
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
		itemNames.forEach(s -> json.add(s));
		return json;
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		return new LinkedHashSet<>();
	}
}
