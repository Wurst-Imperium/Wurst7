/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface CameraTransformViewBobbingListener extends Listener
{
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event);
	
	public static class CameraTransformViewBobbingEvent
		extends CancellableEvent<CameraTransformViewBobbingListener>
	{
		@Override
		public void fire(
			ArrayList<CameraTransformViewBobbingListener> listeners)
		{
			for(CameraTransformViewBobbingListener listener : listeners)
			{
				listener.onCameraTransformViewBobbing(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<CameraTransformViewBobbingListener> getListenerType()
		{
			return CameraTransformViewBobbingListener.class;
		}
	}
}
