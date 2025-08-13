package io.saturday.Neo.events.impl;

import io.saturday.Neo.events.api.events.Event;

public class EventUseItemRayTrace implements Event {
   private float yaw;
   private float pitch;

   public EventUseItemRayTrace(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventUseItemRayTrace other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         return Float.compare(this.getYaw(), other.getYaw()) != 0 ? false : Float.compare(this.getPitch(), other.getPitch()) == 0;
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventUseItemRayTrace;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + Float.floatToIntBits(this.getYaw());
      return result * 59 + Float.floatToIntBits(this.getPitch());
   }

   @Override
   public String toString() {
      return "EventUseItemRayTrace(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }
}
