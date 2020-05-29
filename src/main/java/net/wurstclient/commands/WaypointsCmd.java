package net.wurstclient.commands;

import net.wurstclient.WurstClient;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hacks.WaypointsHack;
import net.wurstclient.util.MathUtils;

public class WaypointsCmd extends Command {

    private final WaypointsHack waypointsHack = WurstClient.INSTANCE.getHax().waypointsHack;

    public WaypointsCmd() {
        super("wp", "Waypoint commands.", ".wp add <x> <y> <z> (optional)<name>", ".wp del <name>", ".wp clear");
    }
    public void call(String[] args) throws CmdException
    {
        if(args.length == 0)
            throw new CmdSyntaxError();

        switch(args[0].toLowerCase())
        {
            case "add":
                addWaypoint(args);
                break;
            case "del":
                deleteWaypoint(args);
                break;
            case "clear":
                clearWaypoints();
                break;
            default:
                throw new CmdSyntaxError();
        }
    }

    public void addWaypoint(String[] args) throws CmdException
    {
        if(args.length < 4)
            throw new CmdSyntaxError();
        for(int i=1; i<4; i++) {
            if(!MathUtils.isDouble(args[i]))
                throw new CmdSyntaxError();
        }

        if(args.length == 4)
            waypointsHack.addWaypoint("", Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]));
        else {
            String name = "";
            for(int i=4; i<args.length; i++)
                name += args[i] + " ";
            name = name.trim();
            if(!waypointsHack.addWaypoint(name, Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3])))
                throw new CmdError("Waypoint: " + args[4] + " already exists.");
        }

    }

    public void deleteWaypoint(String[] args) throws CmdException
    {
        if(args.length < 2)
            throw new CmdSyntaxError();
        String name = "";
        for(int i=1; i < args.length; i++)
        {
            name += args[i] + " ";
        }
        name = name.trim();
        if(!waypointsHack.removeWaypoint(name))
            throw new CmdError("Waypoint: " + name + " does not exist.");
    }

    public void clearWaypoints()
    {
        waypointsHack.removeAllWaypoints();
    }
}
