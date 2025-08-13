package io.saturday.Neo.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class MathUtils {
   public static final double PI = Math.PI;
   static final double PI2 = Math.PI * 2;
   static final float PI_f = (float) Math.PI;
   static final float PI2_f = (float) (Math.PI * 2);
   static final double PIHalf = Math.PI / 2;
   static final float PIHalf_f = (float) (Math.PI / 2);
   static final double PI_4 = Math.PI / 4;
   static final double PI_INV = 0.3183098861837907;
   public static final Random random = new Random();

   public static float clampPitch_To90(float pitch) {
      if (pitch > 90.0F) {
         return 90.0F;
      } else {
         return pitch < -90.0F ? -90.0F : pitch;
      }
   }

   public static float map(float x, float prev_min, float prev_max, float new_min, float new_max) {
      return (x - prev_min) / (prev_max - prev_min) * (new_max - new_min) + new_min;
   }

   public static double clamp(double value, double min, double max) {
      return value < min ? min : Math.min(value, max);
   }

   public static int clamp(int value, int min, int max) {
      return value < min ? min : Math.min(value, max);
   }

   public static <T extends Number> T clamp(T value, T minimum, T maximum) {
      if (value instanceof Integer) {
         if (value.intValue() > maximum.intValue()) {
            value = maximum;
         } else if (value.intValue() < minimum.intValue()) {
            value = minimum;
         }
      } else if (value instanceof Float) {
         if (value.floatValue() > maximum.floatValue()) {
            value = maximum;
         } else if (value.floatValue() < minimum.floatValue()) {
            value = minimum;
         }
      } else if (value instanceof Double) {
         if (value.doubleValue() > maximum.doubleValue()) {
            value = maximum;
         } else if (value.doubleValue() < minimum.doubleValue()) {
            value = minimum;
         }
      } else if (value instanceof Long) {
         if (value.longValue() > maximum.longValue()) {
            value = maximum;
         } else if (value.longValue() < minimum.longValue()) {
            value = minimum;
         }
      } else if (value instanceof Short) {
         if (value.shortValue() > maximum.shortValue()) {
            value = maximum;
         } else if (value.shortValue() < minimum.shortValue()) {
            value = minimum;
         }
      } else if (value instanceof Byte) {
         if (value.byteValue() > maximum.byteValue()) {
            value = maximum;
         } else if (value.byteValue() < minimum.byteValue()) {
            value = minimum;
         }
      }

      return value;
   }

   public static double getRandomDoubleInRange(double minDouble, double maxDouble) {
      return minDouble >= maxDouble ? minDouble : random.nextDouble() * (maxDouble - minDouble) + minDouble;
   }

   public static int getRandomIntInRange(int startInclusive, int endExclusive, Random random) {
      return endExclusive - startInclusive <= 0 ? startInclusive : startInclusive + random.nextInt(endExclusive - startInclusive);
   }

   public static int getRandomIntInRange(int startInclusive, int endExclusive) {
      return endExclusive - startInclusive <= 0 ? startInclusive : startInclusive + new Random().nextInt(endExclusive - startInclusive);
   }

   public static float normalizeAngle(float angle) {
      float newAngle = angle % 360.0F;
      return newAngle < -180.0F ? newAngle + 360.0F : (newAngle > 180.0F ? newAngle - 360.0F : newAngle);
   }

   public static float interpolate(float delta, float start, float end) {
      return start + delta * (end - start);
   }

   public static double interpolate(double delta, double start, float end) {
      return start + delta * ((double)end - start);
   }

   public static double interpolate(float delta, double start, double end) {
      return start + (double)delta * (end - start);
   }

   public static float interpolateAngle(float delta, float start, float end) {
      return start + delta * normalizeAngle(end - start);
   }

   public static double roundToPlace(double value, int places) {
      if (places < 0) {
         throw new IllegalArgumentException();
      } else {
         return new BigDecimal(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
      }
   }

   public static <T extends Number> int getNumberDecimalDigits(T value) {
      if (!(value instanceof Integer) && !(value instanceof Long)) {
         String[] number = value.toString().split("\\.");
         if (number.length == 2) {
            if (number[1].endsWith("0")) {
               number[1] = number[1].substring(0, number[1].length() - 1);
            }

            return number[1].length();
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public static float clampValue(float value, float floor, float cap) {
      return value < floor ? floor : Math.min(value, cap);
   }

   public static int clampValue(int value, int floor, int cap) {
      return value < floor ? floor : Math.min(value, cap);
   }

   public static int powerOfTwo(int cap) {
      int n = cap - 1;
      n |= n >> 1;
      n |= n >> 2;
      n |= n >> 4;
      n |= n >> 8;
      n |= n >> 16;
      return n + 1;
   }

   public static float log(double base, double number) {
      return (float)(Math.log(number) / Math.log(base));
   }

   public static double round(double value, int places) {
      if (places < 0) {
         throw new IllegalArgumentException();
      } else {
         BigDecimal bd = new BigDecimal(value);
         bd = bd.setScale(places, RoundingMode.HALF_UP);
         return bd.doubleValue();
      }
   }

   public static boolean contains(float x, float y, float minX, float minY, float maxX, float maxY) {
      return x > minX && x < maxX && y > minY && y < maxY;
   }

   public static float calculateGaussianValue(float x, float sigma) {
      double PI = 3.141592653;
      double output = 1.0 / Math.sqrt(2.0 * PI * (double)(sigma * sigma));
      return (float)(output * Math.exp((double)(-(x * x)) / (2.0 * (double)(sigma * sigma))));
   }

   public static double cosFromSin(double sin, double angle) {
      double cos = sqrt(1.0 - sin * sin);
      double a = angle + (Math.PI / 2);
      double b = a - (double)((int)(a / (Math.PI * 2))) * (Math.PI * 2);
      if (b < 0.0) {
         b += Math.PI * 2;
      }

      return b >= Math.PI ? -cos : cos;
   }

   public static double sqrt(double r) {
      return Math.sqrt(r);
   }
}
