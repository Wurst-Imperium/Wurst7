/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.nochatreports;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.PlayChannelHandler;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class NoChatReportsChannelHandler implements PlayChannelHandler
{
	public static final Identifier CHANNEL =
		new Identifier("nochatreports", "sync");
	
	public static final NoChatReportsChannelHandler INSTANCE =
		new NoChatReportsChannelHandler();
	
	@Override
	public void receive(MinecraftClient client,
		ClientPlayNetworkHandler handler, PacketByteBuf buf,
		PacketSender responseSender)
	{
		// NO-OP
	}
}
