/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filters.FilterBabiesSetting;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"feed aura", "BreedAura", "breed aura", "AutoBreeder",
	"auto breeder"})
public final class FeedAuraHack extends Hack
	implements UpdateListener, PostMotionListener, RenderListener
{
	private final SliderSetting range = new SliderSetting("Range",
		"Determines how far FeedAura will reach to feed animals.\n"
			+ "Anything that is further away than the specified value will not be fed.",
		5, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final FilterBabiesSetting filterBabies =
		new FilterBabiesSetting("Won't feed baby animals.\n"
			+ "Saves food, but doesn't speed up baby growth.", true);
	
	private final CheckboxSetting filterUntamed =
		new CheckboxSetting("Filter untamed",
			"Won't feed tameable animals that haven't been tamed yet.", false);
	
	private final CheckboxSetting filterHorses = new CheckboxSetting(
		"Filter horse-like animals",
		"Won't feed horses, llamas, donkeys, etc.\n"
			+ "Recommended due to Minecraft bug MC-233276, which causes these animals to consume items indefinitely.",
		true);
	
	private final Random random = new Random();
	private AnimalEntity target;
	private AnimalEntity renderTarget;
	
	public FeedAuraHack()
	{
		super("FeedAura");
		setCategory(Category.OTHER);
		addSetting(range);
		addSetting(filterBabies);
		addSetting(filterUntamed);
		addSetting(filterHorses);
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
		
		double rangeSq = range.getValueSq();
		Stream<AnimalEntity> stream = EntityUtils.getValidAnimals()
			.filter(e -> player.squaredDistanceTo(e) <= rangeSq)
			.filter(e -> e.isBreedingItem(heldStack))
			.filter(AnimalEntity::canEat);
		
		if(filterBabies.isChecked())
			stream = stream.filter(filterBabies);
		
		if(filterUntamed.isChecked())
			stream = stream.filter(e -> !isUntamed(e));
		
		if(filterHorses.isChecked())
			stream = stream.filter(e -> !(e instanceof AbstractHorseEntity));
		
		// convert targets to list
		ArrayList<AnimalEntity> targets =
			stream.collect(Collectors.toCollection(ArrayList::new));
		
		// pick a target at random
		target = targets.isEmpty() ? null
			: targets.get(random.nextInt(targets.size()));
		
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
		
		// create realistic hit result
		Box box = target.getBoundingBox();
		Vec3d start = RotationUtils.getEyesPos();
		Vec3d end = box.getCenter();
		Vec3d hitVec = box.raycast(start, end).orElse(start);
		EntityHitResult hitResult = new EntityHitResult(target, hitVec);
		
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
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		Box box = new Box(BlockPos.ORIGIN);
		float p = 1;
		LivingEntity le = renderTarget;
		p = (le.getMaxHealth() - le.getHealth()) / le.getMaxHealth();
		float green = p * 2F;
		float red = 2 - green;
		
		Vec3d lerpedPos = EntityUtils.getLerpedPos(renderTarget, partialTicks)
			.subtract(region.toVec3d());
		matrixStack.translate(lerpedPos.x, lerpedPos.y, lerpedPos.z);
		
		matrixStack.translate(0, 0.05, 0);
		matrixStack.scale(renderTarget.getWidth(), renderTarget.getHeight(),
			renderTarget.getWidth());
		matrixStack.translate(-0.5, 0, -0.5);
		
		matrixStack.translate(0.5, 0.5, 0.5);
		matrixStack.scale(p, p, p);
		matrixStack.translate(-0.5, -0.5, -0.5);
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		RenderSystem.setShaderColor(red, green, 0, 0.25F);
		RenderUtils.drawSolidBox(box, matrixStack);
		
		RenderSystem.setShaderColor(red, green, 0, 0.5F);
		RenderUtils.drawOutlinedBox(box, matrixStack);
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private boolean isUntamed(AnimalEntity e)
	{
		if(e instanceof AbstractHorseEntity horse && !horse.isTame())
			return true;
		
		if(e instanceof TameableEntity tame && !tame.isTamed())
			return true;
		
		return false;
	}
}
