package net.wurstclient.util;

import java.awt.Color;

public enum ColorUtils
{
    ;

    public static Color parse(String color) throws Exception
    {
        // TODO: alpha parsing (if needed)

        String[] rgb = color.split(",");

        if (rgb.length != 3)
            throw new Exception("Expected comma-separated RGB value.");

        String red = rgb[0];
        String green = rgb[1];
        String blue = rgb[2];
        if (!MathUtils.isInteger(red) || !MathUtils.isInteger(green) || !MathUtils.isInteger(blue))
            throw new Exception("RGB contains non-integer values.");


        int parsedRed = Integer.parseInt(red);
        int parsedGreen = Integer.parseInt(green);
        int parsedBlue = Integer.parseInt(blue);

        // Let it be so
        parsedRed = Math.min(Math.max(parsedRed, 0), 255);
        parsedGreen = Math.min(Math.max(parsedGreen, 0), 255);
        parsedBlue = Math.min(Math.max(parsedBlue, 0), 255);

        return new Color(parsedRed, parsedGreen, parsedBlue);
    }
}
