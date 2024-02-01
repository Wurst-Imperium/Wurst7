/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.function.BiConsumer;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.client.gui.screen.ChatScreen;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.autocomplete.ApiProviderSetting;
import net.wurstclient.hacks.autocomplete.MessageCompleter;
import net.wurstclient.hacks.autocomplete.ModelSettings;
import net.wurstclient.hacks.autocomplete.OobaboogaMessageCompleter;
import net.wurstclient.hacks.autocomplete.OpenAiMessageCompleter;
import net.wurstclient.hacks.autocomplete.SuggestionHandler;
import net.wurstclient.util.ChatUtils;

@SearchTags({"auto complete", "Copilot", "ChatGPT", "chat GPT", "GPT-3", "GPT3",
	"GPT 3", "OpenAI", "open ai", "ChatAI", "chat AI", "ChatBot", "chat bot"})
public final class AutoCompleteHack extends Hack
	implements ChatOutputListener, UpdateListener
{
	private final ModelSettings modelSettings = new ModelSettings();
	private final SuggestionHandler suggestionHandler = new SuggestionHandler();
	private final ApiProviderSetting apiProvider = new ApiProviderSetting();
	
	private MessageCompleter completer;
	private String draftMessage;
	private BiConsumer<SuggestionsBuilder, String> suggestionsUpdater;
	
	private Thread apiCallThread;
	private long lastApiCallTime;
	private long lastRefreshTime;
	
	public AutoCompleteHack()
	{
		super("AutoComplete");
		setCategory(Category.CHAT);
		
		addSetting(apiProvider);
		modelSettings.forEach(this::addSetting);
		suggestionHandler.getSettings().forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		completer = switch(apiProvider.getSelected())
		{
			case OPENAI -> new OpenAiMessageCompleter(modelSettings);
			case OOBABOOGA -> new OobaboogaMessageCompleter(modelSettings);
		};
		
		if(completer instanceof OpenAiMessageCompleter
			&& System.getenv("WURST_OPENAI_KEY") == null)
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
			String suggestion = completer.completeChatMessage(draftMessage2);
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
	
	// See ChatInputSuggestorMixin
}
