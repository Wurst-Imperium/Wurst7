package net.wurstclient.hacks;

import net.wurstclient.hack.Hack;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.events.UpdateListener;

@SearchTags({"GravityHack", "gravity hack", "high gravity", "Crystal Fall Speed"})
public final class HighGravityHack extends Hack implements UpdateListener
{
	private int jumpTicks = 0;
	public final SliderSetting multipier =
		new SliderSetting("Multiplier", 1, 0, 5, 0.05, ValueDisplay.DECIMAL);
	
	public HighGravityHack()
	{
		super("HighGravity", "Increases gravity so you fall faster");
		setCategory(Category.MOVEMENT);
		addSetting(multipier);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
        if(MC.options.keyJump.isPressed() && MC.player.isOnGround())
            jumpTicks=5;
        if(jumpTicks < 1)
            MC.player.setVelocity(MC.player.getVelocity().x, MC.player.getVelocity().y - multipier.getValue(), MC.player.getVelocity().z);
        else
            jumpTicks--;

    }
}
