/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autocomplete;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.wurstclient.WurstClient;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.WsonObject;

public abstract class MessageCompleter
{
	protected static final MinecraftClient MC = WurstClient.MC;
	
	protected final ModelSettings modelSettings;
	
	public MessageCompleter(ModelSettings modelSettings)
	{
		this.modelSettings = modelSettings;
	}
	
	public final String completeChatMessage(String draftMessage)
	{
		// build prompt and parameters
		String prompt = buildPrompt(draftMessage);
		JsonObject params = buildParams(prompt);
		System.out.println(params);
		
		try
		{
			// send request
			WsonObject response = requestCompletion(params);
			System.out.println(response);
			
			// read response
			return extractCompletion(response);
			
		}catch(IOException | JsonException e)
		{
			e.printStackTrace();
			return "";
		}
	}
	
	protected String buildPrompt(String draftMessage)
	{
		// tell the model that it's talking in a Minecraft chat
		String prompt = "=== Minecraft chat log ===\n";
		
		// add chat history
		List<ChatHudLine.Visible> chatHistory =
			MC.inGameHud.getChatHud().visibleMessages;
		int messages = 0;
		for(int i = chatHistory.size() - 1; i >= 0; i--)
		{
			// get message
			String message = ChatUtils.getAsString(chatHistory.get(i));
			
			// filter out Wurst messages so the model won't admit it's hacking
			if(message.startsWith(ChatUtils.WURST_PREFIX))
				continue;
			
			// give non-player messages a sender to avoid confusing the model
			if(!message.startsWith("<"))
				if(modelSettings.filterServerMessages.isChecked())
					continue;
				else
					message = "<System> " + message;
				
			// limit context length to save tokens
			if(messages >= modelSettings.contextLength.getValueI())
				break;
			
			// add message to prompt
			prompt += message + "\n";
			messages++;
		}
		
		// if the chat history is empty, add a dummy system message
		if(chatHistory.isEmpty())
			prompt += "<System> " + MC.getSession().getUsername()
				+ " joined the game.\n";
		
		// add draft message
		prompt += "<" + MC.getSession().getUsername() + "> " + draftMessage;
		
		return prompt;
	}
	
	protected abstract JsonObject buildParams(String prompt);
	
	protected abstract WsonObject requestCompletion(JsonObject parameters)
		throws IOException, JsonException;
	
	protected abstract String extractCompletion(WsonObject response)
		throws JsonException;
}
