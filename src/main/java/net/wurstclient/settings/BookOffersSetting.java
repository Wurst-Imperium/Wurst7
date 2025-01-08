/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.BookOffersEditButton;
import net.wurstclient.hacks.autolibrarian.BookOffer;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;
import net.wurstclient.util.text.WText;

public final class BookOffersSetting extends Setting
{
	private final ArrayList<BookOffer> offers = new ArrayList<>();
	private final BookOffer[] defaultOffers;
	
	public BookOffersSetting(String name, WText description,
		String... enchantments)
	{
		super(name, description);
		
		Arrays.stream(enchantments).filter(Objects::nonNull).map(s -> {
			String[] parts = s.split(";");
			return new BookOffer(parts[0], Integer.parseInt(parts[1]), 64);
		}).filter(BookOffer::isMostlyValid).distinct().sorted()
			.forEach(offers::add);
		defaultOffers = offers.toArray(new BookOffer[0]);
	}
	
	public BookOffersSetting(String name, String descriptionKey,
		String... enchantments)
	{
		this(name, WText.translated(descriptionKey), enchantments);
	}
	
	public List<BookOffer> getOffers()
	{
		return Collections.unmodifiableList(offers);
	}
	
	public int indexOf(BookOffer offer)
	{
		return Collections.binarySearch(offers, offer);
	}
	
	public boolean contains(BookOffer offer)
	{
		return indexOf(offer) >= 0;
	}
	
	public boolean isWanted(BookOffer offer)
	{
		// find a wanted offer with the same enchantment and level
		int index = indexOf(offer);
		if(index < 0)
			return false;
		
		// check if the price is low enough
		int maxPrice = offers.get(index).price();
		return offer.price() <= maxPrice;
	}
	
	public void add(BookOffer offer)
	{
		// check if offer is valid
		if(offer == null || !offer.isFullyValid())
			return;
		
		// check if an equal offer is already in the list
		if(contains(offer))
			return;
		
		offers.add(offer);
		Collections.sort(offers);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void remove(int index)
	{
		if(index < 0 || index >= offers.size())
			return;
		
		offers.remove(index);
		WurstClient.INSTANCE.saveSettings();
	}
	
	/**
	 * Replaces the offer at the given index with a new one. If the index is
	 * invalid, the new offer is invalid, or the new offer already exists
	 * elsewhere in the list, nothing will happen.
	 */
	public void replace(int index, BookOffer offer)
	{
		// check if index is valid
		if(index < 0 || index >= offers.size())
			return;
		
		// check if new offer is valid
		if(offer == null || !offer.isFullyValid())
			return;
		
		// check if new offer is different and already in the list
		if(!offer.equals(offers.get(index)) && contains(offer))
			return;
		
		// remove old offer
		offers.remove(index);
		
		// add new offer
		offers.add(offer);
		Collections.sort(offers);
		
		// save the changes
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void resetToDefaults()
	{
		offers.clear();
		offers.addAll(Arrays.asList(defaultOffers));
		WurstClient.INSTANCE.saveSettings();
	}
	
	@Override
	public Component getComponent()
	{
		return new BookOffersEditButton(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		try
		{
			offers.clear();
			
			// if string "default", load default offers
			if(JsonUtils.getAsString(json, "nope").equals("default"))
			{
				offers.addAll(Arrays.asList(defaultOffers));
				return;
			}
			
			// otherwise, load the offers in the JSON array
			JsonUtils.getAsArray(json).getAllObjects().parallelStream()
				.map(this::loadOffer).filter(Objects::nonNull)
				.filter(BookOffer::isMostlyValid).distinct().sorted()
				.forEachOrdered(offers::add);
			
		}catch(JsonException e)
		{
			System.out.println("Invalid book offer list: " + json);
			e.printStackTrace();
			resetToDefaults();
		}
	}
	
	private BookOffer loadOffer(WsonObject wson)
	{
		try
		{
			String id = wson.getString("id");
			int level = wson.getInt("level");
			int price = wson.getInt("max_price", 64);
			return new BookOffer(id, level, price);
			
		}catch(JsonException e)
		{
			System.out.println("Invalid book offer: " + wson);
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public JsonElement toJson()
	{
		// if offers is the same as defaultOffers, save string "default"
		if(offers.equals(Arrays.asList(defaultOffers)))
			return new JsonPrimitive("default");
		
		JsonArray json = new JsonArray();
		
		offers.forEach(offer -> {
			JsonObject jsonOffer = new JsonObject();
			
			jsonOffer.addProperty("id", offer.id());
			jsonOffer.addProperty("level", offer.level());
			if(offer.price() < 64)
				jsonOffer.addProperty("max_price", offer.price());
			
			json.add(jsonOffer);
		});
		
		return json;
	}
	
	@Override
	public JsonObject exportWikiData()
	{
		JsonObject json = new JsonObject();
		json.addProperty("name", getName());
		json.addProperty("description", getDescription());
		json.addProperty("type", "BookOffers");
		
		JsonArray jsonDefaultOffers = new JsonArray();
		for(BookOffer offer : defaultOffers)
		{
			JsonObject jsonOffer = new JsonObject();
			jsonOffer.addProperty("id", offer.id());
			jsonOffer.addProperty("level", offer.level());
			if(offer.price() < 64)
				jsonOffer.addProperty("max_price", offer.price());
			
			jsonDefaultOffers.add(jsonOffer);
		}
		json.add("defaultOffers", jsonDefaultOffers);
		
		return json;
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		return new LinkedHashSet<>();
	}
}
