/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"bow aimbot"})
public final class BowAimbotHack extends Hack
	implements UpdateListener, RenderListener, GUIRenderListener
{
	private final EnumSetting<Priority> priority = new EnumSetting<>("Priority",
		"Determines which entity will be attacked first.\n"
			+ "\u00a7lDistance\u00a7r - Attacks the closest entity.\n"
			+ "\u00a7lAngle\u00a7r - Attacks the entity that requires the least head movement.\n"
			+ "\u00a7lHealth\u00a7r - Attacks the weakest entity.",
		Priority.values(), Priority.ANGLE);
	
	private final SliderSetting predictMovement = new SliderSetting(
		"Predict movement",
		"Controls the strength of BowAimbot's movement prediction algorithm.",
		0.2, 0, 2, 0.01, ValueDisplay.PERCENTAGE);
	
	private final EntityFilterList entityFilters =
		EntityFilterList.genericCombat();
	
	private final ColorSetting color = new ColorSetting("ESP color",
		"Color of the box that BowAimbot draws around the target.", Color.RED);
	
	private static final Box TARGET_BOX =
		new Box(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
	
	private Entity target;
	private float velocity;
	
	public BowAimbotHack()
	{
		super("BowAimbot");
		
		setCategory(Category.COMBAT);
		addSetting(priority);
		addSetting(predictMovement);
		
		entityFilters.forEach(this::addSetting);
		
		addSetting(color);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(GUIRenderListener.class, this);
		EVENTS.add(RenderListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(GUIRenderListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		// check if item is ranged weapon
		ItemStack stack = MC.player.getInventory().getMainHandStack();
		Item item = stack.getItem();
		if(!(item instanceof BowItem || item instanceof CrossbowItem))
		{
			target = null;
			return;
		}
		
		// check if using bow
		if(item instanceof BowItem && !MC.options.useKey.isPressed()
			&& !player.isUsingItem())
		{
			target = null;
			return;
		}
		
		// check if crossbow is loaded
		if(item instanceof CrossbowItem && !CrossbowItem.isCharged(stack))
		{
			target = null;
			return;
		}
		
		// set target
		if(filterEntities(Stream.of(target)) == null)
			target = filterEntities(StreamSupport
				.stream(MC.world.getEntities().spliterator(), true));
		
		if(target == null)
			return;
		
		// set velocity
		velocity = (72000 - player.getItemUseTimeLeft()) / 20F;
		velocity = (velocity * velocity + velocity * 2) / 3;
		if(velocity > 1)
			velocity = 1;
		
		// set position to aim at
		double d = RotationUtils.getEyesPos().distanceTo(
			target.getBoundingBox().getCenter()) * predictMovement.getValue();
		double posX = target.getX() + (target.getX() - target.lastRenderX) * d
			- player.getX();
		double posY = target.getY() + (target.getY() - target.lastRenderY) * d
			+ target.getHeight() * 0.5 - player.getY()
			- player.getEyeHeight(player.getPose());
		double posZ = target.getZ() + (target.getZ() - target.lastRenderZ) * d
			- player.getZ();
		
		// set yaw
		MC.player.setYaw((float)Math.toDegrees(Math.atan2(posZ, posX)) - 90);
		
		// calculate needed pitch
		double hDistance = Math.sqrt(posX * posX + posZ * posZ);
		double hDistanceSq = hDistance * hDistance;
		float g = 0.006F;
		float velocitySq = velocity * velocity;
		float velocityPow4 = velocitySq * velocitySq;
		float neededPitch = (float)-Math.toDegrees(Math.atan((velocitySq - Math
			.sqrt(velocityPow4 - g * (g * hDistanceSq + 2 * posY * velocitySq)))
			/ (g * hDistance)));
		
		// set pitch
		if(Float.isNaN(neededPitch))
			WURST.getRotationFaker()
				.faceVectorClient(target.getBoundingBox().getCenter());
		else
			MC.player.setPitch(neededPitch);
	}
	
	private Entity filterEntities(Stream<Entity> s)
	{
		Stream<Entity> stream = s.filter(e -> e != null && !e.isRemoved())
			.filter(e -> e instanceof LivingEntity
				&& ((LivingEntity)e).getHealth() > 0
				|| e instanceof EndCrystalEntity
				|| e instanceof ShulkerBulletEntity)
			.filter(e -> e != MC.player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> !WURST.getFriends().contains(e.getEntityName()));
		
		stream = entityFilters.applyTo(stream);
		
		return stream.min(priority.getSelected().comparator).orElse(null);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(target == null)
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRenderOffset(matrixStack);
		
		// set position
		matrixStack.translate(target.getX(), target.getY(), target.getZ());
		
		// set size
		float boxWidth = target.getWidth() + 0.1F;
		float boxHeight = target.getHeight() + 0.1F;
		matrixStack.scale(boxWidth, boxHeight, boxWidth);
		
		// move to center
		matrixStack.translate(0, 0.5, 0);
		
		float v = 1 / velocity;
		matrixStack.scale(v, v, v);
		
		RenderSystem.setShader(GameRenderer::getPositionShader);
		float[] colorF = color.getColorF();
		
		// draw outline
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2],
			0.5F * velocity);
		RenderUtils.drawOutlinedBox(TARGET_BOX, matrixStack);
		
		// draw box
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2],
			0.25F * velocity);
		RenderUtils.drawSolidBox(TARGET_BOX, matrixStack);
		
		matrixStack.pop();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	@Override
	public void onRenderGUI(MatrixStack matrixStack, float partialTicks)
	{
		if(target == null)
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		matrixStack.push();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		String message;
		if(velocity < 1)
			message = "Charging: " + (int)(velocity * 100) + "%";
		else
			message = "Target Locked";
		
		// translate to center
		Window sr = MC.getWindow();
		int msgWidth = MC.textRenderer.getWidth(message);
		matrixStack.translate(sr.getScaledWidth() / 2 - msgWidth / 2,
			sr.getScaledHeight() / 2 + 1, 0);
		
		// background
		RenderSystem.setShader(GameRenderer::getPositionShader);
		RenderSystem.setShaderColor(0, 0, 0, 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, msgWidth + 3, 0, 0).next();
		bufferBuilder.vertex(matrix, msgWidth + 3, 10, 0).next();
		bufferBuilder.vertex(matrix, 0, 10, 0).next();
		tessellator.draw();
		
		// text
		MC.textRenderer.draw(matrixStack, message, 2, 1, 0xffffffff);
		
		matrixStack.pop();
		
		// GL resets
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private enum Priority
	{
		DISTANCE("Distance", e -> MC.player.squaredDistanceTo(e)),
		
		ANGLE("Angle",
			e -> RotationUtils
				.getAngleToLookVec(e.getBoundingBox().getCenter())),
		
		HEALTH("Health", e -> e instanceof LivingEntity
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
