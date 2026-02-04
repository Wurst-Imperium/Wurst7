/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class RoundingPrecisionSetting extends SliderSetting
{
	private static final DecimalFormatSymbols SYMBOLS =
		new DecimalFormatSymbols(Locale.ENGLISH);
	
	private final DecimalFormat[] FORMATS;
	
	public RoundingPrecisionSetting(String name, String description, int value,
		int min, int max)
	{
		super(name, description, value, min, max, 1,
			ValueDisplay.ROUNDING_PRECISION);
		
		if(min < 0)
			throw new IllegalArgumentException(
				"min must be greater than or equal to 0");
		
		FORMATS = new DecimalFormat[max + 1];
	}
	
	public DecimalFormat getFormat()
	{
		int value = getValueI();
		
		if(FORMATS[value] == null)
		{
			String pattern = "0";
			if(value > 0)
				pattern += "." + "#".repeat(value);
			
			FORMATS[value] = new DecimalFormat(pattern, SYMBOLS);
		}
		
		return FORMATS[value];
	}
	
	public String format(double value)
	{
		return getFormat().format(value);
	}
}
