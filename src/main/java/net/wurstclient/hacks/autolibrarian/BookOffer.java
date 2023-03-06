/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autolibrarian;

import java.util.Objects;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.ILanguageManager;

public record BookOffer(String id, int level, int price)
	implements Comparable<BookOffer>
{
	public static BookOffer create(Enchantment enchantment)
	{
		Identifier id = Registries.ENCHANTMENT.getId(enchantment);
		return new BookOffer("" + id, enchantment.getMaxLevel(), 64);
	}
	
	public Enchantment getEnchantment()
	{
		return Registries.ENCHANTMENT.get(new Identifier(id));
	}
	
	public String getEnchantmentName()
	{
		ILanguageManager languageManager = WurstClient.IMC.getLanguageManager();
		Enchantment enchantment = getEnchantment();
		String trKey = enchantment.getTranslationKey();
		String name = languageManager.getEnglish().get(trKey);
		
		return name;
	}
	
	public String getEnchantmentNameWithLevel()
	{
		ILanguageManager languageManager = WurstClient.IMC.getLanguageManager();
		Enchantment enchantment = getEnchantment();
		String trKey = enchantment.getTranslationKey();
		String name = languageManager.getEnglish().get(trKey);
		
		if(enchantment.getMaxLevel() > 1)
			name += " " + languageManager.getEnglish()
				.get("enchantment.level." + level);
		
		return name;
	}
	
	public String getFormattedPrice()
	{
		return price + " emerald" + (price == 1 ? "" : "s");
	}
	
	public boolean isValid()
	{
		Enchantment enchantment = getEnchantment();
		return enchantment != null
			&& enchantment.isAvailableForEnchantedBookOffer() && level >= 1
			&& level <= enchantment.getMaxLevel() && price >= 1 && price <= 64;
	}
	
	public Integer[] possiblePriceRange() {
		// see net.minecraft.village.TradeOffers
		int min = 2 + 3 * level;
		int max = Math.min(64, 2 + (4 + level * 10) + 3 * level);
		if (getEnchantment().isTreasure()) {
			min = Math.min(64, 2 * min);
			max = Math.min(64, 2 * max);
		}
		return new Integer[]{min, max};
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
