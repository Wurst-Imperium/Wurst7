/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.BookOffersEditButton;
import net.wurstclient.hacks.autolibrarian.BookOffer;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonArray;
import net.wurstclient.util.json.WsonObject;

public final class BookOffersSetting extends Setting
{
	private final ArrayList<BookOffer> offers = new ArrayList<>();
	private final BookOffer[] defaultOffers;
	
	public BookOffersSetting(String name, String description,
		String... enchantments)
	{
		super(name, description);
		
		Arrays.stream(enchantments)
			.map(s -> Registries.ENCHANTMENT.get(new Identifier(s)))
			.filter(Objects::nonNull).map(BookOffer::create)
			.filter(BookOffer::isValid).distinct().sorted()
			.forEach(offers::add);
		defaultOffers = offers.toArray(new BookOffer[0]);
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
		if(offer == null || !offer.isValid())
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
		if(offer == null || !offer.isValid())
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
			WsonArray wson = JsonUtils.getAsArray(json);
			offers.clear();
			
			wson.getAllObjects().parallelStream().map(this::loadOffer)
				.filter(Objects::nonNull).filter(BookOffer::isValid).distinct()
				.sorted().forEachOrdered(offers::add);
			
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
		json.addProperty("descriptionKey", getDescriptionKey());
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
