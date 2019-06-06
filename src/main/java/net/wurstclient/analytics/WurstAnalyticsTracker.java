/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.analytics;

import net.wurstclient.analytics.dmurph.AnalyticsRequestData;
import net.wurstclient.analytics.dmurph.JGoogleAnalyticsTracker;

public final class WurstAnalyticsTracker extends JGoogleAnalyticsTracker
{
	private boolean enabled = true;
	
	public WurstAnalyticsTracker(String trackingID)
	{
		super(new WurstAnalyticsConfigData(trackingID),
			GoogleAnalyticsVersion.V_4_7_2);
	}
	
	@Override
	public void trackPageView(String argPageURL, String argPageTitle,
		String argHostName)
	{
		if(!enabled)
			return;
		
		super.trackPageView(argPageURL, argPageTitle, argHostName);
	}
	
	@Override
	public void trackPageViewFromReferrer(String argPageURL,
		String argPageTitle, String argHostName, String argReferrerSite,
		String argReferrerPage)
	{
		if(!enabled)
			return;
		
		super.trackPageViewFromReferrer(argPageURL, argPageTitle, argHostName,
			argReferrerSite, argReferrerPage);
	}
	
	@Override
	public void trackPageViewFromSearch(String argPageURL, String argPageTitle,
		String argHostName, String argSearchSource, String argSearchKeywords)
	{
		if(!enabled)
			return;
		
		super.trackPageViewFromSearch(argPageURL, argPageTitle, argHostName,
			argSearchSource, argSearchKeywords);
	}
	
	@Override
	public void trackEvent(String argCategory, String argAction)
	{
		if(!enabled)
			return;
		
		super.trackEvent(argCategory, argAction);
	}
	
	@Override
	public void trackEvent(String argCategory, String argAction,
		String argLabel)
	{
		if(!enabled)
			return;
		
		super.trackEvent(argCategory, argAction, argLabel);
	}
	
	@Override
	public void trackEvent(String argCategory, String argAction,
		String argLabel, Integer argValue)
	{
		if(!enabled)
			return;
		
		super.trackEvent(argCategory, argAction, argLabel, argValue);
	}
	
	@Override
	public synchronized void makeCustomRequest(AnalyticsRequestData argData)
	{
		if(!enabled)
			return;
		
		super.makeCustomRequest(argData);
	}
	
	@Override
	public boolean isEnabled()
	{
		return enabled;
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
}
