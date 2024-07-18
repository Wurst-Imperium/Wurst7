/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autolibrarian;

import net.wurstclient.settings.BookOffersSetting;
import net.wurstclient.settings.EnumSetting;

public final class UpdateBooksSetting
	extends EnumSetting<UpdateBooksSetting.UpdateBooks>
{
	public UpdateBooksSetting()
	{
		super("Update books",
			"Automatically updates the list of wanted books when a villager"
				+ " has learned to sell one of them.\n\n"
				+ "\u00a7lOff\u00a7r - Don't update the list.\n\n"
				+ "\u00a7lRemove\u00a7r - Remove the book from the list so"
				+ " that the next villager will learn a different book.\n\n"
				+ "\u00a7lPrice\u00a7r - Update the maximum price for the book"
				+ " so that the next villager has to sell it for a cheaper"
				+ " price.",
			UpdateBooks.values(), UpdateBooks.REMOVE);
	}
	
	public enum UpdateBooks
	{
		OFF("Off"),
		REMOVE("Remove"),
		PRICE("Price");
		
		private String name;
		
		private UpdateBooks(String name)
		{
			this.name = name;
		}
		
		public void update(BookOffersSetting wantedBooks, BookOffer offer)
		{
			int index = wantedBooks.indexOf(offer);
			
			switch(this)
			{
				case OFF:
				return;
				
				case REMOVE:
				wantedBooks.remove(index);
				break;
				
				case PRICE:
				if(offer.price() <= 1)
					wantedBooks.remove(index);
				else
					wantedBooks.replace(index, new BookOffer(offer.id(),
						offer.level(), offer.price() - 1));
				break;
			}
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
