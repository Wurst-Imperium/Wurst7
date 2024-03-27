/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"telekinesis", "move entities"})
@DontSaveState
public final class TelekinesisHack extends Hack implements UpdateListener
{

private final SliderSetting range = new SliderSetting("Range", 5, 1, 512, 0.000001, ValueDisplay.DECIMAL);

private final ArrayList<ItemEntity> items = new ArrayList<>();
	
	public TelekinesisHack()
	{
		super("Telekinesis" /*Collect items on long distance*/);
		setCategory(Category.OTHER);
        addSetting(range);
	}
	
	@Override
	public void onEnable()
	{	
		// add listener
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		// remove listener
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
        Stream<Entity> stream = EntityUtils.getEntities();
        double rangeSq = Math.pow(range.getValue(), 2) ;
        stream = stream.filter(e -> MC.player.squaredDistanceTo(e) <= rangeSq);
        stream = stream.filter(e -> e instanceof ItemEntity);
        ArrayList<Entity> entities = stream.collect(Collectors.toCollection(ArrayList::new));
        ClientPlayNetworkHandler netHandler = MC.player.networkHandler;
        ClientPlayerEntity player = MC.player;

        if(entities.isEmpty())
        return;

        for(Entity entity : entities) {
        netHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(entity.getX(), entity.getY(), entity.getZ(), entity.isOnGround()));
		}
   //public final TelekinesisHack telekinesisHack = new TelekinesisHack(); to HackList      
	}
}
