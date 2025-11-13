/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.BlockComponent;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.text.WText;

public final class BlockSetting extends Setting
{
	private String blockName = "";
	private final String defaultName;
	private final boolean allowAir;
	
	public BlockSetting(String name, WText description, String blockName,
		boolean allowAir)
	{
		super(name, description);
		
		Block block = BlockUtils.getBlockFromNameOrID(blockName);
		Objects.requireNonNull(block);
		this.blockName = BlockUtils.getName(block);
		
		defaultName = this.blockName;
		this.allowAir = allowAir;
	}
	
	public BlockSetting(String name, String descriptionKey, String blockName,
		boolean allowAir)
	{
		this(name, WText.translated(descriptionKey), blockName, allowAir);
	}
	
	public BlockSetting(String name, String blockName, boolean allowAir)
	{
		this(name, WText.empty(), blockName, allowAir);
	}
	
	/**
	 * @return this setting's {@link Block}. Cannot be null.
	 */
	public Block getBlock()
	{
		return BlockUtils.getBlockFromName(blockName);
	}
	
	public String getBlockName()
	{
		return blockName;
	}
	
	public String getShortBlockName()
	{
		return blockName.replace("minecraft:", "");
	}
	
	public void setBlock(Block block)
	{
		if(block == null)
			return;
		
		if(!allowAir && block instanceof AirBlock)
			return;
		
		String newName = Objects.requireNonNull(BlockUtils.getName(block));
		
		if(blockName.equals(newName))
			return;
		
		blockName = newName;
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void setBlockName(String blockName)
	{
		Block block = BlockUtils.getBlockFromNameOrID(blockName);
		Objects.requireNonNull(block);
		
		setBlock(block);
	}
	
	public void resetToDefault()
	{
		blockName = defaultName;
		WurstClient.INSTANCE.saveSettings();
	}
	
	@Override
	public Component getComponent()
	{
		return new BlockComponent(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		try
		{
			String rawName = JsonUtils.getAsString(json);
			
			Identifier id = Identifier.tryParse(rawName);
			if(id == null)
				throw new JsonException("Discarding Block \"" + rawName
					+ "\" as it is not a valid identifier");
			
			String name = id.toString();
			if(!allowAir && "minecraft:air".equals(name))
				throw new JsonException("Discarding Block \"" + rawName
					+ "\" as this setting does not allow air blocks");
			
			blockName = name;
			
		}catch(JsonException e)
		{
			e.printStackTrace();
			resetToDefault();
		}
	}
	
	@Override
	public JsonElement toJson()
	{
		return new JsonPrimitive(blockName);
	}
	
	@Override
	public JsonObject exportWikiData()
	{
		JsonObject json = new JsonObject();
		json.addProperty("name", getName());
		json.addProperty("description", getDescription());
		json.addProperty("type", "Block");
		json.addProperty("defaultValue", defaultName);
		json.addProperty("allowAir", allowAir);
		return json;
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		String fullName = featureName + " " + getName();
		
		String command = ".setblock " + featureName.toLowerCase() + " ";
		command += getName().toLowerCase().replace(" ", "_") + " ";
		
		LinkedHashSet<PossibleKeybind> pkb = new LinkedHashSet<>();
		// Can't just list all the blocks here. Would need to change UI to allow
		// user to choose a block after selecting this option.
		// pkb.add(new PossibleKeybind(command + "dirt", "Set " + fullName + "
		// to dirt"));
		pkb.add(new PossibleKeybind(command + "reset", "Reset " + fullName));
		
		return pkb;
	}
}
