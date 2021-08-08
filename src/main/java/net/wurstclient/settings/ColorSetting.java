package net.wurstclient.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Set;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.ColorComponent;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.ColorUtils;
import net.wurstclient.util.json.JsonUtils;

public class ColorSetting extends Setting
{
    private Color color;
    private final Color colorByDefault;

    public ColorSetting(String name, String description, Color color)
    {
        super(name, description);
        this.color = color;
        this.colorByDefault = color;
    }

    public ColorSetting(String name, Color color)
    {
        this(name, "", color);
    }

    public final void setColor(Color color)
    {
        this.color = color;
    }

    public Color getColor()
    {
        return this.color;
    }

    public final Color getColorByDefault()
    {
        return this.colorByDefault;
    }

    @Override
    public final Component getComponent()
    {
        return new ColorComponent(this);
    }

    @Override
    public final void fromJson(JsonElement json)
    {
        if (!JsonUtils.isString(json))
            return;

        try {
            setColor(ColorUtils.parse(json.getAsString()));
        } catch (Exception ignored) {
        }
    }

    @Override
    public final JsonElement toJson()
    {
        return new JsonPrimitive(color.getRed() + "," + color.getGreen() + "," + color.getBlue());
    }

    @Override
    public final Set<PossibleKeybind> getPossibleKeybinds(String featureName)
    {
        String fullName = featureName + " " + getName();

        String command = ".setcolor " + featureName.toLowerCase() + " ";
        command = command + getName().toLowerCase().replace(" ", "_") + " ";

        LinkedHashSet<PossibleKeybind> pkb = new LinkedHashSet();
        pkb.add(new PossibleKeybind(command, "Set color of " + fullName));

        return pkb;
    }
}
