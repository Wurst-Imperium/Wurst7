/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autocomplete;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.wurstclient.settings.Setting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class SuggestionHandler
{
	private final ArrayList<String> suggestions = new ArrayList<>();
	
	private final SliderSetting maxSuggestionPerDraft = new SliderSetting(
		"Max suggestions per draft",
		"How many suggestions the AI is allowed to generate for the same draft"
			+ " message.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r Higher values can use up a lot of"
			+ " tokens. Definitely limit this to 1 for expensive models like"
			+ " GPT-4.",
		3, 1, 10, 1, ValueDisplay.INTEGER);
	
	private final SliderSetting maxSuggestionsKept = new SliderSetting(
		"Max suggestions kept", "Maximum number of suggestions kept in memory.",
		100, 10, 1000, 10, ValueDisplay.INTEGER);
	
	private final SliderSetting maxSuggestionsShown = new SliderSetting(
		"Max suggestions shown",
		"How many suggestions can be shown above the chat box.\n\n"
			+ "If this is set too high, the suggestions will obscure some of"
			+ " the existing chat messages. How high you can set this depends"
			+ " on your screen resolution and GUI scale.",
		5, 1, 10, 1, ValueDisplay.INTEGER);
	
	private final List<Setting> settings = Arrays.asList(maxSuggestionPerDraft,
		maxSuggestionsKept, maxSuggestionsShown);
	
	public List<Setting> getSettings()
	{
		return settings;
	}
	
	public boolean hasEnoughSuggestionFor(String draftMessage)
	{
		synchronized(suggestions)
		{
			return suggestions.stream().map(String::toLowerCase)
				.filter(s -> s.startsWith(draftMessage.toLowerCase()))
				.count() >= maxSuggestionPerDraft.getValue();
		}
	}
	
	public void addSuggestion(String suggestion, String draftMessage,
		BiConsumer<SuggestionsBuilder, String> suggestionsUpdater)
	{
		synchronized(suggestions)
		{
			String completedMessage = draftMessage + suggestion;
			
			if(!suggestions.contains(completedMessage))
			{
				suggestions.add(completedMessage);
				
				if(suggestions.size() > maxSuggestionsKept.getValue())
					suggestions.remove(0);
			}
			
			showSuggestionsImpl(draftMessage, suggestionsUpdater);
		}
	}
	
	public void showSuggestions(String draftMessage,
		BiConsumer<SuggestionsBuilder, String> suggestionsUpdater)
	{
		synchronized(suggestions)
		{
			showSuggestionsImpl(draftMessage, suggestionsUpdater);
		}
	}
	
	private void showSuggestionsImpl(String draftMessage,
		BiConsumer<SuggestionsBuilder, String> suggestionsUpdater)
	{
		SuggestionsBuilder builder = new SuggestionsBuilder(draftMessage, 0);
		String inlineSuggestion = null;
		
		int shownSuggestions = 0;
		for(int i = suggestions.size() - 1; i >= 0; i--)
		{
			String s = suggestions.get(i);
			if(!s.toLowerCase().startsWith(draftMessage.toLowerCase()))
				continue;
			
			if(shownSuggestions >= maxSuggestionsShown.getValue())
				break;
			
			builder.suggest(s);
			inlineSuggestion = s;
			shownSuggestions++;
		}
		
		suggestionsUpdater.accept(builder, inlineSuggestion);
	}
	
	public void clearSuggestions()
	{
		synchronized(suggestions)
		{
			suggestions.clear();
		}
	}
}
