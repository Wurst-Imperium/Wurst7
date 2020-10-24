/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.BlockListEditButton;
import net.wurstclient.clickgui.components.EntityListEditButton;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonArray;

import java.util.*;

public final class EntityListSetting extends Setting
{
	private final ArrayList<String> entityNames = new ArrayList<>();
	private final String[] defaultNames;
	
	public EntityListSetting(String name, String description, String... entities)
	{
		super(name, description);
		
		Arrays.stream(entities).parallel()
				.map(EntityUtils::getEntityTypeFromName)
				.filter(Objects::nonNull).map(EntityUtils::getEntityName).distinct()
				.sorted().forEachOrdered(entityNames::add);
		defaultNames = entityNames.toArray(new String[0]);
	}
	
	public List<String> getEntityNames()
	{
		return Collections.unmodifiableList(entityNames);
	}
	
	public void add(EntityType<?> entityType)
	{
		
		if (entityType == null)
			return;
		
		String name = EntityUtils.getEntityName(entityType);
		if(Collections.binarySearch(entityNames, name) >= 0)
			return;
		
		entityNames.add(name);
		Collections.sort(entityNames);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void remove(int index)
	{
		if(index < 0 || index >= entityNames.size())
			return;
		
		entityNames.remove(index);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public int size() {
		return this.entityNames.size();
	}
	
	public void resetToDefaults()
	{
		entityNames.clear();
		entityNames.addAll(Arrays.asList(defaultNames));
		WurstClient.INSTANCE.saveSettings();
	}
	
	@Override
	public Component getComponent()
	{
		return new EntityListEditButton(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		try
		{
			WsonArray wson = JsonUtils.getAsArray(json);
			entityNames.clear();
			
			wson.getAllStrings().parallelStream()
				.map(EntityUtils::getEntityTypeFromName)
				.filter(Objects::nonNull).map(EntityUtils::getEntityName).distinct()
				.sorted().forEachOrdered(entityNames::add);
			
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
		entityNames.forEach(json::add);
		return json;
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		return new LinkedHashSet<>();
	}
}
