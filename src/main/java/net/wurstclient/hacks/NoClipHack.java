/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.IsNormalCubeListener;
import net.wurstclient.events.PlayerMoveListener;
import net.wurstclient.events.SetOpaqueCubeListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerEntity;

@SearchTags({"no clip"})
public final class NoClipHack extends Hack implements UpdateListener,
	PlayerMoveListener, IsNormalCubeListener, SetOpaqueCubeListener
{
	public NoClipHack()
	{
		super("NoClip", "Allows you to freely move through blocks.\n"
			+ "A block (e.g. sand) must fall on your head to activate it.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r You will take damage while moving through blocks!");
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PlayerMoveListener.class, this);
		EVENTS.add(IsNormalCubeListener.class, this);
		EVENTS.add(SetOpaqueCubeListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PlayerMoveListener.class, this);
		EVENTS.remove(IsNormalCubeListener.class, this);
		EVENTS.remove(SetOpaqueCubeListener.class, this);
		
		MC.player.noClip = false;
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		player.noClip = true;
		player.fallDistance = 0;
		player.setOnGround(false);
		
		player.abilities.flying = false;
		player.setVelocity(0, 0, 0);
		
		float speed = 0.2F;
		player.flyingSpeed = speed;
		
		if(MC.options.keyJump.isPressed())
			player.addVelocity(0, speed, 0);
		if(MC.options.keySneak.isPressed())
			player.addVelocity(0, -speed, 0);
	}
	
	@Override
	public void onPlayerMove(IClientPlayerEntity player)
	{
		player.setNoClip(true);
	}
	
	@Override
	public void onIsNormalCube(IsNormalCubeEvent event)
	{
		event.cancel();
	}
	
	@Override
	public void onSetOpaqueCube(SetOpaqueCubeEvent event)
	{
		event.cancel();
	}
}
