package io.saturday.Neo.events.api;

import io.saturday.Neo.events.api.events.Event;
import io.saturday.Neo.events.api.events.EventStoppable;
import io.saturday.Neo.events.api.types.Priority;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class EventManager {
   private static final Logger log = LogManager.getLogger(EventManager.class);
   private final Map<Class<? extends Event>, List<EventManager.MethodData>> REGISTRY_MAP = new HashMap<>();

   public void register(Object object) {
      for (Method method : object.getClass().getDeclaredMethods()) {
         if (!this.isMethodBad(method)) {
            this.register(method, object);
         }
      }
   }

   public void register(Object object, Class<? extends Event> eventClass) {
      for (Method method : object.getClass().getDeclaredMethods()) {
         if (!this.isMethodBad(method, eventClass)) {
            this.register(method, object);
         }
      }
   }

   public void unregister(Object object) {
      for (List<EventManager.MethodData> dataList : this.REGISTRY_MAP.values()) {
         for (EventManager.MethodData data : dataList) {
            if (data.getSource().equals(object)) {
               dataList.remove(data);
            }
         }
      }

      this.cleanMap(true);
   }

   public void unregister(Object object, Class<? extends Event> eventClass) {
      if (this.REGISTRY_MAP.containsKey(eventClass)) {
         for (EventManager.MethodData data : this.REGISTRY_MAP.get(eventClass)) {
            if (data.getSource().equals(object)) {
               this.REGISTRY_MAP.get(eventClass).remove(data);
            }
         }

         this.cleanMap(true);
      }
   }

   private void register(Method method, Object object) {
      Class<? extends Event> indexClass = (Class<? extends Event>)method.getParameterTypes()[0];
      final EventManager.MethodData data = new EventManager.MethodData(object, method, method.getAnnotation(EventTarget.class).value());
      if (!data.getTarget().isAccessible()) {
         data.getTarget().setAccessible(true);
      }

      if (this.REGISTRY_MAP.containsKey(indexClass)) {
         if (!this.REGISTRY_MAP.get(indexClass).contains(data)) {
            this.REGISTRY_MAP.get(indexClass).add(data);
            this.sortListValue(indexClass);
         }
      } else {
         this.REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<EventManager.MethodData>() {
            private static final long serialVersionUID = 666L;

            {
               this.add(data);
            }
         });
      }
   }

   public void removeEntry(Class<? extends Event> indexClass) {
      Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = this.REGISTRY_MAP.entrySet().iterator();

      while (mapIterator.hasNext()) {
         if (mapIterator.next().getKey().equals(indexClass)) {
            mapIterator.remove();
            break;
         }
      }
   }

   public void cleanMap(boolean onlyEmptyEntries) {
      Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = this.REGISTRY_MAP.entrySet().iterator();

      while (mapIterator.hasNext()) {
         if (!onlyEmptyEntries || mapIterator.next().getValue().isEmpty()) {
            mapIterator.remove();
         }
      }
   }

   private void sortListValue(Class<? extends Event> indexClass) {
      List<EventManager.MethodData> sortedList = new CopyOnWriteArrayList<>();

      for (byte priority : Priority.VALUE_ARRAY) {
         for (EventManager.MethodData data : this.REGISTRY_MAP.get(indexClass)) {
            if (data.getPriority() == priority) {
               sortedList.add(data);
            }
         }
      }

      this.REGISTRY_MAP.put(indexClass, sortedList);
   }

   private boolean isMethodBad(Method method) {
      return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventTarget.class);
   }

   private boolean isMethodBad(Method method, Class<? extends Event> eventClass) {
      return this.isMethodBad(method) || !method.getParameterTypes()[0].equals(eventClass);
   }

   public Event call(Event event) {
      List<EventManager.MethodData> dataList = this.REGISTRY_MAP.get(event.getClass());
      if (dataList != null) {
         if (event instanceof EventStoppable stoppable) {
            for (EventManager.MethodData data : dataList) {
               this.invoke(data, event);
               if (stoppable.isStopped()) {
                  break;
               }
            }
         } else {
            for (EventManager.MethodData datax : dataList) {
               this.invoke(datax, event);
            }
         }
      }

      return event;
   }

   private void invoke(EventManager.MethodData data, Event argument) {
      try {
         data.getTarget().invoke(data.getSource(), argument);
      } catch (InvocationTargetException var4) {
         log.error("sb {} {}", data.source, data.target);
         var4.printStackTrace();
      } catch (Exception var5) {
         log.error("{} {}", data.source, data.target);
         var5.printStackTrace();
      }
   }

   private static final class MethodData {
      private final Object source;
      private final Method target;
      private final byte priority;

      public MethodData(Object source, Method target, byte priority) {
         this.source = source;
         this.target = target;
         this.priority = priority;
      }

      public Object getSource() {
         return this.source;
      }

      public Method getTarget() {
         return this.target;
      }

      public byte getPriority() {
         return this.priority;
      }
   }
}
