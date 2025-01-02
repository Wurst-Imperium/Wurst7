/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.AttackSpeedSliderSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.util.EntityUtils;

@SearchTags({"trigger bot", "AutoAttack", "auto attack", "AutoClicker",
	"auto clicker"})
public final class TriggerBotHack extends Hack
	implements PreMotionListener, HandleInputListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 4.25, 1, 6, 0.05, ValueDisplay.DECIMAL);
	
	private final AttackSpeedSliderSetting speed =
		new AttackSpeedSliderSetting();
	
	private final SliderSetting speedRandMS =
		new SliderSetting("Speed randomization",
			"Helps you bypass anti-cheat plugins by varying the delay between"
				+ " attacks.\n\n" + "\u00b1100ms is recommended for Vulcan.\n\n"
				+ "0 (off) is fine for NoCheat+, AAC, Grim, Verus, Spartan, and"
				+ " vanilla servers.",
			100, 0, 1000, 50, ValueDisplay.INTEGER.withPrefix("\u00b1")
				.withSuffix("ms").withLabel(0, "off"));
	
	private final SwingHandSetting swingHand =
		new SwingHandSetting(this, SwingHand.CLIENT);
	
	private final CheckboxSetting attackWhileBlocking =
		new CheckboxSetting("Attack while blocking",
			"Attacks even while you're blocking with a shield or using"
				+ " items.\n\n"
				+ "This would not be possible in vanilla and won't work if"
				+ " \"Simulate mouse click\" is enabled.",
			false);
	
	private final CheckboxSetting simulateMouseClick = new CheckboxSetting(
		"Simulate mouse click",
		"Simulates an actual mouse click (or key press) when attacking. Can be"
			+ " used to trick CPS measuring tools into thinking that you're"
			+ " attacking manually.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r Simulating mouse clicks can lead"
			+ " to unexpected behavior, like in-game menus clicking themselves."
			+ " Also, the \"Swing hand\" and \"Attack while blocking\" settings"
			+ " will not work while this option is enabled.",
		false);
	
	private final EntityFilterList entityFilters =
		EntityFilterList.genericCombat();
	
	private boolean simulatingMouseClick;
	
	public TriggerBotHack()
	{
		super("TriggerBot");
		setCategory(Category.COMBAT);
		
		addSetting(range);
		addSetting(speed);
		addSetting(speedRandMS);
		addSetting(swingHand);
		addSetting(attackWhileBlocking);
		addSetting(simulateMouseClick);
		
		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		// disable other killauras
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().crystalAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().killauraHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
		speed.resetTimer(speedRandMS.getValue());
		EVENTS.add(PreMotionListener.class, this);
		EVENTS.add(HandleInputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		if(simulatingMouseClick)
		{
			IKeyBinding.get(MC.options.attackKey).simulatePress(false);
			simulatingMouseClick = false;
		}
		
		EVENTS.remove(PreMotionListener.class, this);
		EVENTS.remove(HandleInputListener.class, this);
	}
	
	@Override
	public void onPreMotion()
	{
		if(!simulatingMouseClick)
			return;
		
		IKeyBinding.get(MC.options.attackKey).simulatePress(false);
		simulatingMouseClick = false;
	}
	
	@Override
	public void onHandleInput()
	{
		speed.updateTimer();
		if(!speed.isTimeToAttack())
			return;
		
		// don't attack when a container/inventory screen is open
		if(MC.currentScreen instanceof HandledScreen)
			return;
		
		ClientPlayerEntity player = MC.player;
		if(!attackWhileBlocking.isChecked() && player.isUsingItem())
			return;
		
		if(MC.crosshairTarget == null
			|| !(MC.crosshairTarget instanceof EntityHitResult eResult))
			return;
		
		Entity target = eResult.getEntity();
		if(!isCorrectEntity(target))
			return;
		
		WURST.getHax().autoSwordHack.setSlot(target);
		
		if(simulateMouseClick.isChecked())
		{
			IKeyBinding.get(MC.options.attackKey).simulatePress(true);
			simulatingMouseClick = true;
			
		}else
		{
			MC.interactionManager.attackEntity(player, target);
			swingHand.swing(Hand.MAIN_HAND);
		}
		
		speed.resetTimer(speedRandMS.getValue());
	}
	
	private boolean isCorrectEntity(Entity entity)
	{
		if(!EntityUtils.IS_ATTACKABLE.test(entity))
			return false;
		
		if(MC.player.squaredDistanceTo(entity) > range.getValueSq())
			return false;
		
		return entityFilters.testOne(entity);
	}
}
