/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.clickgui.Window;
import net.wurstclient.clickgui.components.RadarComponent;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.FakePlayerEntity;

@SearchTags({"MiniMap", "mini map"})
public final class RadarHack extends Hack implements UpdateListener
{
	private final Window window;
	private final ArrayList<Entity> entities = new ArrayList<>();
	
	private final SliderSetting radius = new SliderSetting("半径",
		"以块为单位的半径", 100, 1, 100, 1, ValueDisplay.INTEGER);
	private final CheckboxSetting rotate =
		new CheckboxSetting("你旋转时随你一起旋转", true);
	
	private final CheckboxSetting filterPlayers = new CheckboxSetting(
		"排除玩家", "", false);
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"排除睡眠", "", false);
	private final CheckboxSetting filterMonsters = new CheckboxSetting(
		"排除怪物", "", false);
	private final CheckboxSetting filterAnimals = new CheckboxSetting(
		"排除动物", "", false);
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"排除不可见", "", false);
	
	public RadarHack()
	{
		super("雷达",
			"在一个小地图中显示附近实体的位\n"
				+ "\u00a7c红色\u00a7r - 玩家\n"
				+ "\u00a76橙色\u00a7r - 怪物\n"
				+ "\u00a7a绿色\u00a7r - 动物\n"
				+ "\u00a77灰色\u00a7r - 其他\n");
		
		setCategory(Category.RENDER);
		addSetting(radius);
		addSetting(rotate);
		addSetting(filterPlayers);
		addSetting(filterSleeping);
		addSetting(filterMonsters);
		addSetting(filterAnimals);
		addSetting(filterInvisible);
		
		window = new Window("雷达");
		window.setPinned(true);
		window.setInvisible(true);
		window.add(new RadarComponent(this));
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		window.setInvisible(false);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		window.setInvisible(true);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		entities.clear();
		Stream<Entity> stream =
			StreamSupport.stream(world.getEntities().spliterator(), true)
				.filter(e -> !e.isRemoved() && e != player)
				.filter(e -> !(e instanceof FakePlayerEntity))
				.filter(e -> e instanceof LivingEntity)
				.filter(e -> ((LivingEntity)e).getHealth() > 0);
		
		if(filterPlayers.isChecked())
			stream = stream.filter(e -> !(e instanceof PlayerEntity));
		
		if(filterSleeping.isChecked())
			stream = stream.filter(e -> !(e instanceof PlayerEntity
				&& ((PlayerEntity)e).isSleeping()));
		
		if(filterMonsters.isChecked())
			stream = stream.filter(e -> !(e instanceof Monster));
		
		if(filterAnimals.isChecked())
			stream = stream.filter(
				e -> !(e instanceof AnimalEntity || e instanceof AmbientEntity
					|| e instanceof WaterCreatureEntity));
		
		if(filterInvisible.isChecked())
			stream = stream.filter(e -> !e.isInvisible());
		
		entities.addAll(stream.collect(Collectors.toList()));
	}
	
	public Window getWindow()
	{
		return window;
	}
	
	public Iterable<Entity> getEntities()
	{
		return Collections.unmodifiableList(entities);
	}
	
	public double getRadius()
	{
		return radius.getValue();
	}
	
	public boolean isRotateEnabled()
	{
		return rotate.isChecked();
	}
}
