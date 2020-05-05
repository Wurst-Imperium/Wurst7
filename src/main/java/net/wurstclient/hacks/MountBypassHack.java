package net.wurstclient.hacks;


import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.wurstclient.events.PacketOutputListener;

@SearchTags({"mount bypass", "donkey chest"})

public final class MountBypassHack extends Hack implements PacketOutputListener
{
    public MountBypassHack()
    {
        super("MountBypass", "Allows you to mount chests on donkeys\n"
                + "on servers that disable it, allowing\n"
                + "donkey chest duplication glitch.");
        setCategory(Category.OTHER);
    }

    @Override
    public void onEnable()
    {
        EVENTS.add(PacketOutputListener.class, this);
    }

    @Override
    public void onDisable()
    {
        EVENTS.remove(PacketOutputListener.class, this);
    }
    @Override
    public void onSentPacket(PacketOutputEvent event) {
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket)
        {
            PlayerInteractEntityC2SPacket packet = (PlayerInteractEntityC2SPacket)event.getPacket();
            if (packet.getEntity(MC.world) instanceof AbstractDonkeyEntity)
            {
                if (packet.getType() == PlayerInteractEntityC2SPacket.InteractionType.INTERACT_AT)
                {
                    event.cancel();
                }
            }
        }
    }
}