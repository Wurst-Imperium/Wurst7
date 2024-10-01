/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.RoundingPrecisionSetting;
import net.wurstclient.util.RenderUtils;

@SearchTags({"health tags"})
public final class HealthTagsHack extends Hack implements RenderListener
{
	private final CheckboxSetting mobs = new CheckboxSetting("Mobs",
		"Displays health tags above mobs also.", false);
	
	private final CheckboxSetting showMaxHealth =
		new CheckboxSetting("Show max health", "Also displays the entity's"
			+ " maximum health in addition to its current health.", false);
	
	private final RoundingPrecisionSetting precision =
		new RoundingPrecisionSetting("Precision",
			"Rounds the health value to the given number of decimal places.", 0,
			0, 3);
	
	public HealthTagsHack()
	{
		super("HealthTags");
		setCategory(Category.RENDER);
		addSetting(mobs);
		addSetting(showMaxHealth);
		addSetting(precision);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(!mobs.isChecked())
			return;
		
		VertexConsumerProvider.Immediate immediate = VertexConsumerProvider
			.immediate(Tessellator.getInstance().getBuffer());
		
		for(Entity e : MC.world.getEntities())
		{
			if(!(e instanceof MobEntity entity))
				continue;
			
			Text text = addHealth(entity, Text.literal(""));
			RenderUtils.renderTag(matrixStack, text, entity, immediate,
				0xffffff, !entity.hasCustomName() ? 0.5 : 1, partialTicks);
		}
		
		immediate.draw();
	}
	
	public Text addHealth(LivingEntity entity, MutableText nametag)
	{
		if(!isEnabled())
			return nametag;
		
		float health = entity.getHealth();
		float maxHealth = entity.getMaxHealth();
		Formatting color = getColor(health, maxHealth);
		
		String healthString = precision.format(health);
		if(showMaxHealth.isChecked())
			healthString += "/" + precision.format(maxHealth);
		
		if(!nametag.getString().isEmpty())
			nametag = nametag.append(Text.literal(" "));
		
		return nametag.append(Text.literal(healthString).formatted(color));
	}
	
	private Formatting getColor(float health, float maxHealth)
	{
		if(health <= maxHealth * 0.25)
			return Formatting.DARK_RED;
		
		if(health <= maxHealth * 0.5)
			return Formatting.GOLD;
		
		if(health <= maxHealth * 0.75)
			return Formatting.YELLOW;
		
		return Formatting.GREEN;
	}
	
	// See EntityRendererMixin.onRenderLabelIfPresent()
}
