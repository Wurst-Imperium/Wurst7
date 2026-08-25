/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.InteractFromSetting;
import net.wurstclient.settings.InteractFromSetting.InteractFrom;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.settings.filterlists.RemoteViewFilterList;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.EntityUtils;

@SearchTags({"remote view"})
@DontSaveState
public final class RemoteViewHack extends Hack implements UpdateListener
{
	private final InteractFromSetting interactFrom =
		new InteractFromSetting(this, InteractFrom.CAMERA);
	
	private final CheckboxSetting hideHand = new CheckboxSetting("Hide hand",
		"description.wurst.setting.remoteview.hide_hand", true);
	
	private final EntityFilterList entityFilters =
		RemoteViewFilterList.create();
	
	private LivingEntity entity;
	
	public RemoteViewHack()
	{
		super("RemoteView");
		setCategory(Category.RENDER);
		addSetting(interactFrom);
		addSetting(hideHand);
		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		// Find entity if not already set
		if(entity == null)
		{
			Stream<LivingEntity> stream =
				EntityUtils.getAliveEntities(LivingEntity.class)
					.filter(EntityUtils.IS_NOT_SELF);
			
			stream = entityFilters.applyTo(stream);
			
			entity = stream
				.min(
					Comparator.comparingDouble(EntityUtils::distanceToHitboxSq))
				.orElse(null);
		}
		
		// Check if entity was found and is still valid
		if(!isEntityValid())
		{
			entity = null;
			ChatUtils.error("Could not find a valid entity.");
			setEnabled(false);
			return;
		}
		
		WURST.getHax().freecamHack.setEnabled(false);
		WURST.getHax().lsdHack.setEnabled(false);
		
		MC.setCameraEntity(entity);
		ChatUtils.message("Now viewing " + entity.getName().getString() + ".");
		
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		
		if(entity == null)
			return;
		
		ChatUtils
			.message("No longer viewing " + entity.getName().getString() + ".");
		
		if(MC.getCameraEntity() == entity)
			MC.setCameraEntity(MC.player);
		
		entity = null;
	}
	
	public void onToggledByCommand(String name)
	{
		if(name.isEmpty())
		{
			setEnabled(!isEnabled());
			return;
		}
		
		List<LivingEntity> matches =
			EntityUtils.getAliveEntities(LivingEntity.class)
				.filter(EntityUtils.IS_NOT_SELF)
				.filter(e -> name.equalsIgnoreCase(e.getName().getString()))
				.sorted(
					Comparator.comparingDouble(EntityUtils::distanceToHitboxSq))
				.toList();
		
		if(matches.isEmpty())
		{
			ChatUtils.error("Entity \"" + name + "\" could not be found.");
			return;
		}
		
		LivingEntity newEntity =
			matches.get((matches.indexOf(entity) + 1) % matches.size());
		
		if(isEnabled())
			setEnabled(false);
		
		entity = newEntity;
		setEnabled(true);
	}
	
	@Override
	public void onUpdate()
	{
		if(!isEntityValid() || MC.getCameraEntity() != entity)
			setEnabled(false);
	}
	
	private boolean isEntityValid()
	{
		return entity != null && entity.isAlive() && MC.level != null
			&& MC.level.getEntity(entity.getUUID()) != null;
	}
	
	public boolean isViewingEntity()
	{
		return isEnabled() && entity != null && MC.getCameraEntity() == entity;
	}
	
	public boolean isClickingFromPlayer()
	{
		return isViewingEntity()
			&& interactFrom.getSelected() == InteractFrom.PLAYER;
	}
	
	public boolean shouldHideHand()
	{
		return isViewingEntity() && hideHand.isChecked();
	}
}
