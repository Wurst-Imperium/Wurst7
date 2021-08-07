package net.wurstclient.commands;

import java.util.stream.Stream;
import net.wurstclient.DontBlock;
import net.wurstclient.Feature;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.ColorUtils;

@DontBlock
public final class SetColorCmd extends Command
{
    public SetColorCmd()
    {
        super("setcolor",
                "Changes a color setting of a feature. Allows you\nto set RGB values through keybinds.",
                ".setcolor <feature> <setting> <RGB color>");
    }

    public void call(String[] args) throws CmdException
    {
        if (args.length != 3)
            throw new CmdSyntaxError();

        Feature feature = this.findFeature(args[0]);
        Setting setting = this.findSetting(feature, args[1]);
        ColorSetting colorbox = this.getAsColor(feature, setting);
        this.setColor(colorbox, args[2]);
    }

    private Feature findFeature(String name) throws CmdError
    {
        Stream<Feature> stream = WURST.getNavigator().getList().stream();
        stream = stream.filter((f) -> name.equalsIgnoreCase(f.getName()));
        Feature feature = stream.findFirst().orElse(null);

        if (feature == null)
            throw new CmdError("A feature named \"" + name + "\" could not be found.");

        return feature;
    }

    private Setting findSetting(Feature feature, String name) throws CmdError
    {
        name = name.replace("_", " ").toLowerCase();
        Setting setting = feature.getSettings().get(name);

        if (setting == null)
            throw new CmdError("A setting named \"" + name + "\" could not be found in " + feature.getName() + ".");

        return setting;
    }

    private ColorSetting getAsColor(Feature feature, Setting setting) throws CmdError
    {
        if (!(setting instanceof ColorSetting))
            throw new CmdError(feature.getName() + " " + setting.getName() + " is not a color setting.");

        return (ColorSetting)setting;
    }

    private void setColor(ColorSetting color, String value) throws CmdSyntaxError
    {
        try
        {
            color.setColor(ColorUtils.parse(value));
        }
        catch (Exception ignored)
        {
            throw new CmdSyntaxError("Incorrect color value.");
        }
    }
}
