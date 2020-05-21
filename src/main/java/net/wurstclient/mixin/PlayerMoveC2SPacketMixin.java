package net.wurstclient.mixin;

import net.minecraft.network.Packet;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.wurstclient.mixinterface.IPlayerMoveC2SPacket;
import org.lwjgl.system.CallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(PlayerMoveC2SPacket.class)
public abstract class PlayerMoveC2SPacketMixin
        implements IPlayerMoveC2SPacket {

    // Cringe getter functions can't be bothered to reliably return the actual value

    @Shadow
    private double x;
    @Shadow
    private double y;
    @Shadow
    private double z;
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;

    public double getX() { return this.x; }

    public double getY() { return this.y; }

    public double getZ() { return this.z; }

    public float getYaw() { return this.yaw; }

    public float getPitch() { return this.pitch; }

}
