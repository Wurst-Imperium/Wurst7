package net.mersid.events;

import java.util.ArrayList;

import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

/**
 * Any class implementing this will have the ability to cancel the CameraShakeEvent, which does exactly what you think it does.
 * @author Admin
 *
 */
public interface CameraShakeEventListener extends Listener {

	public void onCameraShakeEvent(CameraShakeEvent event);
	
	public static class CameraShakeEvent extends CancellableEvent<CameraShakeEventListener>
	{

		@Override
		public void fire(ArrayList<CameraShakeEventListener> listeners) {
			for (CameraShakeEventListener listener : listeners)
			{
				listener.onCameraShakeEvent(this);
			}
			
		}

		@Override
		public Class<CameraShakeEventListener> getListenerType() {
			return CameraShakeEventListener.class;
		}
		
	}
	
}
