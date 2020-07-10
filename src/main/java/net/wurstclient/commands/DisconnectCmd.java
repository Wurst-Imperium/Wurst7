package net.wurstclient.commands;

import net.minecraft.text.LiteralText;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;

public class DisconnectCmd extends Command {
    public DisconnectCmd() {
        super("disconnect", "Disconnects you from a server", ".disconnect");
    }

    @Override
    public void call(String[] args) throws CmdException {
        MC.player.networkHandler.getConnection().disconnect(new LiteralText("Disconnected from server successfully"));
    }
}
