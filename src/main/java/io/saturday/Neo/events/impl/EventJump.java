package io.saturday.Neo.events.impl;

import io.saturday.Neo.events.api.events.Event;

public class EventJump implements Event {
   private float yaw;

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getYaw() {
      return this.yaw;
   }

   public EventJump(float yaw) {
      this.yaw = yaw;
   }
}
