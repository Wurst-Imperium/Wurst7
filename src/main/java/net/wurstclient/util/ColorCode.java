package net.wurstclient.util;

/**
 * Old class I wrote a long time ago. Takes either a RGB color set or a single colorcode, and maps it to a GL11-compatible RGB set between 0 and 1.
 */
public class ColorCode {
	public float r, g, b;

	// Given a set of RGB values, store it
	public ColorCode(float r, float g, float b)
	{
		this.r = r/255;
		this.g = g/255;
		this.b = b/255;
	}

	// Given a color code, return a ColorCode object with rgb values.
	public ColorCode(String colorstring)
	{
		//colorstring = String.valueOf(colorstring.charAt(1)); // Get second character only. It's the one with the data. Not needed because ListOfColorCodes returns w/o the section sign

		switch(colorstring) {
			// Stored between 0-255. Converted to scale 0-1 at bottom.

			// Black
			case "0":
				r = 0;
				g = 0;
				b = 0;
				break;

			// Dark blue
			case "1":
				r = 0;
				g = 0;
				b = 170;
				break;

			// Dark green
			case "2":
				r = 0;
				g = 170;
				b = 0;
				break;

			// Dark aqua
			case "3":
				r = 0;
				g = 170;
				b = 170;
				break;

			// Dark red
			case "4":
				r = 170;
				g = 0;
				b = 0;
				break;

			// Dark purple
			case "5":
				r = 170;
				g = 0;
				b = 170;
				break;

			// Gold
			case "6":
				r = 255;
				g = 170;
				b = 0;
				break;

			// Gray
			case "7":
				r = 170;
				g = 170;
				b = 170;
				break;

			// Dark gray
			case "8":
				r = 85;
				g = 85;
				b = 85;
				break;

			// Blue
			case "9":
				r = 85;
				g = 85;
				b = 255;
				break;

			// Green
			case "a":
				r = 85;
				g = 255;
				b = 85;
				break;

			// Aqua
			case "b":
				r = 85;
				g = 255;
				b = 255;
				break;

			// Red
			case "c":
				r = 255;
				g = 85;
				b = 85;
				break;

			// Light purple
			case "d":
				r = 255;
				g = 85;
				b = 255;
				break;

			// Yellow
			case "e":
				r = 255;
				g = 255;
				b = 85;
				break;

			// White
			case "f":
				r = 255;
				g = 255;
				b = 255;
				break;

		}

		// Translate to scale of 0-1.
		r /= 255;
		g /= 255;
		b /= 255;
	}
}
