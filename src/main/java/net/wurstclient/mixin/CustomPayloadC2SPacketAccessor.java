/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
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
