/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.MessageListEditButton;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.text.WText;

public final class MessageListSetting extends Setting
{
	private final ArrayList<String> messages = new ArrayList<>();
	private final String[] defaultMessages;
	
	public MessageListSetting(String name, WText description,
		String... messages)
	{
		super(name, description);
		this.messages.addAll(Arrays.asList(messages));
		defaultMessages = this.messages.toArray(new String[0]);
	}
	
	public MessageListSetting(String name, String descriptionKey,
		String... messages)
	{
		this(name, WText.translated(descriptionKey), messages);
	}
	
	public MessageListSetting(String name, String... messages)
	{
		this(name, WText.empty(), messages);
	}
	
	public List<String> getMessages()
	{
		return Collections.unmodifiableList(messages);
	}
	
	public int size()
	{
		return messages.size();
	}
	
	public void add(String message)
	{
		if(message == null || message.isBlank())
			return;
		
		messages.add(message);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void addAt(int index, String message)
	{
		if(message == null || message.isBlank())
			return;
		
		int clampedIndex = Math.max(0, Math.min(index, messages.size()));
		messages.add(clampedIndex, message);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void remove(int index)
	{
		if(index < 0 || index >= messages.size())
			return;
		
		messages.remove(index);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void resetToDefaults()
	{
		messages.clear();
		messages.addAll(Arrays.asList(defaultMessages));
		WurstClient.INSTANCE.saveSettings();
	}
	
	@Override
	public Component getComponent()
	{
		return new MessageListEditButton(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		try
		{
			messages.clear();
			
			if(JsonUtils.getAsString(json, "nope").equals("default"))
			{
				messages.addAll(Arrays.asList(defaultMessages));
				return;
			}
			
			for(String msg : JsonUtils.getAsArray(json).getAllStrings())
				if(!msg.isBlank())
					messages.add(msg);
				
		}catch(JsonException e)
		{
			e.printStackTrace();
			resetToDefaults();
		}
	}
	
	@Override
	public JsonElement toJson()
	{
		if(messages.equals(Arrays.asList(defaultMessages)))
			return new JsonPrimitive("default");
		
		JsonArray json = new JsonArray();
		messages.forEach(s -> json.add(s));
		return json;
	}
	
	@Override
	public JsonObject exportWikiData()
	{
		JsonObject json = new JsonObject();
		json.addProperty("name", getName());
		json.addProperty("description", getDescription());
		json.addProperty("type", "MessageList");
		
		JsonArray defaultJson = new JsonArray();
		Arrays.stream(defaultMessages).forEachOrdered(s -> defaultJson.add(s));
		json.add("defaultMessages", defaultJson);
		
		return json;
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		return new LinkedHashSet<>();
	}
}
