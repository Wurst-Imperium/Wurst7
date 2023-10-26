package net.wurstclient.events;

import net.minecraft.block.BlockState;
import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

import java.util.ArrayList;

public interface ShouldDrawFacelessModelListener extends Listener
{
    public void onShouldDrawFacelessModel(ShouldDrawFacelessModelEvent event);

    public static class ShouldDrawFacelessModelEvent extends CancellableEvent<ShouldDrawFacelessModelListener>
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
            // fire only if called from Sodium's BlockRenderer
            if (Thread.currentThread().getStackTrace().length < 8 || !Thread.currentThread().getStackTrace()[7].getClassName()
                .equals("me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer"))
                return;

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
