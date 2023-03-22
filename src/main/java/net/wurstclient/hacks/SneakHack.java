/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.EnumSetting;

import java.util.ArrayList;

@SearchTags({"AutoSneaking"})
public final class SneakHack extends Hack
	implements PreMotionListener, PostMotionListener
{
	private final EnumSetting<SneakMode> mode = new EnumSetting<>("Mode",
		"\u00a7lPacket\u00a7r mode makes it look like you're sneaking without slowing you down.\n"
			+ "\u00a7lLegit\u00a7r mode actually makes you sneak.",
		SneakMode.values(), SneakMode.LEGIT);
	
	public SneakHack()
	{
		super("Sneak");
		setCategory(Category.MOVEMENT);
		addSetting(mode);
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + mode.getSelected() + "]";
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(PreMotionListener.class, this);
		EVENTS.add(PostMotionListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(PreMotionListener.class, this);
		EVENTS.remove(PostMotionListener.class, this);
		
		switch(mode.getSelected())
		{
			case LEGIT:
			IKeyBinding sneakKey = (IKeyBinding)MC.options.sneakKey;
			sneakKey.resetPressedState();
			break;
			
			case PACKET:
			sendSneakPacket(Mode.RELEASE_SHIFT_KEY);
			break;
		}
	}
	
	@Override
	public void onPreMotion()
	{

		boolean isFlying = checkFly();

		KeyBinding sneakKey = MC.options.sneakKey;

		switch(mode.getSelected()) 
		{
			case LEGIT:
				if(!isFlying){
					sneakKey.setPressed(true);
				}else if(sneakKey.isPressed() && !isOnGround()){
					sneakKey.setPressed(true);
				}
				else{
					sneakKey.setPressed(false);
				}
				break;

			case PACKET:
			((IKeyBinding) sneakKey).resetPressedState();
			sendSneakPacket(Mode.PRESS_SHIFT_KEY);
			sendSneakPacket(Mode.RELEASE_SHIFT_KEY);
			break;
		}
	}
	
	@Override
	public void onPostMotion() 
	{

		if (mode.getSelected() != SneakMode.PACKET)
			return;

			sendSneakPacket(Mode.RELEASE_SHIFT_KEY);
			sendSneakPacket(Mode.PRESS_SHIFT_KEY);
	}

	private boolean checkFly()
	{

		boolean ground = isOnGround();

		ClientPlayerEntity player = MC.player;
		int height = (int)player.getPos().y - 1;
		BlockState blockStateBelow = player.world.getBlockState(new BlockPos((int)player.getPos().x, height, (int)player.getPos().z));

		if(player.world.getBlockState(new BlockPos((int)player.getPos().x, height-1, (int)player.getPos().z)).getMaterial() != Material.AIR && !MC.options.jumpKey.isPressed()){
			MC.options.sneakKey.setPressed(true);
		}

		if(player.getAbilities().flying){
			return true;
		}else if(blockStateBelow.getMaterial() == Material.AIR && !ground){
			return true;
		}else if(MC.options.jumpKey.isPressed()){
			return true;
		}
		else{
			return false;
		}
	}

	private boolean isOnGround() 
	{
		ClientPlayerEntity player = MC.player;
		ArrayList<BlockState> blocks = new ArrayList<>();

		for(int x = 2; x >= -2; x--){
			for(int z = 2; z >= -2; z--){
				BlockPos block = new BlockPos((int)player.getPos().x + x, (int)player.getPos().y-1, (int)player.getPos().z + z);
				blocks.add(player.world.getBlockState(block));
			}
		}

		for(BlockState block : blocks)
		{
			if(block.getMaterial() != Material.AIR){
				System.out.println(block.getMaterial());
				return true;
			}
		}

		return false;

	}
	
	private void sendSneakPacket(Mode mode)
	{
		ClientPlayerEntity player = MC.player;
		ClientCommandC2SPacket packet =
			new ClientCommandC2SPacket(player, mode);
		player.networkHandler.sendPacket(packet);
	}
	
	private enum SneakMode
	{
		PACKET("Packet"),
		LEGIT("Legit");
		
		private final String name;
		
		private SneakMode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
