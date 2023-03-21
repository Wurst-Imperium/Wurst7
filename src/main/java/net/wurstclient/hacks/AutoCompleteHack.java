/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

import com.google.gson.JsonObject;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.autocomplete.SuggestionHandler;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.OpenAiUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.WsonObject;

@SearchTags({"auto complete", "Copilot", "ChatGPT", "chat GPT", "GPT-3", "GPT3",
	"GPT 3", "OpenAI", "open ai", "ChatAI", "chat AI", "ChatBot", "chat bot"})
public final class AutoCompleteHack extends Hack
	implements ChatOutputListener, UpdateListener
{
	private final SuggestionHandler suggestionHandler = new SuggestionHandler();
	
	private String draftMessage;
	private BiConsumer<SuggestionsBuilder, String> suggestionsUpdater;
	
	private Thread apiCallThread;
	private long lastApiCallTime;
	private long lastRefreshTime;
	
	public AutoCompleteHack()
	{
		super("AutoComplete");
		setCategory(Category.CHAT);
		suggestionHandler.getSettings().forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		String apiKey = System.getenv("WURST_OPENAI_KEY");
		if(apiKey == null)
		{
			ChatUtils.error("API key not found. Please set the"
				+ " WURST_OPENAI_KEY environment variable and reboot.");
			setEnabled(false);
			return;
		}
		
		EVENTS.add(ChatOutputListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(ChatOutputListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
		
		suggestionHandler.clearSuggestions();
	}
	
	@Override
	public void onSentMessage(ChatOutputEvent event)
	{
		suggestionHandler.clearSuggestions();
	}
	
	@Override
	public void onUpdate()
	{
		// check if 300ms have passed since the last refresh
		long timeSinceLastRefresh =
			System.currentTimeMillis() - lastRefreshTime;
		if(timeSinceLastRefresh < 300)
			return;
		
		// check if 3s have passed since the last API call
		long timeSinceLastApiCall =
			System.currentTimeMillis() - lastApiCallTime;
		if(timeSinceLastApiCall < 3000)
			return;
		
		// check if the chat is open
		if(!(MC.currentScreen instanceof ChatScreen))
			return;
		
		// check if we have a draft message and suggestions updater
		if(draftMessage == null || suggestionsUpdater == null)
			return;
		
		// don't start a new thread if the old one is still running
		if(apiCallThread != null && apiCallThread.isAlive())
			return;
		
		// check if we already have a suggestion for the current draft message
		if(suggestionHandler.hasEnoughSuggestionFor(draftMessage))
			return;
			
		// copy fields to local variables, in case they change
		// while the thread is running
		String draftMessage2 = draftMessage;
		BiConsumer<SuggestionsBuilder, String> suggestionsUpdater2 =
			suggestionsUpdater;
		
		// build thread
		apiCallThread = new Thread(() -> {
			
			// get suggestion
			String suggestion = completeChatMessage(draftMessage2);
			if(suggestion.isEmpty())
				return;
			
			// apply suggestion
			suggestionHandler.addSuggestion(suggestion, draftMessage2,
				suggestionsUpdater2);
		});
		apiCallThread.setName("AutoComplete API Call");
		apiCallThread.setPriority(Thread.MIN_PRIORITY);
		apiCallThread.setDaemon(true);
		
		// start thread
		lastApiCallTime = System.currentTimeMillis();
		apiCallThread.start();
	}
	
	public void onRefresh(String draftMessage,
		BiConsumer<SuggestionsBuilder, String> suggestionsUpdater)
	{
		suggestionHandler.showSuggestions(draftMessage, suggestionsUpdater);
		
		this.draftMessage = draftMessage;
		this.suggestionsUpdater = suggestionsUpdater;
		lastRefreshTime = System.currentTimeMillis();
	}
	
	private String completeChatMessage(String draftMessage)
	{
		// get API key and parameters
		String apiKey = System.getenv("WURST_OPENAI_KEY");
		String prompt = buildPrompt(draftMessage);
		JsonObject params = buildParams(prompt);
		System.out.println(params);
		
		try
		{
			// send request
			WsonObject response = OpenAiUtils.requestCompletion(apiKey, params);
			System.out.println(response);
			
			// read response
			return extractCompletion(response);
			
		}catch(IOException | JsonException e)
		{
			e.printStackTrace();
			return "";
		}
	}
	
	private String buildPrompt(String draftMessage)
	{
		String prompt = "=== Minecraft chat log ===\n";
		
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
		
		prompt += "<" + MC.getSession().getUsername() + "> " + draftMessage;
		
		return prompt;
	}
	
	private JsonObject buildParams(String prompt)
	{
		JsonObject params = new JsonObject();
		params.addProperty("prompt", prompt);
		params.addProperty("stop", "\n<");
		params.addProperty("model", "code-davinci-002");
		params.addProperty("max_tokens", 16);
		params.addProperty("temperature", 0.7);
		params.addProperty("frequency_penalty", 0.6);
		return params;
	}
	
	private String extractCompletion(WsonObject response) throws JsonException
	{
		// extract completion from response
		String completion =
			response.getArray("choices").getObject(0).getString("text");
		
		// remove newlines
		completion = completion.replace("\n", " ");
		
		// remove leading and trailing whitespace
		completion = completion.strip();
		
		return completion;
	}
	
	// See ChatInputSuggestorMixin
}
