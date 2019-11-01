package net.wurstclient.hacks;

import com.google.common.collect.Streams;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.TrajectoryPath;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TrajectoriesHack extends Hack implements RenderListener {

	private static final int MAX_POINTS = 1000; // Max points in a trajectory. Bigger means longer traces in air, but takes longer to calculate.
	private static final long DEFAULT_COLOR = 0x00FF007FL;

	private final ArrayList<PlayerEntity> livingEntities = new ArrayList<>();

	public TrajectoriesHack()
	{
		super("Trajectories", "Shows the trajectory of ranged items");
		setCategory(Category.RENDER);
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
	public void onRender(float partialTicks)
	{
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		//GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2.0f);



		//RenderUtils.drawSolidBox(new AxisAlignedBB(new BlockPos(0 - TileEntityRendererDispatcher.staticPlayerX, 100 - TileEntityRendererDispatcher.staticPlayerY, 0 - TileEntityRendererDispatcher.staticPlayerZ)));

		for (Entity e : MC.world.getEntities())
		{
			if (!(e instanceof LivingEntity)) continue;
			LivingEntity entity = (LivingEntity)e;

			TrajectoryPath path = getPath(entity);

			double defaultred = ((DEFAULT_COLOR & 0xFF000000) >> 24) / 255d;
			double defaultgreen = ((DEFAULT_COLOR & 0x00FF0000) >> 16) / 255d;
			double defaultblue = ((DEFAULT_COLOR & 0x0000FF00) >> 8) / 255d;
			double defaultalpha = (DEFAULT_COLOR & 0x000000FF) / 255d;

			GL11.glBegin(GL11.GL_LINE_STRIP);
			for (Vec3d point : path)
			{
				//System.out.println("R: " + defaultred + ", G: " + defaultgreen + ", B: " + defaultblue + ", A: " + defaultalpha);
				GL11.glColor4d(defaultred, defaultgreen, defaultblue, defaultalpha);
				GL11.glVertex3d(point.x - BlockEntityRenderDispatcher.renderOffsetX, point.y - BlockEntityRenderDispatcher.renderOffsetY , point.z - BlockEntityRenderDispatcher.renderOffsetZ);
			}
			GL11.glEnd();
		}


		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		//GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glPopMatrix();
	}


	/**
	 * Returns the path the entity's held item will take through the air.
	 * @param entity
	 * @return
	 */
	private static TrajectoryPath getPath(LivingEntity entity)
	{
		return getPath(entity, false);
	}

	/**
	 * Returns the path the entity's held item will take through the air.
	 * if maxPitch is true, return the value of the path at 45°.
	 * @param entity
	 * @return
	 */
	private static TrajectoryPath getPath(LivingEntity entity, boolean maxPitch)
	{
		TrajectoryPath path = new TrajectoryPath();
		Item item = entity.getMainHandStack() == null ? null : entity.getMainHandStack().getItem();

		// If not holding anything or held item is not ranged weapon, return empty path (not null).
		if (item == null || !isRangedWeapon(item))
		{
			return path;
		}

		// The three values indicate the arrow's position in space, using standard Minecraft coordinates.
		// They will be updated each cycle until arrow impacts.
		// Obviously, at this point, these values are very close to the firing entity's pos.
		double arrowPosX = entity.prevRenderX + (entity.x - entity.prevRenderX) * MinecraftClient.getInstance().getTickDelta() - Math.cos((float)Math.toRadians(entity.yaw)) * 0.16f;
		double arrowPosY = entity.prevRenderY + (entity.y - entity.prevRenderY) * MinecraftClient.getInstance().getTickDelta() + entity.getStandingEyeHeight() - 0.1;
		double arrowPosZ = entity.prevRenderZ + (entity.z - entity.prevRenderZ) * MinecraftClient.getInstance().getTickDelta() - Math.sin((float)Math.toRadians(entity.yaw)) * 0.16f;

		// Motion factor. Arrows go faster than snowballs and all that...
		double projectileMotionFactor = (item instanceof BowItem) ? 1.0 : 0.4;

		// Pitch and yaw of the entity's facing, in radians. This is because the Java Math library expects values in radians.
		double yaw = Math.toRadians(entity.yaw); // Left/right. Right is positive, left is negative. Does not wrap.
		double pitch = maxPitch ? (-0.25 * Math.PI) : Math.toRadians(entity.pitch); // Up/down. Up is negative, down is positive.

		// The motion of the projectile. These values are small doubles that will be added to the arrowPos values
		// each iteration of the projection in order to form a list of points that show the motion of a projectile.
		// Right now, this is the value added after the first iteration.
		// Bigger values likely means faster projectiles.
		double projectileMotionX = (float) (-Math.sin(yaw) * Math.cos(pitch) * projectileMotionFactor);
		double projectileMotionY = (float) (-Math.sin(pitch) * projectileMotionFactor);
		double projectileMotionZ = (float) (Math.cos(yaw) * Math.cos(pitch) * projectileMotionFactor);

		// 3D Pythagorean theorem. Returns the length of the vector representing the distance between the two points used to render a path.
		// Last point + projectileMotion = next point's distance in 3D space.
		double projectileMotion = Math.sqrt(projectileMotionX * projectileMotionX + projectileMotionY * projectileMotionY + projectileMotionZ * projectileMotionZ);

		// Divide the motion in each axis by the length. Not really sure why.
		projectileMotionX /= projectileMotion;
		projectileMotionY /= projectileMotion;
		projectileMotionZ /= projectileMotion;

		// Calculation that modifies the projectilemotion. No idea how this works.
		if (item instanceof BowItem || item instanceof CrossbowItem) {
			float bowPower = (72000 - entity.getItemUseTimeLeft()) / 20.0f;
			bowPower = (bowPower * bowPower + bowPower * 2.0f) / 3.0f;
			if (bowPower > 1.0f || bowPower <= 0.1f) {
				bowPower = 1.0f;
			}
			bowPower *= 3.0f;
			projectileMotionX *= bowPower;
			projectileMotionY *= bowPower;
			projectileMotionZ *= bowPower;

		}
		else {
			projectileMotionX *= 1.5;
			projectileMotionY *= 1.5;
			projectileMotionZ *= 1.5;
		}

		// Specifies the ballistic drop for various items. Smaller values means less drop, so greater range.
		double gravity = (item instanceof BowItem || item instanceof CrossbowItem) ? 0.05 :
				((item instanceof PotionItem) ? 0.4 : ((item instanceof FishingRodItem) ? 0.15 : (item instanceof TridentItem) ? 0.015 :0.03));

		for (int i = 0; i < MAX_POINTS; i++)
		{
			// Add this point (in absolute space) to the path.
			path.addPoint(new Vec3d(arrowPosX, arrowPosY, arrowPosZ));

			// Add the projectile's motion this iteration to its absolute position, thereby updating it for the next iteration.
			// Though, I'm not really sure what the 0.1x multiplier is doing.
			arrowPosX += projectileMotionX * 0.1;
			arrowPosY += projectileMotionY * 0.1;
			arrowPosZ += projectileMotionZ * 0.1;

			// Projectile gets slower due to friction in air or something.
			projectileMotionX *= 0.999;
			projectileMotionY *= 0.999;
			projectileMotionZ *= 0.999;

			// Apply gravity drop to the motion.
			projectileMotionY -= gravity * 0.1;
		}

		return path;
	}

	private static boolean isRangedWeapon(Item item) {
		return
				item instanceof BowItem ||
						item instanceof CrossbowItem ||
						item instanceof SnowballItem ||
						item instanceof EggItem ||
						item instanceof EnderPearlItem ||
						item instanceof SplashPotionItem ||
						item instanceof LingeringPotionItem ||
						item instanceof FishingRodItem ||
						item instanceof TridentItem;
	}

	public enum Style
	{
		BOXES("Boxes only", true, false),
		LINES("Lines only", false, true),
		LINES_AND_BOXES("Lines and boxes", true, true);

		private final String name;
		private Style(String name, boolean boxes, boolean lines)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}
