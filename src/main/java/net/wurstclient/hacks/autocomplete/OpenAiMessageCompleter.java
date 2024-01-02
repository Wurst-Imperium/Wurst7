/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

public final class OpenAiMessageCompleter extends MessageCompleter
{
	public OpenAiMessageCompleter(ModelSettings modelSettings)
	{
		super(modelSettings);
	}
	
	@Override
	protected JsonObject buildParams(String prompt)
	{
		// build the request parameters
		JsonObject params = new JsonObject();
		params.addProperty("stop",
			modelSettings.stopSequence.getSelected().getSequence());
		params.addProperty("model",
			"" + modelSettings.openAiModel.getSelected());
		params.addProperty("max_tokens", modelSettings.maxTokens.getValueI());
		params.addProperty("temperature", modelSettings.temperature.getValue());
		params.addProperty("top_p", modelSettings.topP.getValue());
		params.addProperty("presence_penalty",
			modelSettings.presencePenalty.getValue());
		params.addProperty("frequency_penalty",
			modelSettings.frequencyPenalty.getValue());
		
		// add the prompt, depending on the model
		if(modelSettings.openAiModel.getSelected().isChatModel())
		{
			JsonArray messages = new JsonArray();
			JsonObject promptMessage = new JsonObject();
			promptMessage.addProperty("role", "user");
			promptMessage.addProperty("content", prompt);
			messages.add(promptMessage);
			params.add("messages", messages);
			
		}else
			params.addProperty("prompt", prompt);
		
		return params;
	}
	
	@Override
	protected WsonObject requestCompletion(JsonObject parameters)
		throws IOException, JsonException
	{
		// get the API URL
		URL url = modelSettings.openAiModel.getSelected().isChatModel()
			? new URL(modelSettings.openaiChatEndpoint.getValue())
			: new URL(modelSettings.openaiLegacyEndpoint.getValue());
		
		// set up the API request
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
	
	@Override
	protected String extractCompletion(WsonObject response) throws JsonException
	{
		// extract completion from response
		String completion;
		if(modelSettings.openAiModel.getSelected().isChatModel())
			completion = response.getArray("choices").getObject(0)
				.getObject("message").getString("content");
		else
			completion =
				response.getArray("choices").getObject(0).getString("text");
		
		// remove newlines
		completion = completion.replace("\n", " ");
		
		return completion;
	}
}
