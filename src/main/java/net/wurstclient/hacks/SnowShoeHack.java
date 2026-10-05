/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.Blocks;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.hack.Hack;

@SearchTags({"snow shoe", "SnowJesus", "snow jesus", "NoSnowSink",
	"no snow sink", "AntiSnowSink", "anti snow sink"})
public final class SnowShoeHack extends Hack
	implements PreMotionListener, PostMotionListener, PacketOutputListener
{
	private boolean sentMovement;
	
	public SnowShoeHack()
	{
		super("SnowShoe");
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(PreMotionListener.class, this);
		EVENTS.add(PostMotionListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PreMotionListener.class, this);
		EVENTS.remove(PostMotionListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onPreMotion()
	{
		sentMovement = false;
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof ServerboundMovePlayerPacket)
			sentMovement = true;
	}
	
	@Override
	public void onPostMotion()
	{
		if(sentMovement)
			return;
		
		LocalPlayer player = MC.player;
		if(!player.onGround()
			|| !player.getBlockStateOn().is(Blocks.POWDER_SNOW))
			return;
			
		// Movement packets stop server-side freezing from accumulating on the
		// surface, but vanilla only sends them every 20 ticks when stationary
		player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(
			player.onGround(), player.horizontalCollision));
	}
}
