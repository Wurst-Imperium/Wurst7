package io.saturday.Neo.events.impl;

import io.saturday.Neo.events.api.events.callables.EventCancellable;
import net.minecraft.network.packet.Packet;

public class EventPositionItem extends EventCancellable {
   private Packet<?> packet;

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public EventPositionItem(Packet<?> packet) {
      this.packet = packet;
   }
}
