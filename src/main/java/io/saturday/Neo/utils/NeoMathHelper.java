package io.saturday.Neo.utils;

public class NeoMathHelper {
   public static float wrapDegrees(float value) {
      value %= 360.0F;
      if (value >= 180.0F) {
         value -= 360.0F;
      }

      if (value < -180.0F) {
         value += 360.0F;
      }

      return value;
   }

   public static double wrapDegrees(double value) {
      value %= 360.0;
      if (value >= 180.0) {
         value -= 360.0;
      }

      if (value < -180.0) {
         value += 360.0;
      }

      return value;
   }

   public static int wrapDegrees(int angle) {
      angle %= 360;
      if (angle >= 180) {
         angle -= 360;
      }

      if (angle < -180) {
         angle += 360;
      }

      return angle;
   }

   public static int clamp(int num, int min, int max) {
      if (num < min) {
         return min;
      } else {
         return num > max ? max : num;
      }
   }

   public static float clamp(float num, float min, float max) {
      if (num < min) {
         return min;
      } else {
         return num > max ? max : num;
      }
   }

   public static double clamp(double num, double min, double max) {
      if (num < min) {
         return min;
      } else {
         return num > max ? max : num;
      }
   }
}
