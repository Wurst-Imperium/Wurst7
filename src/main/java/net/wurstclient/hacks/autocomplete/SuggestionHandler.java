/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autocomplete;

import java.util.ArrayList;
import java.util.function.BiConsumer;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;

public class SuggestionHandler
{
	private final ArrayList<String> suggestions = new ArrayList<>();
	
	public boolean hasSuggestionFor(String draftMessage)
	{
		synchronized(suggestions)
		{
			return suggestions.stream().anyMatch(
				s -> s.toLowerCase().startsWith(draftMessage.toLowerCase()));
		}
	}
	
	public void addSuggestion(String suggestion, String draftMessage,
		BiConsumer<SuggestionsBuilder, String> suggestionsUpdater)
	{
		synchronized(suggestions)
		{
			suggestions.add(draftMessage + suggestion);
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
		
		for(String s : suggestions)
			if(s.toLowerCase().startsWith(draftMessage.toLowerCase()))
			{
				builder.suggest(s);
				inlineSuggestion = s;
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
