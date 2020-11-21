/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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

import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
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
			@SuppressWarnings("unchecked")
			ArrayList<L> listeners = (ArrayList<L>)listenerMap.get(type);
			
			if(listeners == null || listeners.isEmpty())
				return;
				
			// Creating a copy of the list to avoid concurrent modification
			// issues.
			ArrayList<L> listeners2 = new ArrayList<>(listeners);
			
			// remove() sets an element to null before removing it. When one
			// thread calls remove() while another calls fire(), it is possible
			// for this list to contain null elements, which need to be filtered
			// out.
			listeners2.removeIf(Objects::isNull);
			
			event.fire(listeners2);
			
		}catch(Throwable e)
		{
			e.printStackTrace();
			
			CrashReport report = CrashReport.create(e, "Firing Wurst event");
			CrashReportSection section = report.addElement("Affected event");
			section.add("Event class", () -> event.getClass().getName());
			
			throw new CrashException(report);
		}
	}
	
	public <L extends Listener> void add(Class<L> type, L listener)
	{
		try
		{
			@SuppressWarnings("unchecked")
			ArrayList<L> listeners = (ArrayList<L>)listenerMap.get(type);
			
			if(listeners == null)
			{
				listeners = new ArrayList<>(Arrays.asList(listener));
				listenerMap.put(type, listeners);
				return;
			}
			
			listeners.add(listener);
			
		}catch(Throwable e)
		{
			e.printStackTrace();
			
			CrashReport report =
				CrashReport.create(e, "Adding Wurst event listener");
			CrashReportSection section = report.addElement("Affected listener");
			section.add("Listener type", () -> type.getName());
			section.add("Listener class", () -> listener.getClass().getName());
			
			throw new CrashException(report);
		}
	}
	
	public <L extends Listener> void remove(Class<L> type, L listener)
	{
		try
		{
			@SuppressWarnings("unchecked")
			ArrayList<L> listeners = (ArrayList<L>)listenerMap.get(type);
			
			if(listeners != null)
				listeners.remove(listener);
			
		}catch(Throwable e)
		{
			e.printStackTrace();
			
			CrashReport report =
				CrashReport.create(e, "Removing Wurst event listener");
			CrashReportSection section = report.addElement("Affected listener");
			section.add("Listener type", () -> type.getName());
			section.add("Listener class", () -> listener.getClass().getName());
			
			throw new CrashException(report);
		}
	}
}
