package io.saturday.Neo.events.impl;

import io.saturday.Neo.events.api.events.Event;
import io.saturday.Neo.events.api.types.EventType;

public class EventRunTicks implements Event {
   private final EventType type;

   public EventType getType() {
      return this.type;
   }

   public EventRunTicks(EventType type) {
      this.type = type;
   }
}
