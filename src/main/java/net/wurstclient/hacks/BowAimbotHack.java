/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
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

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.mob.ZombiePigmanEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
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
			+ "\u00a7lAngle\u00a7r - Attacks the entity that requires\n"
			+ "the least head movement.\n"
			+ "\u00a7lHealth\u00a7r - Attacks the weakest entity.",
		Priority.values(), Priority.ANGLE);

	private final CheckboxSetting filterPlayers = new CheckboxSetting(
		"Filter players", "Won't attack other players.", false);
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"Filter sleeping", "Won't attack sleeping players.", false);
	private final SliderSetting filterFlying =
		new SliderSetting("Filter flying",
			"Won't attack players that\n" + "are at least the given\n"
				+ "distance above ground.",
			0, 0, 2, 0.05,
			v -> v == 0 ? "off" : ValueDisplay.DECIMAL.getValueString(v));

	private final CheckboxSetting filterMonsters = new CheckboxSetting(
		"Filter monsters", "Won't attack zombies, creepers, etc.", false);
	private final CheckboxSetting filterPigmen = new CheckboxSetting(
		"Filter pigmen", "Won't attack zombie pigmen.", false);
	private final CheckboxSetting filterEndermen =
		new CheckboxSetting("Filter endermen", "Won't attack endermen.", false);

	private final CheckboxSetting filterAnimals = new CheckboxSetting(
		"Filter animals", "Won't attack pigs, cows, etc.", false);
	private final CheckboxSetting filterBabies =
		new CheckboxSetting("Filter babies",
			"Won't attack baby pigs,\n" + "baby villagers, etc.", false);
	private final CheckboxSetting filterPets =
		new CheckboxSetting("Filter pets",
			"Won't attack tamed wolves,\n" + "tamed horses, etc.", false);

	private final CheckboxSetting filterVillagers = new CheckboxSetting(
		"Filter villagers", "Won't attack villagers.", false);
	private final CheckboxSetting filterGolems =
		new CheckboxSetting("Filter golems",
			"Won't attack iron golems,\n" + "snow golems and shulkers.", false);

	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"Filter invisible", "Won't attack invisible entities.", false);

	private static final Box TARGET_BOX =
		new Box(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);

	private Entity target;
	private float bowPower;
	private final double g = 0.05F;
	private final double k = 0.01F;

	public BowAimbotHack()
	{
		super("BowAimbot", "Automatically aims your bow or crossbow.");

		setCategory(Category.COMBAT);
		addSetting(priority);

		addSetting(filterPlayers);
		addSetting(filterSleeping);
		addSetting(filterFlying);
		addSetting(filterMonsters);
		addSetting(filterPigmen);
		addSetting(filterEndermen);
		addSetting(filterAnimals);
		addSetting(filterBabies);
		addSetting(filterPets);
		addSetting(filterVillagers);
		addSetting(filterGolems);
		addSetting(filterInvisible);
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
		ItemStack stack = MC.player.inventory.getMainHandStack();
		Item item = stack.getItem();
		if(!(item instanceof BowItem || item instanceof CrossbowItem))
		{
			target = null;
			return;
		}

		// check if using bow
		if(item instanceof BowItem && !MC.options.keyUse.isPressed()
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
		Stream<LivingEntity> stream = StreamSupport
			.stream(MC.world.getEntities().spliterator(), true)
			.filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity)e)
			.filter(e -> !e.removed && e.getHealth() > 0)
			.filter(e -> e != player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> !WURST.getFriends().contains(e.getEntityName()));

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
				return MC.world.doesNotCollide(box);
			});

		if(filterMonsters.isChecked())
			stream = stream.filter(e -> !(e instanceof Monster));

		if(filterPigmen.isChecked())
			stream = stream.filter(e -> !(e instanceof ZombiePigmanEntity));

		if(filterEndermen.isChecked())
			stream = stream.filter(e -> !(e instanceof EndermanEntity));

		if(filterAnimals.isChecked())
			stream = stream.filter(
				e -> !(e instanceof AnimalEntity || e instanceof AmbientEntity
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

		if(filterVillagers.isChecked())
			stream = stream.filter(e -> !(e instanceof VillagerEntity));

		if(filterGolems.isChecked())
			stream = stream.filter(e -> !(e instanceof GolemEntity));

		if(filterInvisible.isChecked())
			stream = stream.filter(e -> !e.isInvisible());

		target = stream.min(priority.getSelected().comparator).orElse(null);
		if(target == null)
			return;

		// set velocity
		bowPower = (72000 - player.getItemUseTimeLeft()) / 20F;
		bowPower = (bowPower * bowPower + bowPower * 2) / 3;
		if(bowPower > 1 || bowPower < 0.1)
			bowPower = 1;

		float velocity = bowPower * 3F;

		double targetVelocityX = (target.getX() - target.lastRenderX);
		double targetVelocityY = (target.getY() - target.lastRenderY);
		double targetVelocityZ = (target.getZ() - target.lastRenderZ);
		double playerVelocityX = player.getVelocity().getX();
		double playerVelocityY = player.getVelocity().getY();
		double playerVelocityZ = player.getVelocity().getZ();

		// set position to aim at
		double d = RotationUtils.getEyesPos()
			.distanceTo(target.getBoundingBox().getCenter());
		double timeEstimated = d / velocity;
		double posX = target.getX() + targetVelocityX * timeEstimated
			- player.getX();
		double posY = target.getY() + targetVelocityY * timeEstimated
			+ target.getHeight() * 0.5 - player.getY()
			- (player.getStandingEyeHeight() - 0.1);
		double posZ = target.getZ() + targetVelocityZ * timeEstimated
			- player.getZ();

		// set yaw
		MC.player.yaw = (float)Math.toDegrees(Math.atan2(posZ, posX)) - 90;

		// calculate needed pitch
		double hDistance = Math.sqrt(posX * posX + posZ * posZ);
		double hDistanceSq = hDistance * hDistance;
		float velocitySq = velocity * velocity;
		float velocityPow4 = velocitySq * velocitySq;
		double theta = Math.atan((velocitySq - Math
			.sqrt(velocityPow4 - g * (g * hDistanceSq + 2 * posY * velocitySq)))
			/ (g * hDistance));

		if (!Double.isNaN(theta)) {
			final double epsilon = 0.001;
			final double deltaTheta = 0.0001;
			double playerVelocityH = Math.sqrt(playerVelocityX * playerVelocityX + playerVelocityZ * playerVelocityZ);
			double ft = toBeGotRootFunction(theta, playerVelocityH, playerVelocityY, hDistance, posY, velocity);
			int count = 0;
			while ((Math.abs(ft) > epsilon || count <= 5) && count <= 100) {
				timeEstimated = getEstimatedTime(hDistance, playerVelocityH + velocity * Math.cos(theta));
				posX = target.getX() + targetVelocityX * timeEstimated
						- player.getX();
				posY = target.getY() + targetVelocityY * timeEstimated
						+ target.getHeight() * 0.5 - player.getY()
						- (player.getStandingEyeHeight() - 0.1);
				posZ = target.getZ() + targetVelocityZ * timeEstimated
						- player.getZ();
				hDistance = Math.sqrt(posX * posX + posZ * posZ);

				ft = toBeGotRootFunction(theta, playerVelocityH, playerVelocityY, hDistance, posY, velocity);
				double error2 =
						toBeGotRootFunction(theta + deltaTheta, playerVelocityH, playerVelocityY, hDistance, posY, velocity);
				double dfdt = (error2 - ft) / deltaTheta;
				if (Double.isNaN(dfdt))
					break;

				theta -= ft / dfdt;

				count += 1;
			}

			if(ft > epsilon || Double.isNaN(ft))
				WURST.getRotationFaker()
						.faceVectorClient(target.getBoundingBox().getCenter());
			else
				MC.player.pitch = (float)-Math.toDegrees(theta);
		} else {
			WURST.getRotationFaker()
					.faceVectorClient(target.getBoundingBox().getCenter());
		}
	}

	private double getEstimatedTime(double deltaH, double vx0) {
		return  Math.log(1 - (deltaH * k) / vx0) / Math.log(1 - k);
	}

	private double toBeGotRootFunction(double theta, double playerVelocityH, double playerVelocityY
			, double deltaH, double deltaY, double velocity) {
		double vx0 = playerVelocityH + velocity * Math.cos(theta);
		if (1 - (deltaH * k) / vx0 < 0)
			return Double.NaN;
		return deltaY - ( - g * getEstimatedTime(deltaH, vx0)
				+ deltaH * (g + k * (playerVelocityY + velocity * Math.sin(theta))) / vx0) / k;
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(target == null)
			return;

		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);

		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();

		// set position
		GL11.glTranslated(target.getX(), target.getY(), target.getZ());

		// set size
		double boxWidth = target.getWidth() + 0.1;
		double boxHeight = target.getHeight() + 0.1;
		GL11.glScaled(boxWidth, boxHeight, boxWidth);

		// move to center
		GL11.glTranslated(0, 0.5, 0);

		double v = 1 / bowPower;
		GL11.glScaled(v, v, v);

		// draw outline
		GL11.glColor4d(1, 0, 0, 0.5F * bowPower);
		RenderUtils.drawOutlinedBox(TARGET_BOX);

		// draw box
		GL11.glColor4d(1, 0, 0, 0.25F * bowPower);
		RenderUtils.drawSolidBox(TARGET_BOX);

		GL11.glPopMatrix();

		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}

	@Override
	public void onRenderGUI(float partialTicks)
	{
		if(target == null)
			return;

		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);

		GL11.glPushMatrix();

		String message;
		if(bowPower < 1)
			message = "Charging: " + (int)(bowPower * 100) + "%";
		else
			message = "Ready To Shoot";

		// translate to center
		Window sr = MC.getWindow();
		int msgWidth = MC.textRenderer.getStringWidth(message);
		GL11.glTranslated(sr.getScaledWidth() / 2 - msgWidth / 2,
			sr.getScaledHeight() / 2 + 1, 0);

		// background
		GL11.glColor4f(0, 0, 0, 0.5F);
		GL11.glBegin(GL11.GL_QUADS);
		{
			GL11.glVertex2d(0, 0);
			GL11.glVertex2d(msgWidth + 3, 0);
			GL11.glVertex2d(msgWidth + 3, 10);
			GL11.glVertex2d(0, 10);
		}
		GL11.glEnd();

		// text
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		MC.textRenderer.draw(message, 2, 1, 0xffffffff);

		GL11.glPopMatrix();

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

		HEALTH("Health", e -> e.getHealth());

		private final String name;
		private final Comparator<LivingEntity> comparator;

		private Priority(String name,
			ToDoubleFunction<LivingEntity> keyExtractor)
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
