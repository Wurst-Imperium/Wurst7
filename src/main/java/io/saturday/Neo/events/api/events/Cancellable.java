package io.saturday.Neo.events.api.events;

public interface Cancellable {
   boolean isCancelled();

   void setCancelled(boolean var1);
}
