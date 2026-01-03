/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public abstract class GenericInteractHack extends Hack
	implements UpdateListener, HandleInputListener, RenderListener
{
	
	protected Animal target;
	protected Animal renderTarget;
	
	public GenericInteractHack(String name)
	{
		super(name);
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
	public void onHandleInput()
	{
		if(target == null)
			return;
		
		MultiPlayerGameMode im = MC.gameMode;
		LocalPlayer player = MC.player;
		InteractionHand hand = InteractionHand.MAIN_HAND;
		
		if(im.isDestroying() || player.isHandsBusy())
			return;
		
		// create realistic hit result
		AABB box = target.getBoundingBox();
		Vec3 start = RotationUtils.getEyesPos();
		Vec3 end = box.getCenter();
		Vec3 hitVec = box.clip(start, end).orElse(start);
		EntityHitResult hitResult = new EntityHitResult(target, hitVec);
		
		InteractionResult actionResult =
			im.interactAt(player, target, hitResult, hand);
		
		if(!actionResult.consumesAction())
			actionResult = im.interact(player, target, hand);
		
		if(actionResult instanceof InteractionResult.Success success
			&& success.swingSource() == InteractionResult.SwingSource.CLIENT)
			player.swing(hand);
		
		target = null;
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
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
		
		AABB box = EntityUtils.getLerpedBox(renderTarget, partialTicks);
		if(p < 1)
			box = box.deflate((1 - p) * 0.5 * box.getXsize(),
				(1 - p) * 0.5 * box.getYsize(), (1 - p) * 0.5 * box.getZsize());
		
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}
	
	protected boolean isUntamed(Animal e)
	{
		if(e instanceof AbstractHorse horse && !horse.isTamed())
			return true;
		
		if(e instanceof TamableAnimal tame && !tame.isTame())
			return true;
		
		return false;
	}
}
