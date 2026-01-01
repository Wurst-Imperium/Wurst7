/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autolibrarian;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.wurstclient.WurstClient;
import net.wurstclient.WurstTranslator;

public record BookOffer(String id, int level, int price)
	implements Comparable<BookOffer>
{
	public static BookOffer create(Enchantment enchantment)
	{
		RegistryAccess drm = WurstClient.MC.level.registryAccess();
		Registry<Enchantment> registry =
			drm.lookupOrThrow(Registries.ENCHANTMENT);
		Identifier id = registry.getKey(enchantment);
		return new BookOffer("" + id, enchantment.getMaxLevel(), 64);
	}
	
	public Optional<? extends Holder<Enchantment>> getEnchantmentEntry()
	{
		if(WurstClient.MC.level == null)
			return Optional.empty();
		
		RegistryAccess drm = WurstClient.MC.level.registryAccess();
		Registry<Enchantment> registry =
			drm.lookupOrThrow(Registries.ENCHANTMENT);
		return registry.get(Identifier.parse(id));
	}
	
	public Enchantment getEnchantment()
	{
		return getEnchantmentEntry().map(Holder::value).orElse(null);
	}
	
	public String getEnchantmentName()
	{
		Component description = getEnchantment().description();
		if(description.getContents() instanceof TranslatableContents tr)
			return WurstClient.INSTANCE.getTranslator()
				.translateMcEnglish(tr.getKey());
		
		return description.getString();
	}
	
	public String getEnchantmentNameWithLevel()
	{
		WurstTranslator translator = WurstClient.INSTANCE.getTranslator();
		Enchantment enchantment = getEnchantment();
		String name;
		
		if(enchantment.description()
			.getContents() instanceof TranslatableContents tr)
			name = translator.translateMcEnglish(tr.getKey());
		else
			name = enchantment.description().getString();
		
		if(enchantment.getMaxLevel() > 1)
			name += " "
				+ translator.translateMcEnglish("enchantment.level." + level);
		
		return name;
	}
	
	public String getFormattedPrice()
	{
		return price + " emerald" + (price == 1 ? "" : "s");
	}
	
	/**
	 * Fully validates the book offer using the dynamic enchantment registry.
	 * Will return false if called while the user is not in a world or server.
	 */
	public boolean isFullyValid()
	{
		return isMostlyValid() && getEnchantmentEntry()
			.map(entry -> entry.is(EnchantmentTags.TRADEABLE)
				&& level <= entry.value().getMaxLevel())
			.orElse(false);
	}
	
	/**
	 * Tries to validate the book offer without using dynamic registries, which
	 * aren't loaded until the user enters a world or server.
	 */
	public boolean isMostlyValid()
	{
		return Identifier.tryParse(id) != null && level >= 1 && price >= 1
			&& price <= 64;
	}
	
	@Override
	public int compareTo(BookOffer other)
	{
		int idCompare = id.compareTo(other.id);
		if(idCompare != 0)
			return idCompare;
		
		return Integer.compare(level, other.level);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		
		if(obj == null || getClass() != obj.getClass())
			return false;
		
		BookOffer other = (BookOffer)obj;
		return id.equals(other.id) && level == other.level;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(id, level);
	}
}
