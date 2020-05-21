package net.wurstclient.mixinterface;

import net.minecraft.network.Packet;

public interface IPlayerMoveC2SPacket
{
    double getX();

    double getY();

    double getZ();

    float getYaw();

    float getPitch();
}
