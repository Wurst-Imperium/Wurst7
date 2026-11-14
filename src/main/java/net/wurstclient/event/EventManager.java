/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.wurstclient.WurstClient;

public final class EventManager
{
	private final WurstClient wurst;
	private final HashMap<Class<? extends Listener>, ArrayList<? extends Listener>> listenerMap =
		new HashMap<>();
	
	public EventManager(WurstClient wurst)
	{
		this.wurst = wurst;
	}
	
	/**
	 * Fires the given {@link Event} if Wurst is enabled and the
	 * {@link EventManager} is ready to accept events. This method is safe to
	 * call even when the EventManager hasn't been initialized yet.
	 */
	public static <L extends Listener, E extends Event<L>> void fire(E event)
	{
		EventManager eventManager = WurstClient.INSTANCE.getEventManager();
		if(eventManager == null)
			return;
		
		eventManager.fireImpl(event);
	}
	
	private <L extends Listener, E extends Event<L>> void fireImpl(E event)
	{
		if(!wurst.isEnabled())
			return;
		
		try
		{
			Class<L> type = event.getListenerType();
			ArrayList<L> snapshot;
			synchronized(listenerMap)
			{
				@SuppressWarnings("unchecked")
				ArrayList<L> listeners = (ArrayList<L>)listenerMap.get(type);
				
				if(listeners == null || listeners.isEmpty())
					return;
				
				snapshot = new ArrayList<>(listeners);
			}
			
			event.fire(snapshot);
			
		}catch(Throwable e)
		{
			e.printStackTrace();
			
			CrashReport report =
				CrashReport.forThrowable(e, "Firing Wurst event");
			CrashReportCategory section = report.addCategory("Affected event");
			section.setDetail("Event class", () -> event.getClass().getName());
			
			throw new ReportedException(report);
		}
	}
	
	public <L extends Listener> void add(Class<L> type, L listener)
	{
		Objects.requireNonNull(listener);
		
		try
		{
			synchronized(listenerMap)
			{
				@SuppressWarnings("unchecked")
				ArrayList<L> listeners = (ArrayList<L>)listenerMap.get(type);
				
				if(listeners == null)
				{
					listeners = new ArrayList<>(Arrays.asList(listener));
					listenerMap.put(type, listeners);
					return;
				}
				
				if(!listeners.contains(listener))
					listeners.add(listener);
			}
			
		}catch(Throwable e)
		{
			e.printStackTrace();
			
			CrashReport report =
				CrashReport.forThrowable(e, "Adding Wurst event listener");
			CrashReportCategory section =
				report.addCategory("Affected listener");
			section.setDetail("Listener type", () -> type.getName());
			section.setDetail("Listener class",
				() -> listener.getClass().getName());
			
			throw new ReportedException(report);
		}
	}
	
	public <L extends Listener> void remove(Class<L> type, L listener)
	{
		try
		{
			synchronized(listenerMap)
			{
				@SuppressWarnings("unchecked")
				ArrayList<L> listeners = (ArrayList<L>)listenerMap.get(type);
				
				if(listeners != null)
					listeners.remove(listener);
			}
			
		}catch(Throwable e)
		{
			e.printStackTrace();
			
			CrashReport report =
				CrashReport.forThrowable(e, "Removing Wurst event listener");
			CrashReportCategory section =
				report.addCategory("Affected listener");
			section.setDetail("Listener type", () -> type.getName());
			section.setDetail("Listener class",
				() -> listener.getClass().getName());
			
			throw new ReportedException(report);
		}
	}
}
