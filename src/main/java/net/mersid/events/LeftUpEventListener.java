package net.mersid.events;

import java.util.ArrayList;

import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface LeftUpEventListener extends Listener {
	public void onLeftUp(LeftUpEvent event);
	
	public static class LeftUpEvent extends CancellableEvent<LeftUpEventListener>
	{
		@Override
		public void fire(ArrayList<LeftUpEventListener> listeners) {
			for (LeftUpEventListener listener : listeners)
			{
				listener.onLeftUp(this);
				
				if(isCancelled())
					break;
			}
		}

		@Override
		public Class<LeftUpEventListener> getListenerType() {
			return LeftUpEventListener.class;
		}
	}
}
