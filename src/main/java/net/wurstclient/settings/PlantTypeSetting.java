/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.PlantTypeComponent;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;
import net.wurstclient.util.text.WText;

public final class PlantTypeSetting extends Setting
{
	private final ItemStack icon;
	private boolean harvest;
	private final boolean harvestByDefault;
	private boolean replant;
	private final boolean replantByDefault;
	
	public PlantTypeSetting(String name, WText description, Item icon,
		boolean harvest, boolean replant)
	{
		super(name, description);
		this.icon = new ItemStack(icon);
		this.harvest = harvest;
		harvestByDefault = harvest;
		this.replant = replant;
		replantByDefault = replant;
	}
	
	public PlantTypeSetting(String name, String descriptionKey, Item icon,
		boolean harvest, boolean replant)
	{
		this(name, WText.translated(descriptionKey), icon, harvest, replant);
	}
	
	public PlantTypeSetting(String name, Item icon, boolean harvest,
		boolean replant)
	{
		this(name, WText.empty(), icon, harvest, replant);
	}
	
	public ItemStack getIcon()
	{
		return icon;
	}
	
	public boolean isHarvestingEnabled()
	{
		return harvest;
	}
	
	public boolean isHarvestingEnabledByDefault()
	{
		return harvestByDefault;
	}
	
	public boolean isReplantingEnabled()
	{
		return replant;
	}
	
	public boolean isReplantingEnabledByDefault()
	{
		return replantByDefault;
	}
	
	public void setHarvestingEnabled(boolean harvest)
	{
		setHarvestingEnabledWithoutSaving(harvest);
		WurstClient.INSTANCE.saveSettings();
	}
	
	void setHarvestingEnabledWithoutSaving(boolean harvest)
	{
		this.harvest = harvest;
		update();
	}
	
	public void setReplantingEnabled(boolean replant)
	{
		setReplantingEnabledWithoutSaving(replant);
		WurstClient.INSTANCE.saveSettings();
	}
	
	void setReplantingEnabledWithoutSaving(boolean replant)
	{
		this.replant = replant;
		update();
	}
	
	public void toggleHarvestingEnabled()
	{
		setHarvestingEnabled(!isHarvestingEnabled());
	}
	
	public void toggleReplantingEnabled()
	{
		setReplantingEnabled(!isReplantingEnabled());
	}
	
	public void resetHarvestingEnabled()
	{
		setHarvestingEnabled(isHarvestingEnabledByDefault());
	}
	
	public void resetReplantingEnabled()
	{
		setReplantingEnabled(isReplantingEnabledByDefault());
	}
	
	@Override
	public Component getComponent()
	{
		return new PlantTypeComponent(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		if(JsonUtils.getAsString(json, "nope").equals("default"))
		{
			harvest = harvestByDefault;
			replant = replantByDefault;
			return;
		}
		
		try
		{
			WsonObject object = JsonUtils.getAsObject(json);
			harvest = object.getBoolean("harvest");
			replant = object.getBoolean("replant");
			
		}catch(JsonException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public JsonElement toJson()
	{
		if(harvest == harvestByDefault && replant == replantByDefault)
			return new JsonPrimitive("default");
		
		JsonObject json = new JsonObject();
		json.addProperty("harvest", harvest);
		json.addProperty("replant", replant);
		return json;
	}
	
	@Override
	public JsonObject exportWikiData()
	{
		JsonObject json = new JsonObject();
		json.addProperty("name", getName());
		json.addProperty("description", getDescription());
		json.addProperty("type", "PlantType");
		json.addProperty("harvestByDefault", harvestByDefault);
		json.addProperty("replantByDefault", replantByDefault);
		return json;
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		return new LinkedHashSet<>();
	}
}
