package net.wurstclient.hacks.autotrader;


import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.wurstclient.mixinterface.ILanguageManager;

import java.util.Objects;

public record ItemOffer(String id, int level, int price)
        implements Comparable<ItemOffer>
{
    public static ItemOffer create(Enchantment enchantment)
    {
        Identifier id = Registries.ENCHANTMENT.getId(enchantment);
        return new ItemOffer("" + id, enchantment.getMaxLevel(), 64);
    }

    public Enchantment getEnchantment()
    {
        return Registries.ENCHANTMENT.get(new Identifier(id));
    }

    public String getEnchantmentName()
    {
        TranslationStorage english = ILanguageManager.getEnglish();
        Enchantment enchantment = getEnchantment();
        return english.get(enchantment.getTranslationKey());
    }

    public String getEnchantmentNameWithLevel()
    {
        TranslationStorage english = ILanguageManager.getEnglish();
        Enchantment enchantment = getEnchantment();
        String name = english.get(enchantment.getTranslationKey());

        if(enchantment.getMaxLevel() > 1)
            name += " " + english.get("enchantment.level." + level);

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
    public int compareTo(TradeOffer other)
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

        TradeOffer other = (TradeOffer)obj;
        return id.equals(other.id) && level == other.level;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, level);
    }
}

