package net.mersid.hacks;

import net.mersid.events.CameraShakeEventListener;
import net.wurstclient.Category;
import net.wurstclient.hack.Hack;

public class NoHurtcamHack extends Hack implements CameraShakeEventListener {

	public NoHurtcamHack() {
		super("NoHurtcam", "Prevents camera shake when hurt.");
		setCategory(Category.RENDER);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(CameraShakeEventListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(CameraShakeEventListener.class, this);
	}

	@Override
	public void onCameraShakeEvent(CameraShakeEvent event) {
		event.cancel();
		
	}

}
