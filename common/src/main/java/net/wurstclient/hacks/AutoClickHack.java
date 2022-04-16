package net.wurstclient.hacks;

import java.net.Inet4Address;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import net.wurstclient.Category;
import net.wurstclient.WurstClient;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public class AutoClickHack extends Hack implements UpdateListener{

	private final EnumSetting<Button> button = new EnumSetting<AutoClickHack.Button>("Button", "Button to clik automatically", Button.values(), Button.RMB);
	private final SliderSetting interval = new SliderSetting("Interval", "How often to click the button", 500, 10, 10000, 10, v -> ValueDisplay.INTEGER.getValueString(v) + " ms");
	
	private Timer timer;
	private int timerInerval;
	
	public AutoClickHack() {
		super("AutoClick");
		setCategory(Category.OTHER);
		addSetting(button);
		addSetting(interval);
		timerInerval = interval.getValueI();
	}

	@Override
	public void onEnable() {
		
		TimerTask task = new TimerTask(){
		
			@Override
			public void run() {
				if(button.getSelected().name == "Right Mouse Button"){
					IMC.rightClick();
				}
				else {
					IMC.leftClick();
				}
			}
		};
		timer = new Timer();
		timer.schedule(task, new Date(), (long) interval.getValue());
		EVENTS.add(UpdateListener.class, this);
	}

	@Override
	public void onDisable() {
		timer.cancel();
		timer.purge();
		EVENTS.remove(UpdateListener.class, this);
	}

	@Override
	public void onUpdate() {
		if(timerInerval != interval.getValueI()){
			timerInerval = interval.getValueI();
			onDisable();
			onEnable();
		}
	}

	public static enum Button
	{
		RMB("Right Mouse Button"),
		LMB("Left Mouse Button");
		
		private final String name;
		
		private Button(String name)
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