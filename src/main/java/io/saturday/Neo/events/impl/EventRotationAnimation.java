package io.saturday.Neo.events.impl;

import io.saturday.Neo.events.api.events.Event;
import net.minecraft.entity.Entity;

public class EventRotationAnimation implements Event {
   public static Entity currentEntity;
   private float yaw;
   private float lastYaw;
   private float pitch;
   private float lastPitch;

   public float getYaw() {
      return this.yaw;
   }

   public float getLastYaw() {
      return this.lastYaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public float getLastPitch() {
      return this.lastPitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setLastYaw(float lastYaw) {
      this.lastYaw = lastYaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void setLastPitch(float lastPitch) {
      this.lastPitch = lastPitch;
   }

   public EventRotationAnimation(float yaw, float lastYaw, float pitch, float lastPitch) {
      this.yaw = yaw;
      this.lastYaw = lastYaw;
      this.pitch = pitch;
      this.lastPitch = lastPitch;
   }
}
