/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filters.FilterBabiesSetting;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"feed aura", "BreedAura", "breed aura", "AutoBreeder",
	"auto breeder"})
public final class FeedAuraHack extends Hack
	implements UpdateListener, HandleInputListener, RenderListener
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
			+ "Recommended in Minecraft versions before 1.20.3 due to MC-233276,"
			+ "which causes these animals to consume items indefinitely.",
		false);
	
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
		EVENTS.add(HandleInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(HandleInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		target = null;
		renderTarget = null;
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		ItemStack heldStack = player.getInventory().getSelectedStack();
		
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
	public void onHandleInput()
	{
		if(target == null)
			return;
		
		ClientPlayerInteractionManager im = MC.interactionManager;
		ClientPlayerEntity player = MC.player;
		Hand hand = Hand.MAIN_HAND;
		
		if(im.isBreakingBlock() || player.isRiding())
			return;
		
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
		
		if(actionResult instanceof ActionResult.Success success
			&& success.swingSource() == ActionResult.SwingSource.CLIENT)
			player.swingHand(hand);
		
		target = null;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(renderTarget == null)
			return;
		
		float p = 1;
		if(renderTarget.getMaxHealth() > 1e-5)
			p = renderTarget.getHealth() / renderTarget.getMaxHealth();
		float green = p * 2F;
		float red = 2 - green;
		float[] rgb = {red, green, 0};
		int quadColor = RenderUtils.toIntColor(rgb, 0.25F);
		int lineColor = RenderUtils.toIntColor(rgb, 0.5F);
		
		Box box = EntityUtils.getLerpedBox(renderTarget, partialTicks);
		if(p < 1)
			box = box.contract((1 - p) * 0.5 * box.getLengthX(),
				(1 - p) * 0.5 * box.getLengthY(),
				(1 - p) * 0.5 * box.getLengthZ());
		
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
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
