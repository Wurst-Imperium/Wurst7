package io.saturday.Neo.events.impl;

import io.saturday.Neo.events.api.events.Event;
import net.minecraft.entity.Entity;

public class EventRayTrace implements Event {

   public Entity entity;
   public float yaw;
   public float pitch;

   public Entity getEntity() {
      return this.entity;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setEntity(Entity entity) {
      this.entity = entity;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public EventRayTrace(Entity entity, float yaw, float pitch) {
      this.entity = entity;
      this.yaw = yaw;
      this.pitch = pitch;
   }
}
