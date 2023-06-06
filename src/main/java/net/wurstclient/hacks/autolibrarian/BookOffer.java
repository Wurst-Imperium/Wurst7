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
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.ILanguageManager;

public final class BookOffer implements Comparable<BookOffer>
{
	private final String id;
	private final int level;
	private final int price;
	
	public BookOffer(String id, int level, int price)
	{
		this.id = id;
		this.level = level;
		this.price = price;
	}
	
	public static BookOffer create(Enchantment enchantment)
	{
		Identifier id = Registry.ENCHANTMENT.getId(enchantment);
		return new BookOffer("" + id, enchantment.getMaxLevel(), 64);
	}
	
	public Enchantment getEnchantment()
	{
		return Registry.ENCHANTMENT.get(new Identifier(id));
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
	
	public String id()
	{
		return id;
	}
	
	public int level()
	{
		return level;
	}
	
	public int price()
	{
		return price;
	}
}
