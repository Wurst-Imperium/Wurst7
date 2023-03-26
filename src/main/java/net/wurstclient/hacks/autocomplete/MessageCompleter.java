/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autocomplete;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.wurstclient.WurstClient;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
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
		for(int i = chatHistory.size() - 1; i >= 0; i--)
		{
			// get message
			String message = ChatUtils.getAsString(chatHistory.get(i));
			
			// filter out Wurst messages so the model won't admit it's hacking
			if(message.startsWith(ChatUtils.WURST_PREFIX))
				continue;
			
			// give non-player messages a sender to avoid confusing the model
			if(!message.startsWith("<"))
				message = "<System> " + message;
			
			prompt += message + "\n";
		}
		
		// if the chat history is empty, add a dummy system message
		if(chatHistory.isEmpty())
			prompt += "<System> " + MC.getSession().getUsername()
				+ " joined the game.\n";
		
		// add draft message
		prompt += "<" + MC.getSession().getUsername() + "> " + draftMessage;
		
		return prompt;
	}
	
	protected JsonObject buildParams(String prompt)
	{
		JsonObject params = new JsonObject();
		params.addProperty("prompt", prompt);
		params.addProperty("stop", "\n<");
		params.addProperty("model", "code-davinci-002");
		params.addProperty("max_tokens", modelSettings.maxTokens.getValueI());
		params.addProperty("temperature", modelSettings.temperature.getValue());
		params.addProperty("top_p", modelSettings.topP.getValue());
		params.addProperty("presence_penalty",
			modelSettings.presencePenalty.getValue());
		params.addProperty("frequency_penalty",
			modelSettings.frequencyPenalty.getValue());
		return params;
	}
	
	protected WsonObject requestCompletion(JsonObject parameters)
		throws IOException, JsonException
	{
		// set up the API request
		URL url = new URL("https://api.openai.com/v1/completions");
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization",
			"Bearer " + System.getenv("WURST_OPENAI_KEY"));
		
		// set the request body
		conn.setDoOutput(true);
		try(OutputStream os = conn.getOutputStream())
		{
			os.write(JsonUtils.GSON.toJson(parameters).getBytes());
			os.flush();
		}
		
		// parse the response
		return JsonUtils.parseConnectionToObject(conn);
	}
	
	protected String extractCompletion(WsonObject response) throws JsonException
	{
		// extract completion from response
		String completion =
			response.getArray("choices").getObject(0).getString("text");
		
		// remove newlines
		completion = completion.replace("\n", " ");
		
		return completion;
	}
}
