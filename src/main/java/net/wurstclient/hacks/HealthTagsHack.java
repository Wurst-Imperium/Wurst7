/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
	private static final DecimalFormat DF =
		new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.ENGLISH));
	
	private final CheckboxSetting mobs = new CheckboxSetting("Mobs",
		"Displays health tags above mobs also.", false);
	
	private final CheckboxSetting showMaxHealth =
		new CheckboxSetting("Show max health", "Also displays the entity's"
			+ " maximum health in addition to its current health.", false);
	
	private final CheckboxSetting round =
		new CheckboxSetting("Round health", true);
	
	public HealthTagsHack()
	{
		super("HealthTags");
		setCategory(Category.RENDER);
		addSetting(mobs);
		addSetting(showMaxHealth);
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
	
	public Text addHealth(LivingEntity entity, Text nametag)
	{
		if(!isEnabled())
			return nametag;
		
		float curHealth = entity.getHealth();
		float maxHealth = entity.getMaxHealth();
		
		String curHealthString =
			round.isChecked() ? "" + (int)curHealth : DF.format(curHealth);
		
		String maxHealthString =
			round.isChecked() ? "" + (int)maxHealth : DF.format(maxHealth);
		
		String healthString = showMaxHealth.isChecked()
			? curHealthString + "/" + maxHealthString : curHealthString;
		
		if(!nametag.getString().isEmpty())
			nametag = ((MutableText)nametag).append(Text.literal(" "));
		
		MutableText formattedHealth = Text.literal(healthString)
			.formatted(getColor(curHealth, maxHealth));
		
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
