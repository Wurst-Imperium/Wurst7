package net.wurstclient.hacks;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.StringHelper;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@SearchTags("mass tpa here")
public class MassTpaHerehack extends Hack
    implements UpdateListener, ChatInputListener {
    private final Random random = new Random();
    private final ArrayList<String> players = new ArrayList<>();

    private int index;
    private int timer;

    public MassTpaHerehack()
    {
        super("MassTPAHERE");
        setCategory(Category.CHAT);
    }

    @Override
    public void onEnable()
    {
        index = 0;
        timer = -1;

        players.clear();
        String playerName = MC.getSession().getProfile().getName();

        for(PlayerListEntry info : MC.player.networkHandler.getPlayerList())
        {
            String name = info.getProfile().getName();
            name = StringHelper.stripTextFormat(name);

            if(name.equalsIgnoreCase(playerName))
                continue;

            players.add(name);
        }

        Collections.shuffle(players, random);

        EVENTS.add(ChatInputListener.class, this);
        EVENTS.add(UpdateListener.class, this);

        if(players.isEmpty())
        {
            ChatUtils.error("Couldn't find any players.");
            setEnabled(false);
        }
    }

    @Override
    public void onDisable()
    {
        EVENTS.remove(ChatInputListener.class, this);
        EVENTS.remove(UpdateListener.class, this);
    }

    @Override
    public void onUpdate()
    {
        if(timer > -1)
        {
            timer--;
            return;
        }

        if(index >= players.size())
        {
            setEnabled(false);
            return;
        }

        MC.getNetworkHandler().sendChatCommand("tpahere " + players.get(index));
        index++;
        timer = 20;
    }

    @Override
    public void onReceivedMessage(ChatInputEvent event)
    {
        String message = event.getComponent().getString().toLowerCase();
        if(message.startsWith("\u00a7c[\u00a76wurst\u00a7c]"))
            return;

        if(message.contains("/help") || message.contains("permission"))
        {
            event.cancel();
            ChatUtils.error("This server doesn't have TPAHERE.");
            setEnabled(false);

        }else if(message.contains("accepted") && message.contains("request")
                || message.contains("akzeptiert") && message.contains("anfrage"))
        {
            event.cancel();
            ChatUtils.message("Someone accepted your TPAHERE request. Stopping.");
            setEnabled(false);
        }
    }
}