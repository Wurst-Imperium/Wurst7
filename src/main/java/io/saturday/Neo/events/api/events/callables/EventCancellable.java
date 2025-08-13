package io.saturday.Neo.events.api.events.callables;

import io.saturday.Neo.events.api.events.Cancellable;
import io.saturday.Neo.events.api.events.Event;

public abstract class EventCancellable implements Event, Cancellable {
   public boolean cancelled;

   protected EventCancellable() {
   }

   @Override
   public boolean isCancelled() {
      return this.cancelled;
   }

   @Override
   public void setCancelled(boolean state) {
      this.cancelled = state;
   }
}
