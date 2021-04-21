package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;

/**
 * Same as
 * {@link net.fabricmc.fabric.mixin.networking.accessor.CustomPayloadC2SPacketAccessor}
 * and
 * {@link net.fabricmc.fabric.mixin.networking.CustomPayloadC2SPacketAccessor},
 * except that this class doesn't change its package based on what Fabric API
 * version you have.
 */
@Mixin(CustomPayloadC2SPacket.class)
public interface CustomPayloadC2SPacketAccessor
{
	@Accessor
	Identifier getChannel();
	
	@Accessor
	PacketByteBuf getData();
}
