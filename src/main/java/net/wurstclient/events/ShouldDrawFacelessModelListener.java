package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.block.BlockState;
import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface ShouldDrawFacelessModelListener extends Listener
{
	public void onShouldDrawFacelessModel(ShouldDrawFacelessModelEvent event);
	
	public static class ShouldDrawFacelessModelEvent
		extends CancellableEvent<ShouldDrawFacelessModelListener>
	{
		private final BlockState state;
		
		public ShouldDrawFacelessModelEvent(BlockState state)
		{
			this.state = state;
		}
		
		public BlockState getState()
		{
			return state;
		}
		
		@Override
		public void fire(ArrayList<ShouldDrawFacelessModelListener> listeners)
		{
			for(ShouldDrawFacelessModelListener listener : listeners)
				listener.onShouldDrawFacelessModel(this);
		}
		
		@Override
		public Class<ShouldDrawFacelessModelListener> getListenerType()
		{
			return ShouldDrawFacelessModelListener.class;
		}
	}
}
