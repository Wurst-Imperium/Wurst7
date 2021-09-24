/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"feed aura", "BreedAura", "breed aura", "AutoBreeder",
	"auto breeder"})
public final class FeedAuraHack extends Hack
	implements UpdateListener, PostMotionListener, RenderListener
{
	private final SliderSetting range = new SliderSetting("范围",
		"自动喂食给定距离内的生物",
		5, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final EnumSetting<Priority> priority = new EnumSetting<>("优先级",
		"攻击优先级的定义:\n§l[距离]§r:距离最近的实体\n§l[角度]§r:§b[Angle]§r值最小的实体\n§b[Angle]§r值:攻击某实体时,\n你的头部旋转的角度\n§l[生命值]§r生命值最低的实体",
		Priority.values(), Priority.ANGLE);
	
	private final CheckboxSetting filterBabies =
		new CheckboxSetting("排除幼年生物",
			"不喂食幼年生物\n节省食物,但不会加快幼年生物的生长",
			false);
	
	private AnimalEntity target;
	private AnimalEntity renderTarget;
	
	public FeedAuraHack()
	{
		super("喂食光环", "自动喂食周围的动物");
		setCategory(Category.OTHER);
		addSetting(range);
		addSetting(priority);
		addSetting(filterBabies);
	}
	
	@Override
	protected void onEnable()
	{
		// disable other auras
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().triggerBotHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PostMotionListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PostMotionListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		target = null;
		renderTarget = null;
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		ItemStack heldStack = player.getInventory().getMainHandStack();
		
		double rangeSq = Math.pow(range.getValue(), 2);
		Stream<AnimalEntity> stream = StreamSupport
			.stream(MC.world.getEntities().spliterator(), true)
			.filter(e -> !e.isRemoved()).filter(e -> e instanceof AnimalEntity)
			.map(e -> (AnimalEntity)e).filter(e -> e.getHealth() > 0)
			.filter(e -> player.squaredDistanceTo(e) <= rangeSq)
			.filter(e -> e.isBreedingItem(heldStack))
			.filter(AnimalEntity::canEat);
		
		if(filterBabies.isChecked())
			stream = stream.filter(e -> !e.isBaby());
		
		target = stream.min(priority.getSelected().comparator).orElse(null);
		renderTarget = target;
		if(target == null)
			return;
		
		WURST.getRotationFaker()
			.faceVectorPacket(target.getBoundingBox().getCenter());
	}
	
	@Override
	public void onPostMotion()
	{
		if(target == null)
			return;
		
		ClientPlayerInteractionManager im = MC.interactionManager;
		ClientPlayerEntity player = MC.player;
		Hand hand = Hand.MAIN_HAND;
		
		EntityHitResult hitResult = new EntityHitResult(target);
		ActionResult actionResult =
			im.interactEntityAtLocation(player, target, hitResult, hand);
		
		if(!actionResult.isAccepted())
			actionResult = im.interactEntity(player, target, hand);
		
		if(actionResult.isAccepted() && actionResult.shouldSwingHand())
			player.swingHand(hand);
		
		target = null;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(renderTarget == null)
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRenderOffset(matrixStack);
		
		Box box = new Box(BlockPos.ORIGIN);
		float p = 1;
		LivingEntity le = renderTarget;
		p = (le.getMaxHealth() - le.getHealth()) / le.getMaxHealth();
		float red = p * 2F;
		float green = 2 - red;
		
		matrixStack.translate(
			renderTarget.prevX
				+ (renderTarget.getX() - renderTarget.prevX) * partialTicks,
			renderTarget.prevY
				+ (renderTarget.getY() - renderTarget.prevY) * partialTicks,
			renderTarget.prevZ
				+ (renderTarget.getZ() - renderTarget.prevZ) * partialTicks);
		matrixStack.translate(0, 0.05, 0);
		matrixStack.scale(renderTarget.getWidth(), renderTarget.getHeight(),
			renderTarget.getWidth());
		matrixStack.translate(-0.5, 0, -0.5);
		
		if(p < 1)
		{
			matrixStack.translate(0.5, 0.5, 0.5);
			matrixStack.scale(p, p, p);
			matrixStack.translate(-0.5, -0.5, -0.5);
		}
		
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		RenderSystem.setShaderColor(red, green, 0, 0.25F);
		RenderUtils.drawSolidBox(box, matrixStack);
		
		RenderSystem.setShaderColor(red, green, 0, 0.5F);
		RenderUtils.drawOutlinedBox(box, matrixStack);
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private enum Priority
	{
		DISTANCE("距离", e -> MC.player.squaredDistanceTo(e)),
		
		ANGLE("角度",
			e -> RotationUtils
				.getAngleToLookVec(e.getBoundingBox().getCenter())),
		
		HEALTH("生命值", e -> e instanceof LivingEntity
			? ((LivingEntity)e).getHealth() : Integer.MAX_VALUE);
		
		private final String name;
		private final Comparator<Entity> comparator;
		
		private Priority(String name, ToDoubleFunction<Entity> keyExtractor)
		{
			this.name = name;
			comparator = Comparator.comparingDouble(keyExtractor);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
