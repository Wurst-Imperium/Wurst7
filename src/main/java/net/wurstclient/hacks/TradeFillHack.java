/**
 * 
 */
package net.wurstclient.hacks;

import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

/**
 * @author yuanlu
 *
 */
@SearchTags({ "auto", "autotradefill", "villager" })
public class TradeFillHack extends Hack implements UpdateListener {

	public TradeFillHack() {
		super("TradeFill");
	}

	private Runnable nextTick;

	public void nextTick(Runnable runnable) {
		nextTick = runnable;
	}

	@Override
	protected void onEnable() {
		EVENTS.add(UpdateListener.class, this);
	}

	@Override
	protected void onDisable() {
		EVENTS.remove(UpdateListener.class, this);
	}

	@Override
	public void onUpdate() {
		if (nextTick != null) nextTick.run();
	}
}
