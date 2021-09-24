/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.FakePlayerEntity;

@SearchTags({"remote view"})
@DontSaveState
public final class RemoteViewHack extends Hack
	implements UpdateListener, PacketOutputListener
{
	private final CheckboxSetting filterPlayers = new CheckboxSetting(
		"排除玩家", "", false);
	
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"排除睡眠", "", false);
	
	private final SliderSetting filterFlying =
		new SliderSetting("排除飞行",
			"",
			0, 0, 2, 0.05,
			v -> v == 0 ? "off" : ValueDisplay.DECIMAL.getValueString(v));
	
	private final CheckboxSetting filterMonsters = new CheckboxSetting(
		"排除怪物", "", true);
	
	private final CheckboxSetting filterPigmen =
		new CheckboxSetting("排除猪人", "", true);
	
	private final CheckboxSetting filterEndermen =
		new CheckboxSetting("排除末影人", "", true);
	
	private final CheckboxSetting filterAnimals = new CheckboxSetting(
		"排除动物", "", true);
	
	private final CheckboxSetting filterBabies =
		new CheckboxSetting("排除幼年",
			"", true);
	
	private final CheckboxSetting filterPets =
		new CheckboxSetting("排除宠物",
			"", true);
	
	private final CheckboxSetting filterTraders =
		new CheckboxSetting("排除商人",
			"", true);
	
	private final CheckboxSetting filterGolems =
		new CheckboxSetting("排除魔物",
			"不进入铁傀儡,雪傀儡和潜影贝的视角", true);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"排除隐形", "", false);
	private final CheckboxSetting filterStands = new CheckboxSetting(
		"排除盔甲架", "", true);
	
	private Entity entity = null;
	private boolean wasInvisible;
	
	private FakePlayerEntity fakePlayer;
	
	public RemoteViewHack()
	{
		super("换视","让你进入其他实体的视角\n使用[.rv + 实体名]定义目标实体");
		setCategory(Category.RENDER);
		
		addSetting(filterPlayers);
		addSetting(filterSleeping);
		addSetting(filterFlying);
		addSetting(filterMonsters);
		addSetting(filterPigmen);
		addSetting(filterEndermen);
		addSetting(filterAnimals);
		addSetting(filterBabies);
		addSetting(filterPets);
		addSetting(filterTraders);
		addSetting(filterGolems);
		addSetting(filterInvisible);
		addSetting(filterStands);
	}
	
	@Override
	public void onEnable()
	{
		// find entity if not already set
		if(entity == null)
		{
			Stream<Entity> stream = StreamSupport
				.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> e instanceof LivingEntity)
				.filter(
					e -> !e.isRemoved() && ((LivingEntity)e).getHealth() > 0)
				.filter(e -> e != MC.player)
				.filter(e -> !(e instanceof FakePlayerEntity));
			
			if(filterPlayers.isChecked())
				stream = stream.filter(e -> !(e instanceof PlayerEntity));
			
			if(filterSleeping.isChecked())
				stream = stream.filter(e -> !(e instanceof PlayerEntity
					&& ((PlayerEntity)e).isSleeping()));
			
			if(filterFlying.getValue() > 0)
				stream = stream.filter(e -> {
					
					if(!(e instanceof PlayerEntity))
						return true;
					
					Box box = e.getBoundingBox();
					box = box.union(box.offset(0, -filterFlying.getValue(), 0));
					return !MC.world.isSpaceEmpty(box);
				});
			
			if(filterMonsters.isChecked())
				stream = stream.filter(e -> !(e instanceof Monster));
			
			if(filterPigmen.isChecked())
				stream =
					stream.filter(e -> !(e instanceof ZombifiedPiglinEntity));
			
			if(filterEndermen.isChecked())
				stream = stream.filter(e -> !(e instanceof EndermanEntity));
			
			if(filterAnimals.isChecked())
				stream = stream.filter(e -> !(e instanceof AnimalEntity
					|| e instanceof AmbientEntity
					|| e instanceof WaterCreatureEntity));
			
			if(filterBabies.isChecked())
				stream = stream.filter(e -> !(e instanceof PassiveEntity
					&& ((PassiveEntity)e).isBaby()));
			
			if(filterPets.isChecked())
				stream = stream
					.filter(e -> !(e instanceof TameableEntity
						&& ((TameableEntity)e).isTamed()))
					.filter(e -> !(e instanceof HorseBaseEntity
						&& ((HorseBaseEntity)e).isTame()));
			
			if(filterTraders.isChecked())
				stream = stream.filter(e -> !(e instanceof MerchantEntity));
			
			if(filterGolems.isChecked())
				stream = stream.filter(e -> !(e instanceof GolemEntity));
			
			if(filterInvisible.isChecked())
				stream = stream.filter(e -> !e.isInvisible());
			
			if(filterStands.isChecked())
				stream = stream.filter(e -> !(e instanceof ArmorStandEntity));
			
			entity = stream
				.min(Comparator
					.comparingDouble(e -> MC.player.squaredDistanceTo(e)))
				.orElse(null);
			
			// check if entity was found
			if(entity == null)
			{
				ChatUtils.error("找不到有效实体");
				setEnabled(false);
				return;
			}
		}
		
		// save old data
		wasInvisible = entity.isInvisibleTo(MC.player);
		
		// enable NoClip
		MC.player.noClip = true;
		
		// spawn fake player
		fakePlayer = new FakePlayerEntity();
		
		// success message
		ChatUtils.message("Now viewing " + entity.getName().getString() + ".");
		
		// add listener
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		// remove listener
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		
		// reset entity
		if(entity != null)
		{
			ChatUtils.message(
				"No longer viewing " + entity.getName().getString() + ".");
			entity.setInvisible(wasInvisible);
			entity = null;
		}
		
		// disable NoClip
		MC.player.noClip = false;
		
		// remove fake player
		if(fakePlayer != null)
		{
			fakePlayer.resetPlayerPosition();
			fakePlayer.despawn();
		}
	}
	
	public void onToggledByCommand(String viewName)
	{
		// set entity
		if(!isEnabled() && viewName != null && !viewName.isEmpty())
		{
			entity = StreamSupport
				.stream(MC.world.getEntities().spliterator(), false)
				.filter(e -> e instanceof LivingEntity)
				.filter(
					e -> !e.isRemoved() && ((LivingEntity)e).getHealth() > 0)
				.filter(e -> e != MC.player)
				.filter(e -> !(e instanceof FakePlayerEntity))
				.filter(e -> viewName.equalsIgnoreCase(e.getName().getString()))
				.min(Comparator
					.comparingDouble(e -> MC.player.squaredDistanceTo(e)))
				.orElse(null);
			
			if(entity == null)
			{
				ChatUtils
					.error("Entity \"" + viewName + "\" could not be found.");
				return;
			}
		}
		
		// toggle RemoteView
		setEnabled(!isEnabled());
	}
	
	@Override
	public void onUpdate()
	{
		// validate entity
		if(entity.isRemoved() || ((LivingEntity)entity).getHealth() <= 0)
		{
			setEnabled(false);
			return;
		}
		
		// update position, rotation, etc.
		MC.player.copyPositionAndRotation(entity);
		MC.player.setPos(entity.getX(),
			entity.getY() - MC.player.getEyeHeight(MC.player.getPose())
				+ entity.getEyeHeight(entity.getPose()),
			entity.getZ());
		MC.player.resetPosition();
		MC.player.setVelocity(Vec3d.ZERO);
		
		// set entity invisible
		entity.setInvisible(true);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof PlayerMoveC2SPacket)
			event.cancel();
	}
}
