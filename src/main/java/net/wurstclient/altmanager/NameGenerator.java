/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.util.Random;

public final class NameGenerator
{
	private static final Random random = new Random();
	
	public static String generateName()
	{
		String name = "";
		int nameLength = (int)Math.round(Math.random() * 4) + 5;
		String vowels = "aeiouy";
		String consonants = "bcdfghklmnprstvwz";
		int usedConsonants = 0;
		int usedVowels = 0;
		String lastLetter = "blah";
		
		for(int i = 0; i < nameLength; i++)
		{
			String nextLetter = lastLetter;
			if((random.nextBoolean() || usedConsonants == 1) && usedVowels < 2)
			{
				while(nextLetter.equals(lastLetter))
				{
					int letterIndex =
						(int)(Math.random() * vowels.length() - 1);
					nextLetter = vowels.substring(letterIndex, letterIndex + 1);
				}
				usedConsonants = 0;
				usedVowels++;
			}else
			{
				while(nextLetter.equals(lastLetter))
				{
					int letterIndex =
						(int)(Math.random() * consonants.length() - 1);
					nextLetter =
						consonants.substring(letterIndex, letterIndex + 1);
				}
				usedConsonants++;
				usedVowels = 0;
			}
			lastLetter = nextLetter;
			name = name.concat(nextLetter);
		}
		
		int capitalMode = (int)Math.round(Math.random() * 2);
		if(capitalMode == 1)
			name = name.substring(0, 1).toUpperCase() + name.substring(1);
		else if(capitalMode == 2)
			for(int i = 0; i < nameLength; i++)
				if((int)Math.round(Math.random() * 3) == 1)
					name = name.substring(0, i)
						+ name.substring(i, i + 1).toUpperCase()
						+ (i == nameLength ? "" : name.substring(i + 1));
		int numberLength = (int)Math.round(Math.random() * 3) + 1;
		int numberMode = (int)Math.round(Math.random() * 3);
		
		boolean number = random.nextBoolean();
		if(number)
			if(numberLength == 1)
			{
				int nextNumber = (int)Math.round(Math.random() * 9);
				name = name.concat(Integer.toString(nextNumber));
			}else if(numberMode == 0)
			{
				int nextNumber = (int)(Math.round(Math.random() * 8) + 1);
				for(int i = 0; i < numberLength; i++)
					name = name.concat(Integer.toString(nextNumber));
			}else if(numberMode == 1)
			{
				int nextNumber = (int)(Math.round(Math.random() * 8) + 1);
				name = name.concat(Integer.toString(nextNumber));
				for(int i = 1; i < numberLength; i++)
					name = name.concat("0");
			}else if(numberMode == 2)
			{
				int nextNumber = (int)(Math.round(Math.random() * 8) + 1);
				name = name.concat(Integer.toString(nextNumber));
				for(int i = 0; i < numberLength; i++)
				{
					nextNumber = (int)Math.round(Math.random() * 9);
					name = name.concat(Integer.toString(nextNumber));
				}
			}else if(numberMode == 3)
			{
				int nextNumber = 99999;
				while(Integer.toString(nextNumber).length() != numberLength)
				{
					nextNumber = (int)(Math.round(Math.random() * 12) + 1);
					nextNumber = (int)Math.pow(2, nextNumber);
				}
				name = name.concat(Integer.toString(nextNumber));
			}
		
		boolean leet = !number && random.nextBoolean();
		if(leet)
		{
			String oldName = name;
			while(name.equals(oldName))
			{
				int leetMode = (int)Math.round(Math.random() * 7);
				if(leetMode == 0)
				{
					name = name.replace("a", "4");
					name = name.replace("A", "4");
				}
				if(leetMode == 1)
				{
					name = name.replace("e", "3");
					name = name.replace("E", "3");
				}
				if(leetMode == 2)
				{
					name = name.replace("g", "6");
					name = name.replace("G", "6");
				}
				if(leetMode == 3)
				{
					name = name.replace("h", "4");
					name = name.replace("H", "4");
				}
				if(leetMode == 4)
				{
					name = name.replace("i", "1");
					name = name.replace("I", "1");
				}
				if(leetMode == 5)
				{
					name = name.replace("o", "0");
					name = name.replace("O", "0");
				}
				if(leetMode == 6)
				{
					name = name.replace("s", "5");
					name = name.replace("S", "5");
				}
				if(leetMode == 7)
				{
					name = name.replace("l", "7");
					name = name.replace("L", "7");
				}
			}
		}
		
		int special = (int)Math.round(Math.random() * 8);
		if(special == 3)
			name = "xX".concat(name).concat("Xx");
		else if(special == 4)
			name = name.concat("LP");
		else if(special == 5)
			name = name.concat("HD");
		
		return name;
	}
}
