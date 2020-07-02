package net.wurstclient.commands;

import com.google.gson.JsonArray;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.TreeSet;

public class HackListCmd extends Command {

    private TreeSet<Hack> hiddenHacks = new TreeSet<>((hack1, hack2) -> hack1.getName().compareToIgnoreCase(hack2.getName()));
    private final Path path;

    public HackListCmd() {
        super("hacklist",
                "Collapse specific hacks into a numeric list to declutter your screen.",
                "Add a hack: .hacklist hide <hackname>", "Remove a hack: .hacklist unhide <hackname> OR .hacklist show <hackname>", "List hidden hacks: .hacklist list");

        path = WURST.getWurstFolder().resolve("hiddenhacklist.json");
        load();
    }

    @Override
    public void call(String[] args) throws CmdException {
        if (args.length < 1)
            throw new CmdSyntaxError();

        // Listing hacks doesn't need another parameter, so put it above the two-argument check.
        if (args[0].toLowerCase().equals("list"))
        {
            listHiddenHacks();
            return;
        }

        if (args.length < 2)
            throw new CmdSyntaxError();

        if (WURST.getHax().getHackByName(args[1]) == null)
            throw new CmdError("The specified hack does not exist.");

        switch (args[0].toLowerCase()) {
            case "hide":
                hideHack(args[1]);
                break;

            case "unhide":
            case "show":
                unhideHack(args[1]);
                break;

            default:
                throw new CmdSyntaxError("Only list, hide, and unhide/show subcommands are supported.");
        }
    }

    private void hideHack(String hackName)
    {
        if (WURST.getHax().getHackByName(hackName) == null) return;

        hiddenHacks.add(WURST.getHax().getHackByName(hackName));
        save();
    }

    private void unhideHack(String hackName)
    {
        if (WURST.getHax().getHackByName(hackName) == null) return;

        hiddenHacks.remove(WURST.getHax().getHackByName(hackName));
        save();
    }

    private void listHiddenHacks()
    {
        StringBuilder sb = new StringBuilder("Hidden hacks: ");
        for (Hack hack : hiddenHacks)
            sb.append(hack.getName()).append(", ");


        ChatUtils.message(sb.toString().replaceAll(", $", ""));
    }

    public TreeSet<Hack> getHiddenHacks()
    {
        return hiddenHacks;
    }

    public void load()
    {
        try
        {
            hiddenHacks.clear();

            for (String hackName : JsonUtils.parseFileToArray(path).getAllStrings())
            {
                Hack hack = WURST.getHax().getHackByName(hackName);
                if (hack == null)
                    return;
                hiddenHacks.add(hack);
            }

        }catch(NoSuchFileException e)
        {
            // The file doesn't exist yet. No problem, we'll create it later.

        }catch(IOException | JsonException e)
        {
            System.out.println("Couldn't load " + path.getFileName());
            e.printStackTrace();
        }

        save();
    }

    private void save()
    {
        try
        {
            JsonUtils.toJson(createJson(), path);

        }catch(IOException | JsonException e)
        {
            System.out.println("Couldn't save " + path.getFileName());
            e.printStackTrace();
        }
    }

    private JsonArray createJson()
    {
        JsonArray json = new JsonArray();
        hiddenHacks.forEach(hack -> json.add(hack.getName()));
        return json;
    }
}
