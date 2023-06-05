/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.text.DecimalFormat;

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
import net.wurstclient.util.RenderUtils;

@SearchTags({"health tags"})
public final class HealthTagsHack extends Hack implements RenderListener
{
	private final CheckboxSetting mobs = new CheckboxSetting(
		"Mobs", "Displays health tags above mobs also.", false);
	private final CheckboxSetting unlimitedRange =
		new CheckboxSetting("Unlimited range",
			"Displays mob health tags at any distance.", false);
	private final CheckboxSetting max = new CheckboxSetting(
		"Display max health for players", false);
	private final CheckboxSetting round = new CheckboxSetting(
		"Round health", true);
	
	private static final DecimalFormat DF = new DecimalFormat("0.##");
	
	public HealthTagsHack()
	{
		super("HealthTags");
		setCategory(Category.RENDER);
		addSetting(mobs);
		addSetting(unlimitedRange);
		addSetting(max);
		addSetting(round);
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
		VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
		for(Entity e : MC.world.getEntities())
			if(e instanceof MobEntity entity)
			{
				String health = round.isChecked() ? Integer.toString((int)entity.getHealth())
					: DF.format(entity.getHealth());
				String maxHealth = round.isChecked() ? Integer.toString((int)entity.getMaxHealth())
					: DF.format(entity.getMaxHealth());
				Text text = Text.literal(health + "/" + maxHealth).formatted(
					getColor(entity.getHealth(), entity.getMaxHealth()));
				RenderUtils.renderTag(matrixStack, text, entity, immediate, 0xffffff,
					!entity.hasCustomName() ? 0.5 : 1.0, unlimitedRange.isChecked() ? 1000 : 75, partialTicks);
			}
		immediate.draw();
	}
	
	public Text addHealth(LivingEntity entity, Text nametag)
	{
		if(!isEnabled())
			return nametag;
		
		String health = round.isChecked() ? Integer.toString((int)entity.getHealth())
			: DF.format(entity.getHealth());
		String maxHealth = round.isChecked() ? Integer.toString((int)entity.getMaxHealth())
			: DF.format(entity.getMaxHealth());
		
		MutableText formattedHealth = Text.literal(" ")
			.append(max.isChecked() ? health + "/" + maxHealth : health).formatted(getColor(entity.getHealth(), entity.getMaxHealth()));
		return ((MutableText)nametag).append(formattedHealth);
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
	
	public boolean hasMobHealthTags()
	{
		return isEnabled() && mobs.isChecked();
	}
	
	// See EntityRendererMixin.onRenderLabelIfPresent()
}
